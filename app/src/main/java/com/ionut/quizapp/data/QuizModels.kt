package com.ionut.quizapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// MODELE PENTRU QUIZ (INTREBARI)
// ==========================================

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

@Serializable
data class CustomQuiz(
    val id: String,
    val user_id: String,
    val title: String,
    val created_at: String? = null
)

@Serializable
data class CategoryCountResponse(
    @SerialName("category_name") val categoryName: String,
    @SerialName("difficulty_level") val difficulty: String,
    @SerialName("total_count") val count: Int
)

// ==========================================
// MODELE PENTRU LEADERBOARD & PROFILE JOIN
// ==========================================

@Serializable
data class ProfileJoin(
    @SerialName("avatar_id") val avatar_id: Int? = 1
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
    @SerialName("profiles") val profiles: ProfileJoin? = null
) {
    /**
     * Extrage ID-ul avatarului din obiectul joinat 'profiles'.
     * Returnează 1 (default) dacă datele lipsesc.
     */
    fun getAvatarId(): Int {
        return profiles?.avatar_id ?: 1
    }
}

// ==========================================
// MODELE PENTRU PROGRES & INVATARE
// ==========================================

@Serializable
data class UserLearningProgress(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("category_name") val categoryName: String,
    val difficulty: String,
    @SerialName("last_question_index") val lastQuestionIndex: Int = 0,
    @SerialName("is_utm") val isUtm: Boolean = false
)

// ==========================================
// MODELE PENTRU UTILIZATOR (PROFIL)
// ==========================================

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    @SerialName("is_guest") val is_guest: Boolean = false,
    @SerialName("avatar_id") val avatar_id: Int = 1
)