package com.ionut.quizapp.features.profile

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.features.core.theme.QuizAppTheme
import com.ionut.quizapp.features.core.theme.QuizTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // Forțăm tema standard pentru ecranul de bun venit
    QuizAppTheme(isUtmMode = false) {

        // State-uri pentru câmpurile de text
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val focusManager = LocalFocusManager.current

        // --- LOGICĂ INTERNĂ ---

        val performLogin = {
            if (validateLoginInputs(email, password) { errorMessage = it }) {
                isLoading = true
                errorMessage = null
                viewModel.loginAsUser(email, password) { success ->
                    isLoading = false
                    if (success) onLoginSuccess()
                    else errorMessage = "Login failed! Check your credentials."
                }
            }
        }

        // --- UI STRUCTURE ---

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = QuizTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Branding Section
                Text(
                    text = "QuizAdventure",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Input Section
                LoginInputFields(
                    email = email,
                    onEmailChange = { email = it; errorMessage = null },
                    password = password,
                    onPasswordChange = { password = it; errorMessage = null },
                    focusManager = focusManager,
                    onDone = { performLogin() }
                )

                // Error Handling
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Section
                if (isLoading) {
                    CircularProgressIndicator(color = QuizTheme.colors.primary)
                } else {
                    LoginActionButtons(
                        onLoginClick = { performLogin() },
                        onSignUpClick = onSignUpClick,
                        onGuestClick = { viewModel.loginAsGuest { onGuestLogin() } }
                    )
                }
            }
        }
    }
}

// ========================== COMPONENTE UI (INPUTS & BUTTONS) ==========================

@Composable
private fun LoginInputFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onDone: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = QuizTheme.colors.primary,
        focusedLabelColor = QuizTheme.colors.primary,
        cursorColor = QuizTheme.colors.primary
    )

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onDone()
            })
        )
    }
}

@Composable
private fun LoginActionButtons(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = QuizTheme.colors.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSignUpClick) {
            Text(
                text = "Don't have an account? Sign Up",
                color = QuizTheme.colors.secondary,
                fontWeight = FontWeight.Medium
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.5f)
        )

        OutlinedButton(
            onClick = onGuestClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, QuizTheme.colors.accent),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = QuizTheme.colors.textMain)
        ) {
            Text("Play as Guest", fontWeight = FontWeight.Bold)
        }
    }
}

// ========================== UTILS & VALIDARE ==========================

private fun validateLoginInputs(
    email: String,
    password: String,
    onError: (String) -> Unit
): Boolean {
    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        onError("Please enter a valid email address.")
        return false
    }
    if (password.length < 6) {
        onError("Password must be at least 6 characters.")
        return false
    }
    return true
}