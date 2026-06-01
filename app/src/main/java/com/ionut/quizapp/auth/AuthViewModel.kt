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
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthViewModel : ViewModel() {

    // ==========================================
    // STATE-URI PENTRU UI
    // ==========================================

    var currentUserProfile by mutableStateOf<String?>(null)
        private set

    var isCurrentUserGuest by mutableStateOf(false)
        private set

    var currentUserAvatarId by mutableIntStateOf(1)
        private set

    val currentUserId: String?
        get() = SupabaseClient.client.auth.currentUserOrNull()?.id


    // ==========================================
    // INITIALIZARE
    // ==========================================

    init {
        viewModelScope.launch {
            // Monitorizăm starea sesiunii pentru a actualiza profilul automat la login/logout
            SupabaseClient.client.auth.sessionStatus.collect { _ ->
                fetchProfile()
            }
        }
        fetchProfile()
    }


    // ==========================================
    // LOGICA DE AUTENTIFICARE
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

                // Dacă există deja un user cu identitate, îi dăm logout înainte de guest login
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
            } catch (e: Exception) {
                Log.d("AUTH", "Eroare la delogare server, forțăm delogarea locală: ${e.message}")
            } finally {
                // Resetăm starea locală indiferent de rezultatul apelului de rețea
                currentUserProfile = null
                currentUserAvatarId = 1
                onResult()
            }
        }
    }


    // ==========================================
    // LOGICA DE PROFIL SI AVATAR
    // ==========================================

    fun fetchProfile() {
        viewModelScope.launch {
            try {
                val userId = currentUserId
                if (userId != null) {
                    val profile = SupabaseClient.client.from("profiles")
                        .select {
                            filter { eq("id", userId) }
                        }.decodeSingle<UserProfile>()

                    currentUserProfile = profile.username
                    isCurrentUserGuest = profile.is_guest
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

    fun updateAvatar(newAvatarId: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                SupabaseClient.client.from("profiles").update(
                    { set("avatar_id", newAvatarId) }
                ) {
                    filter { eq("id", userId) }
                }

                currentUserAvatarId = newAvatarId
            } catch (e: Exception) {
                Log.e("AUTH", "Eroare la update avatar: ${e.message}")
            }
        }
    }


    // ==========================================
    // ADMINISTRARE CONT (SETARI)
    // ==========================================

    fun updateUsername(newUsername: String, onResult: (Boolean, String) -> Unit) {
        val userId = currentUserId ?: return onResult(false, "User not logged in.")

        viewModelScope.launch {
            try {
                // Actualizăm în tabela publică 'profiles'
                SupabaseClient.client.from("profiles").update(
                    { set("username", newUsername) }
                ) {
                    filter { eq("id", userId) }
                }

                // Actualizăm metadata din Auth pentru consistență la nivel de sesiune
                SupabaseClient.client.auth.updateUser {
                    data = buildJsonObject {
                        put("username", newUsername)
                    }
                }

                currentUserProfile = newUsername
                onResult(true, "Username updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Failed to update username.")
            }
        }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.updateUser {
                    password = newPassword
                }
                onResult(true, "Password updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Failed to update password.")
            }
        }
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // Apelăm RPC-ul de pe server pentru ștergere definitivă (Trigger SQL)
                SupabaseClient.client.postgrest.rpc("delete_user")

                try {
                    SupabaseClient.client.auth.signOut()
                } catch (authError: Exception) {
                    Log.d("AUTH", "SignOut ignored after deletion: ${authError.message}")
                }

                currentUserProfile = null
                currentUserAvatarId = 1
                onResult(true, "Account permanently deleted.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Failed to delete account.")
            }
        }
    }
}