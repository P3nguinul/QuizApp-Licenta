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
    var errorMessage by mutableStateOf<String?>(null)


    var detailedProgressMap by mutableStateOf<Map<String, Map<String, ProgressData>>>(emptyMap())
        private set

    // --- DATELE TALE OFICIALE ---
    val normalCategoriesUI = listOf(
        "General Knowledge", "Films", "Music", "Television", "Video Games",
        "Science and Nature", "Computer Science", "Mathematics", "Sports",
        "Geography", "History", "Animals", "Vehicles",
        "Japanese Anime & Manga", "Comics"
    )

    val utmCategoriesUI = listOf(
        "Programarea Calculatoarelor", "Baze de Date",
        "Arhitectura Sistemelor", "Retele de Calculatoare"
    )

    private val currentUser get() = SupabaseClient.client.auth.currentUserOrNull()

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
                        val countInfo = allCounts.find {
                            (it.categoryName.trim().equals(dbCat.trim(), ignoreCase = true) ||
                                    it.categoryName.trim().equals(uiCat.trim(), ignoreCase = true)) &&
                                    it.difficulty.trim().lowercase() == diff.trim().lowercase()
                        }
                        val total = countInfo?.count ?: 0

                        val progressInfo = allUserProgress.find {
                            (it.categoryName.trim().equals(dbCat.trim(), ignoreCase = true) ||
                                    it.categoryName.trim().equals(uiCat.trim(), ignoreCase = true)) &&
                                    it.difficulty.trim().lowercase() == diff.trim().lowercase()
                        }
                        val current = progressInfo?.lastQuestionIndex ?: 0

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
}