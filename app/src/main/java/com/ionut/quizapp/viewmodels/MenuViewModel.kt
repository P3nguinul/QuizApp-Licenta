package com.ionut.quizapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MenuViewModel : ViewModel() {
    var isUtmMode by mutableStateOf(false)
        private set

    var selectedCategories by mutableStateOf(setOf("All Categories"))
        private set

    var questionCount by mutableIntStateOf(10)
    var isExpended by mutableStateOf(false) // Controlează dacă Dropdown-ul e deschis

    fun updateQuestionCount(count: Int) {
        questionCount = count
        isExpended = false // Închidem meniul după selecție
    }
    fun toggleUtmMode(enabled: Boolean) {
        isUtmMode = enabled
        selectedCategories = if (enabled) setOf("All UTM") else setOf("All Categories")
    }

    fun toggleCategory(category: String) {
        val allLabel = if (isUtmMode) "All UTM" else "All Categories"
        val newSet = selectedCategories.toMutableSet()

        if (category == allLabel) {
            newSet.clear()
            newSet.add(allLabel)
        } else {
            if (newSet.contains(category)) newSet.remove(category)
            else {
                newSet.add(category)
                newSet.remove(allLabel)
            }
        }
        if (newSet.isEmpty()) newSet.add(allLabel)
        selectedCategories = newSet
    }
}