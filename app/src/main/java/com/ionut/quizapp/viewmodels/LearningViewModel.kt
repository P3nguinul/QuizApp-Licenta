package com.ionut.quizapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionut.quizapp.data.QuizRepository
import com.ionut.quizapp.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

// Model de date pentru progresul pe o dificultate specifică
data class ProgressData(val current: Int, val total: Int)

class LearningViewModel(
    private val repository: QuizRepository = QuizRepository()
) : ViewModel() {

    // --- STATE-URI UI ---
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var detailedProgressMap by mutableStateOf<Map<String, Map<String, ProgressData>>>(emptyMap())
        private set

    // --- CONFIGURAȚIE CATEGORII ---
    val normalCategoriesUI = listOf(
        "General Knowledge", "Films", "Music", "Television", "Video Games",
        "Science and Nature", "Computer Science", "Mathematics", "Sports",
        "Geography", "History", "Animals", "Vehicles",
        "Japanese Anime & Manga", "Comics"
    )

    val utmCategoriesUI = listOf(
        "Tehnici Avansate de Programare", "Inovare și Transformare Digitală", "Comerț Electronic", "Criptografie",
        "Administrarea Rețelelor de Calculatoare", "Metode Avansate De Programare (Java)", "Sisteme de Gestiune a Bazelor de Date",
        "Programare Orientată pe Obiecte (C++)", "Tehnologii Web", "Sisteme de Operare", "Fundamentele Programării", "Programare în Python",
        "Algoritmi și Structuri de Date", "Cloud Computing", "Baze de Date"
    )

    private val currentUser get() = SupabaseClient.client.auth.currentUserOrNull()

    // --- LOGICĂ BUSINESS ---

    fun loadDetailedProgress(isUtmMode: Boolean) {
        val user = currentUser ?: return
        val categoriesUI = if (isUtmMode) utmCategoriesUI else normalCategoriesUI
        val difficulties = listOf("Easy", "Medium", "Hard")

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val allCounts = repository.fetchAllQuestionCounts(isUtmMode)
                val allUserProgress = repository.getAllUserProgress(user.id, isUtmMode)

                val mainResults = mutableMapOf<String, Map<String, ProgressData>>()

                categoriesUI.forEach { uiCat ->
                    val dbCat = if (isUtmMode) uiCat else repository.mapCategoriesToDb(listOf(uiCat)).first()
                    val diffMap = mutableMapOf<String, ProgressData>()

                    difficulties.forEach { diff ->
                        // Calculăm totalul și progresul folosind funcții de extensie pentru claritate
                        val total = calculateTotalQuestions(allCounts, dbCat, uiCat, diff)
                        val current = calculateUserProgress(allUserProgress, dbCat, uiCat, diff)

                        diffMap[diff] = ProgressData(current = current, total = total)
                    }
                    mainResults[uiCat] = diffMap
                }
                detailedProgressMap = mainResults

            } catch (e: java.net.UnknownHostException) {
                errorMessage = "No internet connection. Please check your network and try again."
            } catch (e: Exception) {
                errorMessage = "Failed to load progress: ${e.localizedMessage ?: "Unknown error occurred"}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // --- METODE PRIVATE DE AJUTOR ---

    private fun calculateTotalQuestions(counts: List<com.ionut.quizapp.data.CategoryCountResponse>, dbCat: String, uiCat: String, diff: String): Int {
        return counts.filter {
            (it.categoryName.trim().equals(dbCat.trim(), ignoreCase = true) ||
                    it.categoryName.trim().equals(uiCat.trim(), ignoreCase = true)) &&
                    it.difficulty.trim().equals(diff.trim(), ignoreCase = true)
        }.sumOf { it.count }
    }

    private fun calculateUserProgress(progressList: List<com.ionut.quizapp.data.UserLearningProgress>, dbCat: String, uiCat: String, diff: String): Int {
        return progressList.find {
            (it.categoryName.trim().equals(dbCat.trim(), ignoreCase = true) ||
                    it.categoryName.trim().equals(uiCat.trim(), ignoreCase = true)) &&
                    it.difficulty.trim().equals(diff.trim(), ignoreCase = true)
        }?.lastQuestionIndex ?: 0
    }
}