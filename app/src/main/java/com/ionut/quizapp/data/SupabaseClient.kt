package com.ionut.quizapp.data

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object SupabaseClient {

    @OptIn(SupabaseInternal::class)
    val client = createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
        install(Storage)

        requestTimeout = 40.seconds

    }
}