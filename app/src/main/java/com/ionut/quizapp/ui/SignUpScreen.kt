package com.ionut.quizapp.ui

import android.os.Build
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
import com.ionut.quizapp.ui.theme.QuizAppTheme
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.auth.AuthViewModel

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    QuizAppTheme(isUtmMode = false) {
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") } // <--- NOU: Stare pentru a doua parolă

        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val focusManager = LocalFocusManager.current

        @RequiresApi(Build.VERSION_CODES.FROYO)
        fun validateInputs(): Boolean {
            if (username.trim().length < 3) {
                errorMessage = "Username must be at least 3 characters."
                return false
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                errorMessage = "Please enter a valid email address."
                return false
            }
            if (password.length < 6) {
                errorMessage = "Password must be at least 6 characters."
                return false
            }
            if (password != confirmPassword) { // <--- NOU: Validare potrivire parole
                errorMessage = "Passwords do not match."
                return false
            }
            return true
        }

        val performSignUp = {
            if (validateInputs()) {
                isLoading = true
                errorMessage = null
                viewModel.signUp(email.trim(), password, username.trim()) { success ->
                    isLoading = false
                    if (success) {
                        onSignUpSuccess()
                    } else {
                        errorMessage = "Registration failed! Email might be already taken."
                    }
                }
            }
        }

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
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuizTheme.colors.primary,
                        focusedLabelColor = QuizTheme.colors.primary,
                        cursorColor = QuizTheme.colors.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuizTheme.colors.primary,
                        focusedLabelColor = QuizTheme.colors.primary,
                        cursorColor = QuizTheme.colors.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuizTheme.colors.primary,
                        focusedLabelColor = QuizTheme.colors.primary,
                        cursorColor = QuizTheme.colors.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), // Schimbat în Next
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }) // Coboară la confirmare
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Confirm Password Field (NOU)
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuizTheme.colors.primary,
                        focusedLabelColor = QuizTheme.colors.primary,
                        cursorColor = QuizTheme.colors.primary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), // Acesta e ultimul field
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        performSignUp()
                    })
                )

                // Zonă text pentru afișarea erorilor
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = QuizTheme.colors.primary)
                } else {
                    Button(
                        onClick = { performSignUp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuizTheme.colors.primary
                        )
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