package com.ionut.quizapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val category: String,
    val difficulty: String,
    val question_text: String,
    val correct_answer: String,
    val incorrect_answers: List<String>,
    val is_student_content: Boolean
)

// Clasă ajutătoare care citește doar avatar_id-ul din tabelul profiles
@Serializable
data class ProfileJoin(
    val avatar_id: Int? = 1 // <--- Am adăugat semnul de întrebare aici
)

@Serializable
data class LeaderboardEntry(
    val id: Long? = null,
    val user_id: String,
    val username: String,
    val score: Int,
    val game_mode: String,
    val created_at: String? = null,
    val is_utm: Boolean = false,
    // NOU: Aici Supabase va "injecta" automat datele din tabelul profiles datorită JOIN-ului
    @SerialName("profiles") val profiles: ProfileJoin? = null
) {
    // Funcție utilitară ca să extragem ușor ID-ul pentru UI
    fun getAvatarId(): Int {
        return profiles?.avatar_id ?: 1
    }
}
@Serializable
data class UserLearningProgress(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("category_name") val categoryName: String, // Modificat în camelCase + SerialName
    val difficulty: String,
    @SerialName("last_question_index") val lastQuestionIndex: Int = 0,
    @SerialName("is_utm") val isUtm: Boolean = false
)

@Serializable
data class CategoryCountResponse(
    @SerialName("category_name") val categoryName: String,
    @SerialName("difficulty_level") val difficulty: String,
    @SerialName("total_count") val count: Int
)

@kotlinx.serialization.Serializable
data class CustomQuiz(
    val id: String,
    val user_id: String,
    val title: String,
    val created_at: String? = null
)