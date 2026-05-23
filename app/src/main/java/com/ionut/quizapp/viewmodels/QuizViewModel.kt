package com.ionut.quizapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionut.quizapp.data.*
import com.ionut.quizapp.logic.GameModeLogic
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class AnswerHistory(
    val question: Question,
    val selectedAnswer: String,
    val isCorrect: Boolean
)

class QuizViewModel(
    private val repository: QuizRepository = QuizRepository()
) : ViewModel() {

    // --- 1. STATE-URI UI (COMPETITIVE & GENERAL) ---
    var questions = mutableStateListOf<Question>()
    var quizHistory = mutableStateListOf<AnswerHistory>()
    var currentOptions by mutableStateOf<List<String>>(emptyList())

    var totalQuestionsCount by mutableIntStateOf(0)
    val totalAnsweredQuestions: Int get() = quizHistory.size
    var score by mutableIntStateOf(0)
    val accuracy: Int get() = if (quizHistory.isNotEmpty()) (score * 100) / quizHistory.size else 0

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isGameOver by mutableStateOf(false)
    var isUtmMode by mutableStateOf(false)
    var isTimedMode by mutableStateOf(false)
    var isSuddenDeathMode by mutableStateOf(false)
    var timeLeft by mutableIntStateOf(60)
    var isNewHighScore by mutableStateOf(false)

    // --- 2. STATE-URI UI (LEARNING & AI) ---
    var isLearningMode by mutableStateOf(false)
    var currentLearningIndex by mutableIntStateOf(0)
    var isLearningAnswerLocked by mutableStateOf(false)
    var selectedLearningAnswer by mutableStateOf<String?>(null)

    // State-uri pentru Gemini AI
    var aiExplanation by mutableStateOf<String?>(null)
    var isAiLoading by mutableStateOf(false)

    // State pentru Progress Map (folosit în ecranul de categorii)
    var categoryProgressMap by mutableStateOf<Map<String, Float>>(emptyMap())
        private set

    // --- 3. LOGICĂ INTERNĂ ---
    private var timerJob: Job? = null
    private var currentGameLogic: GameModeLogic = GameModeLogic.Classic(10)
    private var lastCategories = listOf<String>()
    private var lastIsUtm = false
    private val currentUser get() = SupabaseClient.client.auth.currentUserOrNull()

    // --- 4. MANAGEMENT STARE ---
    fun resetQuizState() {
        isGameOver = false
        isNewHighScore = false
        isLoading = false
        isSuddenDeathMode = false
        isLearningMode = false

        score = 0
        timeLeft = 60
        questions.clear()
        quizHistory.clear()
        currentOptions = emptyList()

        // Reset Learning State
        currentLearningIndex = 0
        isLearningAnswerLocked = false
        selectedLearningAnswer = null
        aiExplanation = null

        timerJob?.cancel()
        timerJob = null
    }

    // --- 5. LOGICĂ MODURI COMPETITIVE (CLASSIC, TIMED, SUDDEN DEATH) ---
    fun loadQuestions(isUtm: Boolean, selectedCategories: List<String>, count: Int, mode: String) {
        resetQuizState()
        isLoading = true

        currentGameLogic = when (mode) {
            "Against Time" -> GameModeLogic.AgainstTime
            "Sudden Death" -> GameModeLogic.SuddenDeath
            else -> GameModeLogic.Classic(count)
        }

        this.isUtmMode = isUtm
        this.lastIsUtm = isUtm
        this.lastCategories = selectedCategories
        this.isTimedMode = currentGameLogic.hasTimer
        this.isSuddenDeathMode = mode == "Sudden Death"

        viewModelScope.launch {
            try {
                val fetchCount = if (isTimedMode || mode == "Sudden Death") 30 else count
                val response = repository.fetchQuestions(isUtm, selectedCategories, fetchCount)

                if (response.isNotEmpty()) {
                    questions.clear()
                    questions.addAll(response)
                    totalQuestionsCount = if (isTimedMode || mode == "Sudden Death") 0 else response.size
                    prepareCurrentOptions()

                    if (isTimedMode) startTimer()
                    isGameOver = false
                } else {
                    isGameOver = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isGameOver = true
            } finally {
                isLoading = false
            }
        }
    }

    fun submitAnswer(selectedAnswer: String) {
        if (questions.isNotEmpty()) {
            val currentQuestion = questions[0]
            val isCorrect = selectedAnswer == currentQuestion.correct_answer

            if (isCorrect) {
                score++
                if (isTimedMode) timeLeft += currentGameLogic.bonusTimePerCorrect
            }

            quizHistory.add(AnswerHistory(currentQuestion, selectedAnswer, isCorrect))
            questions.removeAt(0)

            val mistakes = quizHistory.count { !it.isCorrect }

            if (currentGameLogic.shouldEndGame(mistakes, quizHistory.size, questions.size)) {
                isGameOver = true
                checkAndSaveScore()
            } else {
                val isSuddenDeath = !isTimedMode && currentGameLogic is GameModeLogic.SuddenDeath
                if ((isTimedMode || isSuddenDeath) && questions.size < 5) {
                    fetchMoreQuestionsSilently()
                }

                if (questions.isNotEmpty()) {
                    prepareCurrentOptions()
                } else {
                    isGameOver = true
                    checkAndSaveScore()
                }
            }
        }
    }

    fun skipQuestion() {
        if (questions.size > 1) {
            val skippedQuestion = questions.removeAt(0)
            questions.add(skippedQuestion)
            prepareCurrentOptions()
        }
    }

    // --- 6. LOGICĂ MOD LEARNING ---
    fun loadLearningQuestions(categoryUI: String, difficulty: String, isUtm: Boolean) {
        resetQuizState()
        isLoading = true
        isLearningMode = true
        this.isUtmMode = isUtm
        errorMessage = null

        viewModelScope.launch {
            try {
                val user = currentUser
                val response = repository.fetchQuestionsOrdered(categoryUI, difficulty, isUtm)

                if (response.isNotEmpty()) {
                    questions.addAll(response)

                    val allProgress = if (user != null) repository.getAllUserProgress(user.id, isUtm) else emptyList()
                    val dbCat = if (isUtm) categoryUI else repository.mapCategoriesToDb(listOf(categoryUI)).first()

                    val savedProgress = allProgress.find {
                        it.categoryName.trim().equals(dbCat.trim(), ignoreCase = true) &&
                                it.difficulty.equals(difficulty, ignoreCase = true)
                    }

                    currentLearningIndex = (savedProgress?.lastQuestionIndex ?: 0).coerceIn(0, response.size - 1)
                    prepareOptionsForLearning()
                } else {
                    errorMessage = "No questions found for this specific category and difficulty."
                }
            } catch (e: java.net.UnknownHostException) {
                errorMessage = "Connection lost. Could not fetch questions from server."
            } catch (e: Exception) {
                errorMessage = "An error occurred while loading questions: ${e.localizedMessage ?: "Unknown error"}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun prepareOptionsForLearning() {
        if (questions.isNotEmpty() && currentLearningIndex in questions.indices) {
            val q = questions[currentLearningIndex]
            currentOptions = (q.incorrect_answers + q.correct_answer).shuffled()
            isLearningAnswerLocked = false
            selectedLearningAnswer = null
            aiExplanation = null
        }
    }

    fun submitLearningAnswer(selected: String) {
        if (!isLearningAnswerLocked) {
            selectedLearningAnswer = selected
            isLearningAnswerLocked = true
        }
    }

    fun navigateLearning(direction: Int) {
        val newIndex = currentLearningIndex + direction
        if (newIndex in questions.indices) {
            currentLearningIndex = newIndex
            prepareOptionsForLearning()
        }
    }

    // --- 7. UTILITARE & SALVARE ---
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timeLeft > 0 && !isGameOver) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0) {
                isGameOver = true
                checkAndSaveScore()
            }
        }
    }

    private fun fetchMoreQuestionsSilently() {
        viewModelScope.launch {
            try {
                val response = repository.fetchQuestions(lastIsUtm, lastCategories, 20)
                questions.addAll(response)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAndSaveScore() {
        val user = currentUser

        // 1. Verificăm dacă user-ul este null sau anonim (Guest)
        val isAnonymous = user?.appMetadata?.get("provider")?.jsonPrimitive?.content == "anonymous" || user == null

        // 2. Verificăm dacă suntem în modul Classic (nu salvăm scorul aici)
        // Salvăm DOAR dacă este Timed sau Sudden Death
        val shouldSave = isTimedMode || isSuddenDeathMode

        if (!isAnonymous && user != null && shouldSave) {
            viewModelScope.launch {
                try {
                    val username = user.userMetadata?.get("username")?.jsonPrimitive?.content ?: "Explorer"

                    // Determinăm numele modului pentru baza de date
                    val modeName = if (isTimedMode) "Timed" else "Sudden Death"

                    val result = repository.updateTopScore(
                        userId = user.id,
                        username = username,
                        newScore = score,
                        mode = modeName,
                        isUtm = isUtmMode
                    )

                    isNewHighScore = result

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveProgressAndExit(onComplete: () -> Unit) {
        val user = currentUser
        if (isLearningMode && questions.isNotEmpty() && user != null) {
            viewModelScope.launch {
                try {
                    val currentQuestion = questions[currentLearningIndex]

                    // Re-creăm obiectul folosind proprietățile native camelCase
                    // stabilite în modelul nostru actualizat de date
                    val progress = UserLearningProgress(
                        userId = user.id,
                        categoryName = currentQuestion.category, // Stochează denumirea DB
                        difficulty = currentQuestion.difficulty,
                        lastQuestionIndex = currentLearningIndex,
                        isUtm = isUtmMode
                    )
                    repository.saveLearningProgress(progress)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    resetQuizState()
                    onComplete()
                }
            }
        } else {
            resetQuizState()
            onComplete()
        }
    }

    private fun prepareCurrentOptions() {
        if (questions.isNotEmpty()) {
            val q = questions[0]
            currentOptions = (q.incorrect_answers + q.correct_answer).shuffled()
        }
    }

    suspend fun getLeaderboard(mode: String, isUtm: Boolean) = repository.getGlobalLeaderboard(mode, isUtm)
    suspend fun getPersonalBestScore(userId: String, mode: String, isUtm: Boolean) = repository.getPersonalBest(userId, mode, isUtm)

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}