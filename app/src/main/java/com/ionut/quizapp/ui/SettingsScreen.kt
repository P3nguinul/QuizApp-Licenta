package com.ionut.quizapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.SoundManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    soundManager: SoundManager,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Stări Audio & Feedback
    var isMusicOn by remember { mutableStateOf(soundManager.isMusicEnabled) }
    var isSfxOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var isVibrationOn by remember { mutableStateOf(soundManager.isVibrationEnabled) }

    // Stări Dialoguri Cont
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Input & Erori Username
    var newUsernameInput by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // Input & Erori Parolă
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = QuizTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ==========================================
            // SECȚIUNEA 1: PREFERINȚE AUDIO & FEEDBACK
            // ==========================================
            item {
                SettingsSectionTitle("AUDIO & EXPERIENCE")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        SettingsSwitchRow(
                            icon = Icons.Default.MusicNote,
                            title = "Background Music",
                            isChecked = isMusicOn,
                            onCheckedChange = {
                                isMusicOn = it
                                soundManager.isMusicEnabled = it
                                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (it) soundManager.playBGM() else soundManager.pauseBGM()
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "Sound Effects",
                            isChecked = isSfxOn,
                            onCheckedChange = {
                                isSfxOn = it
                                soundManager.isSoundEnabled = it
                                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            icon = Icons.Default.Vibration,
                            title = "Vibrations",
                            isChecked = isVibrationOn,
                            onCheckedChange = {
                                isVibrationOn = it
                                soundManager.isVibrationEnabled = it
                                if (it) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }

            // ==========================================
            // SECȚIUNEA 2: MANAGEMENTUL CONTULUI
            // ==========================================
            item {
                SettingsSectionTitle("ACCOUNT MANAGEMENT")

                if (authViewModel.isCurrentUserGuest) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "You are currently playing as a Guest. Create an account from the Login screen to customize your profile and save your progress.",
                            color = Color(0xFFE65100),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            SettingsClickableRow(
                                icon = Icons.Default.Person,
                                title = "Change Username",
                                onClick = {
                                    usernameError = null
                                    newUsernameInput = ""
                                    showUsernameDialog = true
                                }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                            SettingsClickableRow(
                                icon = Icons.Default.Lock,
                                title = "Change Password",
                                onClick = {
                                    passwordError = null
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    showPasswordDialog = true
                                }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                            SettingsClickableRow(
                                icon = Icons.Default.DeleteForever,
                                title = "Delete Account",
                                isDestructive = true,
                                onClick = { showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ==========================================
        // DIALOG: CHANGE USERNAME
        // ==========================================
        if (showUsernameDialog) {
            AlertDialog(
                onDismissRequest = { if (!isLoading) showUsernameDialog = false },
                title = { Text("Change Username", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter your new username below. This will be visible on the Leaderboards.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newUsernameInput,
                            onValueChange = {
                                newUsernameInput = it
                                usernameError = null // Ascundem eroarea când utilizatorul începe să scrie
                            },
                            label = { Text("New Username") },
                            singleLine = true,
                            isError = usernameError != null,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        // TEXT EROARE INLINE
                        if (usernameError != null) {
                            Text(
                                text = usernameError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanInput = newUsernameInput.trim() // Eliminăm spațiile moarte
                            if (cleanInput.length < 3) {
                                usernameError = "Username must be at least 3 characters."
                            } else {
                                isLoading = true
                                authViewModel.updateUsername(cleanInput) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        showUsernameDialog = false
                                        // Afișăm succesul DOAR DUPĂ ce se închide fereastra
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    } else {
                                        usernameError = message // Eroare de la server (ex: lipsă net)
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("SAVE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUsernameDialog = false }, enabled = !isLoading) {
                        Text("CANCEL")
                    }
                },
                containerColor = QuizTheme.colors.surface
            )
        }

        // ==========================================
        // DIALOG: CHANGE PASSWORD
        // ==========================================
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { if (!isLoading) showPasswordDialog = false },
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Your new password must be at least 6 characters long.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = {
                                newPasswordInput = it
                                passwordError = null
                            },
                            label = { Text("New Password") },
                            singleLine = true,
                            isError = passwordError != null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = {
                                confirmPasswordInput = it
                                passwordError = null
                            },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            isError = passwordError != null,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )

                        // TEXT EROARE INLINE
                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPasswordInput.length < 6) {
                                passwordError = "Password must be at least 6 characters."
                            } else if (newPasswordInput != confirmPasswordInput) {
                                passwordError = "Passwords do not match."
                            } else {
                                isLoading = true
                                authViewModel.updatePassword(newPasswordInput) { success, message ->
                                    isLoading = false
                                    if (success) {
                                        showPasswordDialog = false
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    } else {
                                        passwordError = message
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("SAVE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordDialog = false }, enabled = !isLoading) {
                        Text("CANCEL")
                    }
                },
                containerColor = QuizTheme.colors.surface
            )
        }

        // ==========================================
        // DIALOG: DELETE ACCOUNT
        // ==========================================
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isLoading) showDeleteDialog = false },
                title = { Text("Delete Account", fontWeight = FontWeight.Black, color = Color(0xFFE53935)) },
                text = {
                    Text(
                        "Are you absolutely sure you want to delete your account? This action is permanent. All your scores, progress, and saved AI quizzes will be lost forever.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isLoading = true
                            authViewModel.deleteAccount { success, message ->
                                isLoading = false
                                if (success) {
                                    if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showDeleteDialog = false
                                    onNavigateToLogin()
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("DELETE FOREVER", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }, enabled = !isLoading) {
                        Text("CANCEL")
                    }
                },
                containerColor = QuizTheme.colors.surface
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = QuizTheme.colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = QuizTheme.colors.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold, color = QuizTheme.colors.textMain, modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = QuizTheme.colors.primary)
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDestructive) Color(0xFFE53935) else QuizTheme.colors.textMain
    val iconColor = if (isDestructive) Color(0xFFE53935) else QuizTheme.colors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontWeight = FontWeight.SemiBold, color = contentColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}