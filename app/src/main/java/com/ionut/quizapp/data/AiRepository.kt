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

    // ==========================================
    // CONSTANTE SI CONFIGURARE
    // ==========================================

    private val FUNCTION_NAME = "generate-explanation"
    private val KEY_EXPLANATION = "explanation"


    // ==========================================
    // LOGICA DE GENERARE EXPLICATII (AI)
    // ==========================================

    suspend fun getExplanation(questionId: Int, isUtm: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Apelăm Edge Function-ul de pe serverul Supabase
                val response = SupabaseClient.client.functions.invoke(
                    function = FUNCTION_NAME,
                    body = buildJsonObject {
                        put("question_id", questionId)
                        put("is_utm", isUtm)
                    }
                )

                val responseText = response.bodyAsText()

                // 2. Parsăm răspunsul JSON primit de la Edge Function
                val responseJson = Json.parseToJsonElement(responseText).jsonObject

                // 3. Returnăm textul explicației sau un mesaj de fallback
                responseJson[KEY_EXPLANATION]?.jsonPrimitive?.content
                    ?: "Could not generate an explanation at this time."

            } catch (e: Exception) {
                Log.e("AI_ERROR", "Error calling Edge Function: ${e.message}")
                "Connection error. Please try again later."
            }
        }
    }
}