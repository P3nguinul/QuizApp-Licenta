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

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.decodeFromJsonElement

data class AnswerHistory(
    val question: Question,
    val selectedAnswer: String,
    val isCorrect: Boolean
)

class QuizViewModel(
    private val repository: QuizRepository = QuizRepository(),
    private val aiRepository: AiRepository = AiRepository()
) : ViewModel() {

    // =========================================================================================
    // 1. STATE-URI GENERALE & GLOBALE
    // =========================================================================================
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // State pentru Progress Map (folosit în ecranul de categorii)
    var categoryProgressMap by mutableStateOf<Map<String, Float>>(emptyMap())
        private set

    private val currentUser get() = SupabaseClient.client.auth.currentUserOrNull()
    private var lastCategories = listOf<String>()
    private var lastIsUtm = false

    // =========================================================================================
    // 2. STATE-URI PENTRU MODURI COMPETITIVE (Classic, Timed, Sudden Death)
    // =========================================================================================
    var questions = mutableStateListOf<Question>()
    var quizHistory = mutableStateListOf<AnswerHistory>()
    var currentOptions by mutableStateOf<List<String>>(emptyList())

    var totalQuestionsCount by mutableIntStateOf(0)
    val totalAnsweredQuestions: Int get() = quizHistory.size
    var score by mutableIntStateOf(0)
    val accuracy: Int get() = if (quizHistory.isNotEmpty()) (score * 100) / quizHistory.size else 0

    var isGameOver by mutableStateOf(false)
    var isUtmMode by mutableStateOf(false)
    var isTimedMode by mutableStateOf(false)
    var isSuddenDeathMode by mutableStateOf(false)
    var timeLeft by mutableIntStateOf(60)
    var isNewHighScore by mutableStateOf(false)

    private var timerJob: Job? = null
    private var currentGameLogic: GameModeLogic = GameModeLogic.Classic(10)

    // =========================================================================================
    // 3. STATE-URI PENTRU MODUL LEARNING (Studiu)
    // =========================================================================================
    var isLearningMode by mutableStateOf(false)
    var currentLearningIndex by mutableIntStateOf(0)
    var isLearningAnswerLocked by mutableStateOf(false)
    var selectedLearningAnswer by mutableStateOf<String?>(null)

    // =========================================================================================
    // 4. STATE-URI PENTRU AI & CUSTOM QUIZZES
    // =========================================================================================
    // State-uri pentru Gemini AI (Explicații Mod Learning)
    var aiExplanation by mutableStateOf<String?>(null)
    var isAiLoading by mutableStateOf(false)

    // State-uri pentru Generarea Testelor din PDF
    var isUploadingPdf by mutableStateOf(false)
        private set
    var pdfUploadMessage by mutableStateOf<String?>(null)
        private set

    var isGeneratingQuiz by mutableStateOf(false)
        private set
    var generateQuizError by mutableStateOf<String?>(null)
        private set
    var generateQuizSuccess by mutableStateOf(false)
        private set
    var generatedQuizId by mutableStateOf<String?>(null)
        private set
    var isCustomQuizMode by mutableStateOf(false)
    var showSaveCustomQuizDialog by mutableStateOf(false)
    var customQuizTitleInput by mutableStateOf("")
    var userCustomQuizzes = mutableStateListOf<CustomQuiz>()
        private set
    var isReplayingCustomQuiz by mutableStateOf(false)
    // =========================================================================================
    // 5. FUNCȚII DE BAZĂ (MANAGEMENT STARE)
    // =========================================================================================
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

        isCustomQuizMode = false
        isReplayingCustomQuiz = false
        showSaveCustomQuizDialog = false
        customQuizTitleInput = ""
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // =========================================================================================
    // 6. LOGICĂ: MODURI COMPETITIVE
    // =========================================================================================
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

    // =========================================================================================
    // LOGICĂ: CUSTOM AI QUIZ
    // =========================================================================================
    fun startCustomQuizMode(quizId: String, isReplay: Boolean = false) {
        resetQuizState()
        isLoading = true
        isLearningMode = true
        isCustomQuizMode = true
        isReplayingCustomQuiz = isReplay
        errorMessage = null

        viewModelScope.launch {
            try {
                // Tragem întrebările generate din baza de date
                val customQuestions = repository.fetchCustomQuestions(quizId)

                if (customQuestions.isNotEmpty()) {
                    questions.clear()
                    questions.addAll(customQuestions)

                    // Ne punem pe prima întrebare
                    currentLearningIndex = 0
                    prepareOptionsForLearning()
                } else {
                    errorMessage = "Could not load the generated questions."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error loading the quiz: ${e.message}"
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

    fun loadUserCustomQuizzes(userId: String) {
        viewModelScope.launch {
            try {
                val quizzes = repository.fetchUserCustomQuizzes(userId)
                userCustomQuizzes.clear()
                userCustomQuizzes.addAll(quizzes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCustomQuizFromLibrary(quizId: String) {
        viewModelScope.launch {
            try {
                // Așteptăm să vedem dacă backend-ul a reușit să-l șteargă
                val success = repository.deleteCustomQuiz(quizId)

                if (success) {
                    // Doar dacă a reușit pe server, îl ștergem și de pe ecran
                    userCustomQuizzes.removeAll { it.id == quizId }
                } else {
                    // Opțional: Poți seta un errorMessage aici ca să arăți un Toast/Snackbar utilizatorului
                    errorMessage = "Could not delete the quiz. Please check your connection."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "An error occurred during deletion."
            }
        }
    }

    // =========================================================================================
    // 7. LOGICĂ: MOD LEARNING
    // =========================================================================================
    fun loadLearningQuestions(categoryUI: String, difficulty: String, isUtm: Boolean) {
        // 1. PREVENIM DUBLU-CLICK: Dacă deja se încarcă, ignorăm orice altă apăsare de buton
        if (isLoading) return

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
                    // 2. SIGURANȚĂ EXTRA: Curățăm lista fix înainte să adăugăm datele noi
                    questions.clear()
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

    // =========================================================================================
    // 8. LOGICĂ: AI & CUSTOM QUIZZES (GEMINI)
    // =========================================================================================
    fun generateAiExplanation() {
        if (isAiLoading || questions.isEmpty()) return

        val currentQuestion = questions[currentLearningIndex]
        isAiLoading = true
        aiExplanation = null

        viewModelScope.launch {
            try {
                // Dacă e Custom Quiz, luăm din tabelul custom_questions. Altfel, apelăm Gemini normal.
                val explanation = if (isCustomQuizMode) {
                    repository.getCustomQuestionExplanation(currentQuestion.question_text)
                } else {
                    aiRepository.getExplanation(
                        questionId = currentQuestion.id,
                        isUtm = isUtmMode
                    )
                }
                aiExplanation = explanation
            } catch (e: Exception) {
                e.printStackTrace()
                aiExplanation = "An error occurred while getting the explanation."
            } finally {
                isAiLoading = false
            }
        }
    }

    fun resetGenerateQuizState() {
        generateQuizError = null
        generateQuizSuccess = false
    }

    // --- FUNCȚIA DE GENERARE ---
    fun generateQuizFromPdf(
        fileBytes: ByteArray,
        originalFileName: String,
        userId: String,
        quizTitle: String
    ) {
        viewModelScope.launch {
            isGeneratingQuiz = true
            generateQuizError = null
            generateQuizSuccess = false
            generatedQuizId = null

            try {
                // 1. Creăm un nume unic pentru fișier (UUID + Nume Original)
                val uniqueFileName = "${java.util.UUID.randomUUID()}_${originalFileName}"
                val filePath = "$userId/$uniqueFileName"

                // 2. Urcăm fișierul în bucket-ul Supabase
                SupabaseClient.client.storage
                    .from("pdf_uploads")
                    .upload(filePath, fileBytes)

                // 3. Chemăm Edge Function-ul
                val jsonBodyString = buildJsonObject {
                    put("filePath", filePath)
                    put("userId", userId)
                    put("categoryName", quizTitle)
                }.toString()

                val response = SupabaseClient.client.functions.invoke("generate-custom-quiz") {
                    setBody(jsonBodyString)
                    header("Content-Type", "application/json")
                    // Mărim timpul de așteptare la 100 de secunde (AI-ul are nevoie de timp pt PDF-uri)
                    timeout {
                        requestTimeoutMillis = 100_000
                    }
                }

                // 4. Parsăm răspunsul serverului
                val responseText = response.bodyAsText()
                val responseData = Json.parseToJsonElement(responseText).jsonObject

                val success = responseData["success"]?.jsonPrimitive?.booleanOrNull ?: false

                if (success) {
                    // Succes total!
                    generateQuizSuccess = true
                    generatedQuizId = responseData["quizId"]?.jsonPrimitive?.content
                } else {
                    // Eroare de la AI
                    generateQuizError = responseData["error"]?.jsonPrimitive?.content ?: "The AI couldn't generate a quiz from this document."
                }

            } catch (e: Exception) {
                e.printStackTrace()

                // --- TRADUCEM ERORILE TEHNICE ÎN MESAJE PRIETENOASE ---
                val errorString = e.message ?: ""
                generateQuizError = when {
                    errorString.contains("timeout", ignoreCase = true) ->
                        "The AI is taking too long to read this document. Please try a smaller PDF or try again later."
                    errorString.contains("503") || errorString.contains("busy", ignoreCase = true) ->
                        "The AI model is currently busy. Please try again in a few minutes."
                    errorString.contains("JSON", ignoreCase = true) || errorString.contains("Expected", ignoreCase = true) ->
                        "The AI couldn't properly format the questions from this document. Please try a different PDF."
                    else ->
                        "Oops! Something went wrong while analyzing the document. Please try another one."
                }
            } finally {
                isGeneratingQuiz = false
            }
        }
    }

    fun saveCustomQuizAndExit(onComplete: () -> Unit) {
        val quizId = generatedQuizId
        if (quizId != null && customQuizTitleInput.isNotBlank()) {
            viewModelScope.launch {
                repository.updateCustomQuizTitle(quizId, customQuizTitleInput)
                generatedQuizId = null
                resetQuizState()
                onComplete()
            }
        } else {
            generatedQuizId = null
            resetQuizState()
            onComplete()
        }
    }

    fun discardCustomQuizAndExit(onComplete: () -> Unit) {
        val quizId = generatedQuizId
        if (quizId != null) {
            viewModelScope.launch {
                repository.deleteCustomQuiz(quizId) // Șterge din baza de date
                generatedQuizId = null
                resetQuizState()
                onComplete()
            }
        } else {
            generatedQuizId = null
            resetQuizState()
            onComplete()
        }
    }

    // =========================================================================================
    // 9. LOGICĂ: SALVARE PROGRESS & STATISTICI
    // =========================================================================================
    private fun checkAndSaveScore() {
        val user = currentUser
        val isAnonymous = user?.appMetadata?.get("provider")?.jsonPrimitive?.content == "anonymous" || user == null
        val shouldSave = isTimedMode || isSuddenDeathMode

        if (!isAnonymous && user != null && shouldSave) {
            viewModelScope.launch {
                try {
                    val username = user.userMetadata?.get("username")?.jsonPrimitive?.content ?: "Explorer"
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

    fun saveProgressAndExit(isGuest: Boolean, onComplete: () -> Unit) {
        val user = currentUser
        if (isLearningMode && questions.isNotEmpty() && !isGuest && user != null) {
            viewModelScope.launch {
                try {
                    val currentQuestion = questions[currentLearningIndex]
                    val progress = UserLearningProgress(
                        userId = user.id,
                        categoryName = currentQuestion.category,
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

    suspend fun getLeaderboard(mode: String, isUtm: Boolean) = repository.getGlobalLeaderboard(mode, isUtm)
    suspend fun getPersonalBestScore(userId: String, mode: String, isUtm: Boolean) = repository.getPersonalBest(userId, mode, isUtm)

    // =========================================================================================
    // 10. UTILITARE PRIVATE
    // =========================================================================================
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

    private fun prepareCurrentOptions() {
        if (questions.isNotEmpty()) {
            val q = questions[0]
            currentOptions = (q.incorrect_answers + q.correct_answer).shuffled()
        }
    }
}