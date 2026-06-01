package com.ionut.quizapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class MenuViewModel : ViewModel() {

    // --- STATE-URI DE CONFIGURARE ---

    var isUtmMode by mutableStateOf(false)
        private set

    var selectedCategories by mutableStateOf(setOf("All Categories"))
        private set

    var questionCount by mutableIntStateOf(10)
        private set

    var isExpended by mutableStateOf(false)

    // --- ACȚIUNI (ACTIONS) ---

    /**
     * Schimbă numărul de întrebări și închide meniul dropdown.
     */
    fun updateQuestionCount(count: Int) {
        questionCount = count
        isExpended = false
    }

    /**
     * Comută între modul UTM și modul Normal, resetând categoriile.
     */
    fun toggleUtmMode(enabled: Boolean) {
        isUtmMode = enabled
        selectedCategories = if (enabled) setOf("All UTM") else setOf("All Categories")
    }

    /**
     * Gestionează selecția categoriilor (Multi-select logic).
     * Dacă se selectează "All", celelalte se șterg.
     */
    fun toggleCategory(category: String) {
        val allLabel = if (isUtmMode) "All UTM" else "All Categories"
        val newSet = selectedCategories.toMutableSet()

        if (category == allLabel) {
            newSet.clear()
            newSet.add(allLabel)
        } else {
            // Eliminăm "All" dacă selectăm o categorie specifică
            newSet.remove(allLabel)

            if (newSet.contains(category)) {
                newSet.remove(category)
            } else {
                newSet.add(category)
            }
        }

        // Dacă setul a rămas gol, revenim automat la "All"
        if (newSet.isEmpty()) {
            newSet.add(allLabel)
        }

        selectedCategories = newSet
    }
}