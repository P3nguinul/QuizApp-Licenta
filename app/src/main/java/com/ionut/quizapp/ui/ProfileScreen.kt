package com.ionut.quizapp.ui

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.data.CustomQuiz
import com.ionut.quizapp.data.UserAvatar
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

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

    var timedBest by remember { mutableIntStateOf(0) }
    var suddenBest by remember { mutableIntStateOf(0) }

    // STATE PENTRU MODALUL DE STERGERE
    var quizToDelete by remember { mutableStateOf<CustomQuiz?>(null) }
    var showAvatarDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(isUtmActive) {
        authViewModel.fetchProfile()

        kotlinx.coroutines.Dispatchers.IO.run {
            UserAvatar.entries.forEach { avatar ->
                // Această linie doar accesează referința drawable-ului în fundal
                val triggerRead = avatar.drawableRes
            }
        }

        if (!isGuest && userId != null) {
            timedBest = quizViewModel.getPersonalBestScore(userId, "Timed", isUtmActive)
            suddenBest = quizViewModel.getPersonalBestScore(userId, "Sudden Death", isUtmActive)
            quizViewModel.loadUserCustomQuizzes(userId)
        }
    }

    // DIALOG DE CONFIRMARE STERGERE (MODAL)
    if (quizToDelete != null) {
        AlertDialog(
            onDismissRequest = { quizToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Delete Custom Quiz",
                    fontWeight = FontWeight.Black,
                    color = colors.textMain
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${quizToDelete?.title}'? This action cannot be undone.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        quizToDelete?.let { quizViewModel.deleteCustomQuizFromLibrary(it.id) }
                        quizToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { quizToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MY PROFILE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colors.textMain
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // LISTA SCROLLABILĂ - I-AM DAT WEIGHT(1f) CA SĂ OCUPE DOAR SPAȚIUL DISPONIBIL RĂMAS
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Secțiunea 1: Avatar
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.1f))
                            .clickable(enabled = !isGuest) { showAvatarDialog = true }, // <--- AM ADĂUGAT ASTA AICI
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = UserAvatar.fromId(authViewModel.currentUserAvatarId).drawableRes),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                    }
                    // Opțional, un mic text ajutător
                    if (!isGuest) {
                        Text(
                            text = "Tap to change avatar",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Secțiunea 2: Username Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Username", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                                Text(
                                    text = authViewModel.currentUserProfile ?: "Loading...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = if (isGuest) Color.Gray.copy(alpha = 0.1f) else colors.accent.copy(alpha = 0.2f),
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

                // Secțiunea 3: Recorduri / Banner
                item {
                    if (!isGuest) {
                        Column {
                            Text(
                                text = "PERSONAL RECORDS",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                PersonalScoreCard("Timed Mode", timedBest, colors.primary, Modifier.weight(1f))
                                PersonalScoreCard("Sudden Death", suddenBest, colors.primary, Modifier.weight(1f))
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.accent.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = colors.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Register to save your scores in the global leaderboard!", style = MaterialTheme.typography.bodyMedium, color = colors.textMain)
                            }
                        }
                    }
                }

                // Secțiunea 4: Librăria de AI
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
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = colors.surface.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.1f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your library is empty.\nUpload a PDF in the generator to start!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(quizViewModel.userCustomQuizzes, key = { it.id }) { quiz ->
                            LibraryQuizItem(
                                quiz = quiz,
                                onRedo = {
                                    // BINGO! Aici îi spunem că suntem pe REPLAY!
                                    quizViewModel.startCustomQuizMode(quiz.id, isReplay = true)
                                    onNavigateToGame()
                                },
                                onDelete = {
                                    // Aici doar setăm quiz-ul care declanșează modalul
                                    quizToDelete = quiz
                                }
                            )
                        }
                    }
                }
            } // <- Sfârșit LazyColumn

            // BUTON STICKY LOGOUT - ÎN AFARA LAZYCOLUMN-ULUI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { authViewModel.logout { onLogoutSuccess() } },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUtmActive) colors.primary else colors.secondary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGuest) "Exit Guest Session" else "Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// COMPONENTE EXTRA
@Composable
fun LibraryQuizItem(
    quiz: CustomQuiz,
    onRedo: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = QuizTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(shape = CircleShape, color = Color(0xFF6200EE).copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp)) }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("AI Generated Material", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRedo) { Icon(Icons.Default.PlayArrow, contentDescription = "Start Quiz", tint = colors.primary, modifier = Modifier.size(26.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Quiz", tint = Color(0xFFE53935), modifier = Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
fun PersonalScoreCard(label: String, score: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = QuizTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(score.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun AvatarSelectionDialog(
    currentAvatarId: Int,
    onAvatarSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Your Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.textMain
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Grila cu cele 16 avatare
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4), // 4 pe un rând
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(300.dp) // Setăm o înălțime fixă pentru scroll
                ) {
                    items(UserAvatar.entries) { avatar ->
                        val isSelected = avatar.id == currentAvatarId

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) QuizTheme.colors.primary.copy(alpha = 0.2f)
                                    else QuizTheme.colors.background
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) QuizTheme.colors.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAvatarSelected(avatar.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatar.drawableRes),
                                contentDescription = "Avatar ${avatar.id}",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = QuizTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}