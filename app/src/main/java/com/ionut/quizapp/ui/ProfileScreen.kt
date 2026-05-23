package com.ionut.quizapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutSuccess: () -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel()
) {
    val isUtmActive = menuViewModel.isUtmMode
    val isGuest = authViewModel.isCurrentUserGuest

    var timedBest by remember { mutableIntStateOf(0) }
    var suddenBest by remember { mutableIntStateOf(0) }

    // Tot ce ține de rețea/suspend trebuie să stea în LaunchedEffect
    LaunchedEffect(isUtmActive) {
        authViewModel.fetchProfile()

        if (!isGuest) {
            val userId = authViewModel.currentUserId
            if (userId != null) {

                timedBest = quizViewModel.getPersonalBestScore(userId, "Timed", isUtmActive)
                suddenBest = quizViewModel.getPersonalBestScore(userId, "Sudden Death", isUtmActive)
            }
        }
    }

    Scaffold(
        containerColor = QuizTheme.colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MY PROFILE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QuizTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = QuizTheme.colors.textMain
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Circular
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(QuizTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = QuizTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Username Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Username",
                            style = MaterialTheme.typography.labelMedium,
                            color = QuizTheme.colors.textSecondary
                        )
                        Text(
                            text = authViewModel.currentUserProfile ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = QuizTheme.colors.textMain
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = if (isGuest) Color.Gray.copy(alpha = 0.1f) else QuizTheme.colors.accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isGuest) "GUEST" else "REGISTERED",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isGuest) Color.DarkGray else QuizTheme.colors.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secțiunea de Statistici (Doar pentru utilizatori înregistrați)
            if (!isGuest) {
                Text(
                    text = "PERSONAL RECORDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = QuizTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PersonalScoreCard(
                        label = "Timed Mode",
                        score = timedBest,
                        color = QuizTheme.colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    PersonalScoreCard(
                        label = "Sudden Death",
                        score = suddenBest,
                        color = QuizTheme.colors.primary, // Roșu pentru Sudden Death
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Mesaj pentru Guest
                Card(
                    colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.accent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = QuizTheme.colors.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Register to save your scores in the global leaderboard!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = QuizTheme.colors.textMain
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buton Logout
            Button(
                onClick = { authViewModel.logout { onLogoutSuccess() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUtmActive) QuizTheme.colors.primary else QuizTheme.colors.secondary
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
            Spacer(modifier = Modifier.height(16.dp))
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
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = QuizTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}