package com.ionut.quizapp.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class QuizRepository {
    private val postgrest = SupabaseClient.client.postgrest

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

    suspend fun fetchAllQuestionCounts(isUtm: Boolean): List<CategoryCountResponse> {
        return try {
            postgrest.rpc(
                function = "get_category_counts",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("is_utm_param", isUtm)
                }
            ).decodeList<CategoryCountResponse>()
        } catch (e: Exception) {
            emptyList()
        }
    }

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
            "Comics" to "Entertainment: Comics"
        )

        if (uiCategories.contains("All Categories") || uiCategories.contains("All UTM")) {
            return listOf("All")
        }

        return uiCategories.map { mapping[it] ?: it }
    }

    suspend fun updateTopScore(userId: String, username: String, newScore: Int, mode: String, isUtm: Boolean): Boolean {
        return try {
            // 1. Căutăm dacă există deja un record pentru acest user, în acest mod ȘI acest tip (UTM/Normal)
            val existingEntry = SupabaseClient.client.from("leaderboard")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("game_mode", mode)
                        eq("is_utm", isUtm) // Foarte important pentru separare
                    }
                }.decodeSingleOrNull<LeaderboardEntry>()

            if (existingEntry == null) {
                // 2. Dacă nu există, creăm unul nou
                val newEntry = LeaderboardEntry(
                    user_id = userId,
                    username = username,
                    score = newScore,
                    game_mode = mode,
                    is_utm = isUtm
                )
                SupabaseClient.client.from("leaderboard").insert(newEntry)
                true
            } else if (newScore > existingEntry.score) {
                // 3. Dacă există, dar scorul nou e mai mare, actualizăm
                SupabaseClient.client.from("leaderboard").update(
                    {
                        set("score", newScore)
                        set("username", username) // Actualizăm și numele în caz că s-a schimbat
                    }
                ) {
                    filter {
                        eq("id", existingEntry.id!!) // Folosim ID-ul unic pentru update sigur
                    }
                }
                true
            } else {
                false // Nu este un record nou
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getGlobalLeaderboard(mode: String, isUtm: Boolean): List<LeaderboardEntry> {
        return SupabaseClient.client.from("leaderboard")
            .select {
                filter {
                    eq("game_mode", mode)
                    eq("is_utm", isUtm)
                }
                order("score", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(15)
            }.decodeList<LeaderboardEntry>()
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
                    // Sortăm descrescător după scor și luăm prima intrare
                    order("score", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<LeaderboardEntry>()
            result?.score ?: 0
        } catch (e: Exception) {
            0
        }
    }

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

}