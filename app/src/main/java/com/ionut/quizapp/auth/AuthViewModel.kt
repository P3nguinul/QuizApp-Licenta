package com.ionut.quizapp.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ionut.quizapp.data.SupabaseClient
import com.ionut.quizapp.data.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {

    val currentUserId: String?
        get() = SupabaseClient.client.auth.currentUserOrNull()?.id
    fun signUp(userEmail: String, userPass: String, username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Trimitem username-ul în raw_user_meta_data pentru ca Trigger-ul să-l poată citi
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = userEmail
                    password = userPass
                    // Această linie trimite datele către Trigger-ul SQL
                    data = buildJsonObject {
                        put("username", username)
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    // Funcție pentru logarea unui utilizator existent
    fun loginAsUser(userEmail: String, userPass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    email = userEmail
                    password = userPass
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()

                // Dacă există un user și NU este anonim, îi dăm Sign Out
                // DOAR pentru a face loc sesiunii de Guest
                if (currentUser != null && currentUser.identities?.isEmpty() == false) {
                    SupabaseClient.client.auth.signOut()
                }

                SupabaseClient.client.auth.signInAnonymously()
                fetchProfile()
                onSuccess()
            } catch (e: Exception) {
                Log.e("AUTH", "Guest login failed: ${e.message}")
            }
        }
    }

    fun logout(onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                currentUserProfile = null
                onResult()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Add this inside AuthViewModel class
    var currentUserProfile by mutableStateOf<String?>(null)
        private set
    var isCurrentUserGuest by mutableStateOf(false)
        private set

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val userId = user?.id

                if (userId != null) {
                    // Fetch the username from the profiles table
                    val profile = SupabaseClient.client.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }.decodeSingle<UserProfile>()

                    currentUserProfile = profile.username
                    isCurrentUserGuest = profile.is_guest

                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Eroare la fetch: ${e.message}")
                currentUserProfile = "Error loading"
                isCurrentUserGuest = false
            }
        }
    }
}