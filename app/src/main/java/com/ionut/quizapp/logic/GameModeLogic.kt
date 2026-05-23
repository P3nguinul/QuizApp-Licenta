package com.ionut.quizapp.logic

sealed class GameModeLogic {
    abstract val hasTimer: Boolean
    abstract val bonusTimePerCorrect: Int
    abstract fun shouldEndGame(mistakes: Int, totalAnswered: Int, questionsLeft: Int): Boolean

    // Modul Clasic
    data class Classic(val targetCount: Int) : GameModeLogic() {
        override val hasTimer = false
        override val bonusTimePerCorrect = 0
        override fun shouldEndGame(m: Int, total: Int, left: Int) = total >= targetCount || left == 0
    }

    // Modul Contra Cronometru
    object AgainstTime : GameModeLogic() {
        override val hasTimer = true
        override val bonusTimePerCorrect = 1
        override fun shouldEndGame(m: Int, total: Int, left: Int) = false // Se termină doar la timp = 0
    }

    object SuddenDeath : GameModeLogic() {
        override val hasTimer = false
        override val bonusTimePerCorrect = 0
        override fun shouldEndGame(m: Int, total: Int, left: Int) = m > 0
    }
}