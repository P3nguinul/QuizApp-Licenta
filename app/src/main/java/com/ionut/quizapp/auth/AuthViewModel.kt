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
                // 1. Încercăm să anunțăm serverul civilizat
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                // Dacă serverul dă eroare (ex: contul a fost șters deja), ignorăm eroarea!
                Log.d("AUTH", "Eroare la delogare server, forțăm delogarea locală: ${e.message}")
            } finally {
                // Blocul 'finally' se execută ABSOLUT MEREU, indiferent dacă a fost eroare sau succes.
                // 2. Curățăm memoria aplicației
                currentUserProfile = null
                currentUserAvatarId = 1

                // 3. Executăm navigarea către ecranul de Login!
                onResult()
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

    // --- SETĂRI CONT ---

    fun updateUsername(newUsername: String, onResult: (Boolean, String) -> Unit) {
        val userId = currentUserId ?: return onResult(false, "User not logged in.")

        viewModelScope.launch {
            try {
                // 1. Actualizăm în tabelul public 'profiles' (de unde citim la fetchProfile)
                SupabaseClient.client.from("profiles").update(
                    {
                        set("username", newUsername)
                    }
                ) {
                    filter { eq("id", userId) }
                }

                // 2. Actualizăm și în metadata din Auth pentru consistență
                SupabaseClient.client.auth.updateUser {
                    data = buildJsonObject {
                        put("username", newUsername)
                    }
                }

                // 3. Actualizăm variabila locală ca să se schimbe instant pe ecran
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
                // Supabase se ocupă automat de criptarea noii parole
                SupabaseClient.client.auth.updateUser {
                    password = newPassword
                }
                onResult(true, "Password updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Failed to update password. It must be at least 6 characters.")
            }
        }
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Apelăm funcția SQL pentru a șterge definitiv utilizatorul de pe server
                SupabaseClient.client.postgrest.rpc("delete_user")

                // 2. Încercăm să dăm signOut local. Învăluim într-un try-catch intern
                // pentru că dacă serverul dă eroare (deoarece user-ul e deja șters), o ignorăm.
                try {
                    SupabaseClient.client.auth.signOut()
                } catch (authError: Exception) {
                    // Ignorăm eroarea de rețea, curățarea locală a sesiunii s-a făcut oricum
                    Log.d("AUTH", "SignOut error ignored after account deletion: ${authError.message}")
                }

                // 3. Curățăm datele din aplicație
                currentUserProfile = null
                currentUserAvatarId = 1

                // 4. Întoarcem succes = true ca ecranul să poată schimba pagina!
                onResult(true, "Account permanently deleted.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.message ?: "Failed to delete account.")
            }
        }
    }
}