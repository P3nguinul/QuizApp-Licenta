package com.ionut.quizapp.data

import android.util.Log
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AiRepository {

    suspend fun getExplanation(questionId: Int, isUtm: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                // Apelăm Edge Function-ul creat pe Supabase
                val response = SupabaseClient.client.functions.invoke(
                    "generate-explanation",
                    body = buildJsonObject {
                        put("question_id", questionId)
                        put("is_utm", isUtm)
                    }
                )

                val responseText = response.bodyAsText()

                // Funcția noastră din Cloud returnează: { "explanation": "textul..." }
                val responseJson = Json.parseToJsonElement(responseText).jsonObject
                responseJson["explanation"]?.jsonPrimitive?.content
                    ?: "Could not generate an explanation at this time."

            } catch (e: Exception) {
                Log.e("AI_ERROR", "Error calling Edge Function: ${e.message}")
                "Connection error. Please try again."
            }
        }
    }
}