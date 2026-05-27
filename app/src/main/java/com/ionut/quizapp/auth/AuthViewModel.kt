package com.ionut.quizapp.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    init {
        viewModelScope.launch {
            SupabaseClient.client.auth.sessionStatus.collect { event ->
                fetchProfile()
            }
        }
        fetchProfile()
    }

    val currentUserId: String?
        get() = SupabaseClient.client.auth.currentUserOrNull()?.id

    // ==========================================
    // STATE-URI PENTRU UI
    // ==========================================
    var currentUserProfile by mutableStateOf<String?>(null)
        private set

    var isCurrentUserGuest by mutableStateOf(false)
        private set

    // STATE NOU: Avatarul curent
    var currentUserAvatarId by mutableIntStateOf(1)
        private set

    // ==========================================
    // FUNCȚII DE AUTENTIFICARE
    // ==========================================
    fun signUp(userEmail: String, userPass: String, username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    email = userEmail
                    password = userPass
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
                currentUserAvatarId = 1 // Resetăm la avatarul default
                onResult()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // FUNCȚII PENTRU PROFIL
    // ==========================================
    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull()
                val userId = user?.id

                if (userId != null) {
                    val profile = SupabaseClient.client.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }.decodeSingle<UserProfile>()

                    currentUserProfile = profile.username
                    isCurrentUserGuest = profile.is_guest

                    // NOU: Citim avatarul din baza de date
                    currentUserAvatarId = profile.avatar_id
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Eroare la fetch: ${e.message}")
                currentUserProfile = "Error loading"
                isCurrentUserGuest = false
                currentUserAvatarId = 1
            }
        }
    }

    // NOU: Funcția de salvare a avatarului
    fun updateAvatar(newAvatarId: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // 1. Modificăm în baza de date (tabelul 'profiles')
                SupabaseClient.client.from("profiles").update(
                    {
                        set("avatar_id", newAvatarId)
                    }
                ) {
                    filter { eq("id", userId) }
                }

                // 2. Actualizăm interfața instantaneu
                currentUserAvatarId = newAvatarId

            } catch (e: Exception) {
                Log.e("AUTH", "Eroare la update avatar: ${e.message}")
            }
        }
    }
}