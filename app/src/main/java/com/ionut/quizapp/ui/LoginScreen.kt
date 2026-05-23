package com.ionut.quizapp.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.ui.theme.QuizAppTheme
import com.ionut.quizapp.ui.theme.QuizTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    // Forțăm tema normală (Roz/Galben)
    QuizAppTheme(isUtmMode = false) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val focusManager = LocalFocusManager.current

        fun validateInputs(): Boolean {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                errorMessage = "Please enter a valid email address."
                return false
            }
            if (password.length < 6) {
                errorMessage = "Password must be at least 6 characters."
                return false
            }
            return true
        }

        val performLogin = {
            if (validateInputs()) {
                isLoading = true
                errorMessage = null
                viewModel.loginAsUser(email, password) { success ->
                    isLoading = false
                    if (success) onLoginSuccess() else errorMessage = "Login failed! Check your credentials."
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
                // Titlu cu font mare și culoarea Roz (Primary)
                Text(
                    text = "QuizAdventure",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Field
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Field
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            performLogin()
                        }
                    )
                )

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
                    // Buton Login principal (Roz)
                    Button(
                        onClick = { performLogin() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuizTheme.colors.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onSignUpClick
                    ) {
                        Text(
                            text = "Don't have an account? Sign Up",
                            color = QuizTheme.colors.secondary, // Un roz mai închis
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )

                    // Buton Play as Guest (Contur Galben/Accent)
                    OutlinedButton(
                        onClick = { viewModel.loginAsGuest { onGuestLogin() } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, QuizTheme.colors.accent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = QuizTheme.colors.textMain
                        )
                    ) {
                        Text("Play as Guest", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}