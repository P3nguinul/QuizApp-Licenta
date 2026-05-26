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
    fun loadCustomQuiz(quizId: String) {
        resetQuizState()
        isLoading = true

        // Setăm regulile de bază (fără timer, ca un Classic Mode)
        currentGameLogic = GameModeLogic.Classic(100) // Permitem până la 100 de întrebări
        isTimedMode = false
        isSuddenDeathMode = false
        isUtmMode = false
        isLearningMode = false

        viewModelScope.launch {
            try {
                // Apelează funcția pe care tocmai am făcut-o perfectă în Repository
                val customQuestions = repository.fetchCustomQuestions(quizId)

                if (customQuestions.isNotEmpty()) {
                    questions.clear()
                    questions.addAll(customQuestions)
                    totalQuestionsCount = customQuestions.size
                    prepareCurrentOptions()

                    isGameOver = false
                } else {
                    errorMessage = "Could not load the generated questions."
                    isGameOver = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error loading the quiz: ${e.message}"
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

    // =========================================================================================
    // 7. LOGICĂ: MOD LEARNING
    // =========================================================================================
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

    // =========================================================================================
    // 8. LOGICĂ: AI & CUSTOM QUIZZES (GEMINI)
    // =========================================================================================
    fun generateAiExplanation() {
        if (isAiLoading || questions.isEmpty()) return

        val currentQuestion = questions[currentLearningIndex]
        isAiLoading = true
        aiExplanation = null

        viewModelScope.launch {
            val explanation = aiRepository.getExplanation(
                questionId = currentQuestion.id,
                isUtm = isUtmMode
            )
            aiExplanation = explanation
            isAiLoading = false
        }
    }

    fun uploadPdfForCustomQuiz(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            isUploadingPdf = true
            pdfUploadMessage = "Uploading PDF to Cloud..."

            try {
                val bytes = uri.toByteArray(context)
                    ?: throw Exception("Could not read the file.")

                val fileName = "quiz_doc_${System.currentTimeMillis()}.pdf"
                val publicUrl = repository.uploadPdfForAi(fileName, bytes)

                pdfUploadMessage = "Success! File uploaded."
                println("PDF Uploaded. Public URL: $publicUrl")

                // PASUL URMĂTOR: Apel Edge Function

            } catch (e: Exception) {
                pdfUploadMessage = "Upload error: ${e.message}"
                e.printStackTrace()
            } finally {
                isUploadingPdf = false
            }
        }
    }

    fun resetPdfUploadState() {
        isUploadingPdf = false
        pdfUploadMessage = null
    }

    fun resetGenerateQuizState() {
        generateQuizError = null
        generateQuizSuccess = false
        generatedQuizId = null
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

                // 2. Urcăm fișierul în bucket-ul Supabase (AI-ul îl va șterge automat după)
                // UITE AICI: FĂRĂ io.github.jan.supabase în față, doar SupabaseClient-ul tău!
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
                    // Eroare de la AI (Troll, invalid etc.)
                    generateQuizError = responseData["error"]?.jsonPrimitive?.content ?: "Unknown error occurred during generation."
                }

            } catch (e: Exception) {
                e.printStackTrace()
                generateQuizError = "Connection error: ${e.message}"
            } finally {
                isGeneratingQuiz = false
            }
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