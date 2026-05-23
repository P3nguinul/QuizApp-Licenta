package com.ionut.quizapp.data

data class GameMode(
    val title: String,
    val description: String,
    val icon: String
)

val gameModes = listOf(
    GameMode("Classic", "10 questions at your own pace. Try to solve them all!", "Classic"),
    GameMode("Against Time", "Answer as many as you can in 60 seconds! Each correct answer adds 1 second to the timer!", "Timer"),
    GameMode("Sudden Death", "Answer the questions until you get one wrong! One wrong answer and it's game over!", "Skull"),
    GameMode("Learning", "Practice questions by category without pressure.", "Book")
)