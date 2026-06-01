package com.ionut.quizapp.features.profile

import android.util.Patterns
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.features.core.theme.QuizTheme
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
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State-uri Audio & Feedback
    var isMusicOn by remember { mutableStateOf(soundManager.isMusicEnabled) }
    var isSfxOn by remember { mutableStateOf(soundManager.isSoundEnabled) }
    var isVibrationOn by remember { mutableStateOf(soundManager.isVibrationEnabled) }

    // State-uri Dialoguri & Control
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // State-uri Input Username
    var newUsernameInput by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }

    // State-uri Input Parolă
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Partea 1: Preferințe Audio
            item {
                SettingsSectionTitle("AUDIO & EXPERIENCE")
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        SettingsSwitchRow(
                            icon = Icons.Default.MusicNote,
                            title = "Background Music",
                            isChecked = isMusicOn,
                            onCheckedChange = {
                                isMusicOn = it
                                soundManager.isMusicEnabled = it
                                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }

            // Partea 2: Administrare Cont
            item {
                SettingsSectionTitle("ACCOUNT MANAGEMENT")
                if (authViewModel.isCurrentUserGuest) {
                    GuestSettingsPlaceholder()
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column {
                            SettingsClickableRow(
                                icon = Icons.Default.Person,
                                title = "Change Username",
                                onClick = { usernameError = null; newUsernameInput = ""; showUsernameDialog = true }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            SettingsClickableRow(
                                icon = Icons.Default.Lock,
                                title = "Change Password",
                                onClick = { passwordError = null; newPasswordInput = ""; confirmPasswordInput = ""; showPasswordDialog = true }
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
    }

    // --- MODALURI PENTRU ACȚIUNI ---

    if (showUsernameDialog) {
        UpdateUsernameDialog(
            currentInput = newUsernameInput,
            error = usernameError,
            isLoading = isLoading,
            onValueChange = { newUsernameInput = it; usernameError = null },
            onDismiss = { showUsernameDialog = false },
            onSave = {
                val cleanInput = newUsernameInput.trim()
                if (cleanInput.length < 3) {
                    usernameError = "Username must be at least 3 characters."
                } else {
                    isLoading = true
                    authViewModel.updateUsername(cleanInput) { success, message ->
                        isLoading = false
                        if (success) {
                            showUsernameDialog = false
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        } else { usernameError = message }
                    }
                }
            }
        )
    }

    if (showPasswordDialog) {
        UpdatePasswordDialog(
            newPass = newPasswordInput,
            confirmPass = confirmPasswordInput,
            error = passwordError,
            isLoading = isLoading,
            isVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
            onNewPassChange = { newPasswordInput = it; passwordError = null },
            onConfirmPassChange = { confirmPasswordInput = it; passwordError = null },
            onDismiss = { showPasswordDialog = false },
            onSave = {
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
                        } else { passwordError = message }
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            isLoading = isLoading,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                isLoading = true
                authViewModel.deleteAccount { success, message ->
                    isLoading = false
                    if (success) {
                        if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = false
                        onNavigateToLogin()
                    } else { scope.launch { snackbarHostState.showSnackbar(message) } }
                }
            }
        )
    }
}

// ========================== COMPONENTE INTERNE & DIALOGURI ==========================

@Composable
private fun GuestSettingsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Text(
            text = "You are currently playing as a Guest. Create an account to customize your profile.",
            color = Color(0xFFE65100),
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UpdateUsernameDialog(
    currentInput: String,
    error: String?,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Username", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter your new username. It will be visible on Leaderboards.", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = onValueChange,
                    label = { Text("New Username") },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                if (error != null) Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White) else Text("SAVE")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("CANCEL") } }
    )
}

@Composable
private fun UpdatePasswordDialog(
    newPass: String,
    confirmPass: String,
    error: String?,
    isLoading: Boolean,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onNewPassChange: (String) -> Unit,
    onConfirmPassChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                val visualTrans = if (isVisible) VisualTransformation.None else PasswordVisualTransformation()
                OutlinedTextField(
                    value = newPass,
                    onValueChange = onNewPassChange,
                    label = { Text("New Password") },
                    visualTransformation = visualTrans,
                    trailingIcon = { IconButton(onClick = onToggleVisibility) { Icon(if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = onConfirmPassChange,
                    label = { Text("Confirm New Password") },
                    visualTransformation = visualTrans,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                if (error != null) Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White) else Text("SAVE")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("CANCEL") } }
    )
}

@Composable
private fun DeleteAccountDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account", fontWeight = FontWeight.Black, color = Color(0xFFE53935)) },
        text = { Text("Are you absolutely sure? This action is permanent. All progress and AI quizzes will be lost forever.", fontSize = 14.sp, color = Color.Gray) },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), Color.White) else Text("DELETE FOREVER", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("CANCEL") } }
    )
}

// ========================== COMPONENTE DE RÂND (LIST ITEMS) ==========================

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
fun SettingsSwitchRow(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = QuizTheme.colors.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = QuizTheme.colors.textMain, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = QuizTheme.colors.primary))
    }
}

@Composable
fun SettingsClickableRow(icon: ImageVector, title: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val color = if (isDestructive) Color(0xFFE53935) else QuizTheme.colors.textMain
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (isDestructive) color else QuizTheme.colors.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}