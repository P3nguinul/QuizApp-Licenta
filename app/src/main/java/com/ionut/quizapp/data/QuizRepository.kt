package com.ionut.quizapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class QuizRepository {

    private val postgrest = SupabaseClient.client.postgrest
    private val pdfBucket = SupabaseClient.client.storage.from("pdf_uploads")

    // ==========================================
    // GESTIONARE ÎNTREBĂRI (CORE & CUSTOM)
    // ==========================================

    suspend fun fetchQuestions(isUtm: Boolean, categories: List<String>, count: Int): List<Question> {
        val dbCategories = mapCategoriesToDb(categories)

        return postgrest.rpc(
            function = "get_random_questions",
            parameters = buildJsonObject {
                put("is_utm_param", isUtm)
                put("count_param", count)
                putJsonArray("categories_param") {
                    dbCategories.forEach { add(it) }
                }
            }
        ).decodeList<Question>()
    }

    suspend fun fetchQuestionsOrdered(category: String, difficulty: String, isUtm: Boolean): List<Question> {
        val dbCategory = if (isUtm) category else mapCategoriesToDb(listOf(category)).first()

        return SupabaseClient.client.from("questions")
            .select {
                filter {
                    eq("category", dbCategory)
                    eq("difficulty", difficulty.lowercase().trim())
                    eq("is_student_content", isUtm)
                }
                order("id", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<Question>()
    }

    suspend fun fetchCustomQuestions(quizId: String): List<Question> {
        val response = SupabaseClient.client.from("custom_questions")
            .select {
                filter { eq("quiz_id", quizId) }
            }.decodeList<JsonObject>()

        return response.map { item ->
            val optionsArray = item["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val correctAnswer = item["correct_answer"]?.jsonPrimitive?.content ?: ""

            Question(
                id = item["id"]?.jsonPrimitive?.content?.hashCode() ?: 0,
                category = "Custom AI Quiz",
                difficulty = "Medium",
                question_text = item["question_text"]?.jsonPrimitive?.content ?: "",
                correct_answer = correctAnswer,
                incorrect_answers = optionsArray.filter { it != correctAnswer },
                is_student_content = false
            )
        }
    }

    suspend fun fetchAllQuestionCounts(isUtm: Boolean): List<CategoryCountResponse> {
        return try {
            postgrest.rpc(
                function = "get_category_counts",
                parameters = buildJsonObject { put("is_utm_param", isUtm) }
            ).decodeList<CategoryCountResponse>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // LEADERBOARD & STATISTICI
    // ==========================================

    suspend fun updateTopScore(userId: String, username: String, newScore: Int, mode: String, isUtm: Boolean): Boolean {
        return try {
            val existingEntry = SupabaseClient.client.from("leaderboard")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("game_mode", mode)
                        eq("is_utm", isUtm)
                    }
                }.decodeSingleOrNull<LeaderboardEntry>()

            if (existingEntry == null) {
                val newEntry = LeaderboardEntry(
                    user_id = userId, username = username, score = newScore, game_mode = mode, is_utm = isUtm
                )
                SupabaseClient.client.from("leaderboard").insert(newEntry)
                true
            } else if (newScore > existingEntry.score) {
                SupabaseClient.client.from("leaderboard").update(
                    {
                        set("score", newScore)
                        set("username", username)
                    }
                ) {
                    filter { eq("id", existingEntry.id!!) }
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getGlobalLeaderboard(mode: String, isUtm: Boolean): List<LeaderboardEntry> {
        return try {
            SupabaseClient.client.from("leaderboard")
                .select(columns = io.github.jan.supabase.postgrest.query.Columns.raw("*, profiles(avatar_id)")) {
                    filter {
                        eq("game_mode", mode)
                        eq("is_utm", isUtm)
                    }
                    order("score", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(15)
                }.decodeList<LeaderboardEntry>()
        } catch (e: Exception) {
            Log.e("SUPABASE_ERROR", "Eroare la Leaderboard JOIN: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPersonalBest(userId: String, mode: String, isUtm: Boolean): Int {
        return try {
            val result = SupabaseClient.client.from("leaderboard")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("game_mode", mode)
                        eq("is_utm", isUtm)
                    }
                    order("score", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<LeaderboardEntry>()
            result?.score ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // ==========================================
    // PROGRES ÎNVĂȚARE (LEARNING MODE)
    // ==========================================

    suspend fun getAllUserProgress(userId: String, isUtm: Boolean): List<UserLearningProgress> {
        return try {
            SupabaseClient.client.from("user_learning_progress")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_utm", isUtm)
                    }
                }.decodeList<UserLearningProgress>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveLearningProgress(progress: UserLearningProgress) {
        try {
            SupabaseClient.client.from("user_learning_progress").upsert(
                value = progress,
                onConflict = "user_id,category_name,difficulty,is_utm"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // STOCARE PDF & AI QUIZZES
    // ==========================================

    suspend fun uploadPdfForAi(fileName: String, fileBytes: ByteArray): String {
        return withContext(Dispatchers.IO) {
            try {
                pdfBucket.upload(path = fileName, data = fileBytes, upsert = true)
                pdfBucket.publicUrl(fileName)
            } catch (e: Exception) {
                throw Exception("Failed to upload PDF: ${e.message}")
            }
        }
    }

    suspend fun deletePdf(fileName: String) {
        withContext(Dispatchers.IO) {
            try {
                pdfBucket.delete(listOf(fileName))
            } catch (e: Exception) { /* Silențios */ }
        }
    }

    suspend fun fetchUserCustomQuizzes(userId: String): List<CustomQuiz> {
        return try {
            SupabaseClient.client.from("custom_quizzes")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }.decodeList<CustomQuiz>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateCustomQuizTitle(quizId: String, newTitle: String) {
        try {
            SupabaseClient.client.from("custom_quizzes").update({ set("title", newTitle) }) {
                filter { eq("id", quizId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCustomQuiz(quizId: String): Boolean {
        return try {
            SupabaseClient.client.from("custom_quizzes").delete {
                filter { eq("id", quizId) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCustomQuestionExplanation(questionText: String): String {
        return try {
            val response = SupabaseClient.client.from("custom_questions")
                .select {
                    filter { eq("question_text", questionText) }
                }.decodeList<JsonObject>().firstOrNull()

            response?.get("ai_explanation")?.jsonPrimitive?.content
                ?: "The AI didn't provide a specific explanation for this question."
        } catch (e: Exception) {
            "Connection error while fetching explanation."
        }
    }

    // ==========================================
    // UTILS & MAPPING
    // ==========================================

    fun mapCategoriesToDb(uiCategories: List<String>): List<String> {
        val mapping = mapOf(
            "General Knowledge" to "General Knowledge",
            "Films" to "Entertainment: Film",
            "Music" to "Entertainment: Music",
            "Television" to "Entertainment: Television",
            "Video Games" to "Entertainment: Video Games",
            "Science and Nature" to "Science & Nature",
            "Computer Science" to "Science: Computers",
            "Mathematics" to "Science: Mathematics",
            "Sports" to "Sports",
            "Geography" to "Geography",
            "History" to "History",
            "Animals" to "Animals",
            "Vehicles" to "Vehicles",
            "Japanese Anime & Manga" to "Entertainment: Japanese Anime & Manga",
            "Comics" to "Entertainment: Comics",
            "Tehnici Avansate de Programare" to "Tehnici Avansate de Programare",
            "Inovare și Transformare Digitală" to "Inovare și Transformare Digitală",
            "Comerț Electronic" to "Comerț Electronic",
            "Criptografie" to "Criptografie",
            "Administrarea Rețelelor de Calculatoare" to "Administrarea Rețelelor de Calculatoare",
            "Metode Avansate De Programare (Java)" to "Metode Avansate De Programare (Java)",
            "Sisteme de Gestiune a Bazelor de Date" to "Sisteme de Gestiune a Bazelor de Date",
            "Programare Orientată pe Obiecte (C++)" to "Programare Orientată pe Obiecte (C++)",
            "Tehnologii Web" to "Tehnologii Web",
            "Sisteme de Operare" to "Sisteme de Operare",
            "Fundamentele Programării" to "Fundamentele Programării",
            "Programare în Python" to "Programare în Python",
            "Algoritmi și Structuri de Date" to "Algoritmi și Structuri de Date",
            "Cloud Computing" to "Cloud Computing",
            "Baze de Date" to "Baze de Date"
        )

        if (uiCategories.contains("All Categories") || uiCategories.contains("All UTM")) {
            return listOf("All")
        }

        return uiCategories.map { mapping[it] ?: it }
    }
}

// Helper extern pentru Uri
fun Uri.toByteArray(context: Context): ByteArray? {
    return context.contentResolver.openInputStream(this)?.use { it.readBytes() }
}