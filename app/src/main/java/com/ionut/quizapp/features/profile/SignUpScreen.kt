package com.ionut.quizapp.features.profile

import android.os.Build
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.features.core.theme.QuizAppTheme
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.auth.AuthViewModel

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // Forțăm tema standard pentru consistență cu ecranul de Login
    QuizAppTheme(isUtmMode = false) {

        // State-uri pentru datele de intrare
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val focusManager = LocalFocusManager.current

        // --- LOGICĂ INTERNĂ ---

        val performSignUp = {
            if (validateSignUpInputs(username, email, password, confirmPassword) { errorMessage = it }) {
                isLoading = true
                errorMessage = null
                viewModel.signUp(email.trim(), password, username.trim()) { success ->
                    isLoading = false
                    if (success) onSignUpSuccess()
                    else errorMessage = "Registration failed! Email might be already taken."
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
                // Header
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Section
                SignUpFields(
                    username = username,
                    onUsernameChange = { username = it; errorMessage = null },
                    email = email,
                    onEmailChange = { email = it; errorMessage = null },
                    password = password,
                    onPasswordChange = { password = it; errorMessage = null },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it; errorMessage = null },
                    focusManager = focusManager,
                    onDone = { performSignUp() }
                )

                // Error Area
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Section
                if (isLoading) {
                    CircularProgressIndicator(color = QuizTheme.colors.primary)
                } else {
                    Button(
                        onClick = { performSignUp() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuizTheme.colors.primary)
                    ) {
                        Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onBackToLogin) {
                        Text(
                            text = "Already have an account? Log In",
                            color = QuizTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// ========================== COMPONENTE FORMULAR (INPUTS) ==========================

@Composable
private fun SignUpFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onDone: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = QuizTheme.colors.primary,
        focusedLabelColor = QuizTheme.colors.primary,
        cursorColor = QuizTheme.colors.primary
    )

    Column {
        // Username
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
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

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
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

// ========================== UTILS & VALIDARE ==========================

private fun validateSignUpInputs(
    username: String,
    email: String,
    pass: String,
    confirmPass: String,
    onError: (String) -> Unit
): Boolean {
    if (username.trim().length < 3) {
        onError("Username must be at least 3 characters.")
        return false
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
        onError("Please enter a valid email address.")
        return false
    }
    if (pass.length < 6) {
        onError("Password must be at least 6 characters.")
        return false
    }
    if (pass != confirmPass) {
        onError("Passwords do not match.")
        return false
    }
    return true
}