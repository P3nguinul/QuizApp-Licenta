package com.ionut.quizapp.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.data.CustomQuiz
import com.ionut.quizapp.data.UserAvatar
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutSuccess: () -> Unit,
    onBack: () -> Unit,
    onNavigateToGame: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel()
) {
    val colors = QuizTheme.colors
    val isUtmActive = menuViewModel.isUtmMode
    val isGuest = authViewModel.isCurrentUserGuest
    val userId = authViewModel.currentUserId

    // Scoruri și Dialoguri State
    var timedBest by remember { mutableIntStateOf(0) }
    var suddenBest by remember { mutableIntStateOf(0) }
    var quizToDelete by remember { mutableStateOf<CustomQuiz?>(null) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    // Sincronizare date profil
    LaunchedEffect(isUtmActive) {
        authViewModel.fetchProfile()
        if (!isGuest && userId != null) {
            timedBest = quizViewModel.getPersonalBestScore(userId, "Timed", isUtmActive)
            suddenBest = quizViewModel.getPersonalBestScore(userId, "Sudden Death", isUtmActive)
            quizViewModel.loadUserCustomQuizzes(userId)
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            ProfileTopBar(onBack = onBack)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Zona de conținut scrollabilă
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Partea 1: Avatar utilizator
                item {
                    UserAvatarSection(
                        avatarId = authViewModel.currentUserAvatarId,
                        isGuest = isGuest,
                        onAvatarClick = { showAvatarDialog = true }
                    )
                }

                // Partea 2: Card cu informații cont
                item {
                    UsernameCard(
                        username = authViewModel.currentUserProfile ?: "Loading...",
                        isGuest = isGuest
                    )
                }

                // Partea 3: Recorduri personale (doar pentru utilizatori înregistrați)
                item {
                    PersonalRecordsSection(
                        isGuest = isGuest,
                        timedBest = timedBest,
                        suddenBest = suddenBest
                    )
                }

                // Partea 4: Librăria de teste AI
                if (!isGuest) {
                    item {
                        Text(
                            text = "MY AI QUIZ LIBRARY",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (quizViewModel.userCustomQuizzes.isEmpty()) {
                        item { EmptyLibraryPlaceholder() }
                    } else {
                        items(quizViewModel.userCustomQuizzes, key = { it.id }) { quiz ->
                            LibraryQuizItem(
                                quiz = quiz,
                                onRedo = {
                                    quizViewModel.startCustomQuizMode(quiz.id, isReplay = true)
                                    onNavigateToGame()
                                },
                                onDelete = { quizToDelete = quiz }
                            )
                        }
                    }
                }
            }

            // Buton Logout Sticky
            LogoutButtonSection(
                isGuest = isGuest,
                isUtmActive = isUtmActive,
                onLogout = { authViewModel.logout { onLogoutSuccess() } }
            )
        }
    }

    // --- MODALURI ȘI DIALOGURI ---
    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarId = authViewModel.currentUserAvatarId,
            onAvatarSelected = { selectedId ->
                authViewModel.updateAvatar(selectedId)
                showAvatarDialog = false
            },
            onDismiss = { showAvatarDialog = false }
        )
    }

    if (quizToDelete != null) {
        DeleteConfirmationDialog(
            quizTitle = quizToDelete?.title ?: "",
            onConfirm = {
                quizToDelete?.let { quizViewModel.deleteCustomQuizFromLibrary(it.id) }
                quizToDelete = null
            },
            onDismiss = { quizToDelete = null }
        )
    }
}

// ========================== COMPONENTE HEADER & PROFILE ==========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    val colors = QuizTheme.colors
    CenterAlignedTopAppBar(
        title = { Text("MY PROFILE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.primary)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = colors.textMain
        )
    )
}

@Composable
private fun UserAvatarSection(avatarId: Int, isGuest: Boolean, onAvatarClick: () -> Unit) {
    val colors = QuizTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.1f))
                .clickable(enabled = !isGuest) { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = UserAvatar.fromId(avatarId).drawableRes),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp).clip(CircleShape)
            )
        }
        if (!isGuest) {
            Text(
                text = "Tap to change avatar",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun UsernameCard(username: String, isGuest: Boolean) {
    val colors = QuizTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Username", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textMain)
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = if (isGuest) Color.Gray.copy(0.1f) else colors.accent.copy(0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isGuest) "GUEST" else "REGISTERED",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGuest) Color.DarkGray else colors.secondary
                )
            }
        }
    }
}

// ========================== COMPONENTE RECORDURI & LIBRĂRIE ==========================

@Composable
private fun PersonalRecordsSection(isGuest: Boolean, timedBest: Int, suddenBest: Int) {
    val colors = QuizTheme.colors
    if (!isGuest) {
        Column {
            Text(
                text = "PERSONAL RECORDS",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PersonalScoreCard("Timed Mode", timedBest, colors.primary, Modifier.weight(1f))
                PersonalScoreCard("Sudden Death", suddenBest, colors.primary, Modifier.weight(1f))
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.accent.copy(0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = colors.secondary)
                Spacer(Modifier.width(12.dp))
                Text("Register to save your scores in the global leaderboard!", color = colors.textMain)
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder() {
    val colors = QuizTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, null, tint = colors.textSecondary.copy(0.4f), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your library is empty.\nUpload a PDF in the generator to start!",
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LibraryQuizItem(quiz: CustomQuiz, onRedo: () -> Unit, onDelete: () -> Unit) {
    val colors = QuizTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFF6200EE).copy(0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, fontWeight = FontWeight.Bold, color = colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("AI Generated Material", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            IconButton(onClick = onRedo) { Icon(Icons.Default.PlayArrow, null, tint = colors.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFE53935)) }
        }
    }
}

@Composable
private fun LogoutButtonSection(isGuest: Boolean, isUtmActive: Boolean, onLogout: () -> Unit) {
    val colors = QuizTheme.colors
    Box(modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isUtmActive) colors.primary else colors.secondary)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isGuest) "Exit Guest Session" else "Logout", fontWeight = FontWeight.Bold)
        }
    }
}

// ========================== DIALOGURI ȘI UTILS ==========================

@Composable
fun AvatarSelectionDialog(currentAvatarId: Int, onAvatarSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(QuizTheme.colors.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Choose Your Avatar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(UserAvatar.entries) { avatar ->
                        val isSelected = avatar.id == currentAvatarId
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                .background(if (isSelected) QuizTheme.colors.primary.copy(0.2f) else QuizTheme.colors.background)
                                .border(if (isSelected) 3.dp else 0.dp, QuizTheme.colors.primary, CircleShape)
                                .clickable { onAvatarSelected(avatar.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(avatar.drawableRes), null, modifier = Modifier.size(56.dp).clip(CircleShape))
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = QuizTheme.colors.textSecondary) }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(quizTitle: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Delete Custom Quiz", fontWeight = FontWeight.Black) },
        text = { Text("Are you sure you want to delete '$quizTitle'? This action cannot be undone.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(Color(0xFFE53935))) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = QuizTheme.colors.textSecondary) } }
    )
}

@Composable
fun PersonalScoreCard(label: String, score: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = QuizTheme.colors.textSecondary)
            Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
        }
    }
}