package com.ionut.quizapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.data.LeaderboardEntry
import com.ionut.quizapp.data.UserAvatar
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    quizViewModel: QuizViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val modes = listOf("Timed", "Sudden Death")
    var leaderboardData by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val isUtmActive = menuViewModel.isUtmMode

    LaunchedEffect(selectedTab) {
        isLoading = true
        leaderboardData = quizViewModel.getLeaderboard(modes[selectedTab], isUtmActive)
        isLoading = false
    }

    Scaffold(
        containerColor = QuizTheme.colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GLOBAL RANKINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QuizTheme.colors.textMain)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // TABS Customizate (Stil Pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                modes.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) QuizTheme.colors.primary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else QuizTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QuizTheme.colors.primary)
                }
            } else if (leaderboardData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No scores yet for this mode.", color = QuizTheme.colors.textSecondary)
                }
            } else {
                val top3 = leaderboardData.take(3)
                val restOfPlayers = leaderboardData.drop(3)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // SECȚIUNEA 1: PODIUMUL (Doar dacă avem măcar 1 jucător)
                    if (top3.isNotEmpty()) {
                        item {
                            PodiumSection(top3 = top3)
                        }
                    }

                    // SECȚIUNEA 2: LISTA DE JOS (De la locul 4 încolo)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    itemsIndexed(restOfPlayers) { index, entry ->
                        LeaderboardListTile(
                            rank = index + 4, // Începem de la 4
                            entry = entry
                        )
                    }
                }
            }
        }
    }
}

// ====================================================================
// COMPONENTA 1: PODIUMUL (Designul ca în poză)
// ====================================================================
@Composable
fun PodiumSection(top3: List<LeaderboardEntry>) {
    // Înălțimile cutiilor (Rank 1 e cel mai înalt, apoi Rank 2, Rank 3)
    val rank1Height = 160.dp
    val rank2Height = 120.dp
    val rank3Height = 90.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(280.dp), // Suficient spațiu pentru cutii + avatare
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Locul 2 (Stânga)
        if (top3.size >= 2) {
            PodiumPillar(
                rank = 2,
                entry = top3[1],
                color = Color(0xFFC0C0C0), // Argint
                height = rank2Height
            )
        } else {
            Spacer(modifier = Modifier.width(90.dp))
        }

        // Locul 1 (Centru)
        if (top3.isNotEmpty()) {
            PodiumPillar(
                rank = 1,
                entry = top3[0],
                color = Color(0xFFFFD700), // Aur
                height = rank1Height
            )
        }

        // Locul 3 (Dreapta)
        if (top3.size >= 3) {
            PodiumPillar(
                rank = 3,
                entry = top3[2],
                color = Color(0xFFCD7F32), // Bronz
                height = rank3Height
            )
        } else {
            Spacer(modifier = Modifier.width(90.dp))
        }
    }
}

@Composable
fun PodiumPillar(rank: Int, entry: LeaderboardEntry, color: Color, height: androidx.compose.ui.unit.Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(100.dp)
    ) {
        // 1. Avatarul Jucătorului
        Box(
            modifier = Modifier.size(76.dp) // Părintele e un pic mai mare, netăiat
        ) {
            // Poza avatarului cu contur alb
            Image(
                painter = painterResource(id = UserAvatar.fromId(entry.getAvatarId()).drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape) // Conturul alb pus elegant
            )

            // Cercul cu rank-ul (pus peste poză, nefiind tăiat de părinte)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp) // Îl tragem un pic mai pe centru
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White, CircleShape), // Îi punem și lui un mic contur alb!
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Numele și Scorul
        Text(
            text = entry.username,
            fontWeight = FontWeight.Bold,
            color = QuizTheme.colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )
        Text(
            text = "${entry.score} pts",
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Stâlpul (Cutia podiumului)
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color.copy(alpha = 0.2f)) // Culoare pastelată pe bază de medalie
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = rank.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 48.sp,
                color = color.copy(alpha = 0.5f)
            )
        }
    }
}

// ====================================================================
// COMPONENTA 2: LISTA DE JOS (De la Rank 4 încolo)
// ====================================================================
@Composable
fun LeaderboardListTile(rank: Int, entry: LeaderboardEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Text(
                text = rank.toString(),
                fontWeight = FontWeight.Bold,
                color = QuizTheme.colors.textSecondary,
                modifier = Modifier.width(24.dp)
            )

            // Avatar
            Image(
                painter = painterResource(id = UserAvatar.fromId(entry.getAvatarId()).drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.1f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Nume
            Text(
                text = entry.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QuizTheme.colors.textMain,
                modifier = Modifier.weight(1f)
            )

            // Scor
            Text(
                text = "${entry.score} pts",
                fontWeight = FontWeight.Black,
                color = QuizTheme.colors.primary,
                fontSize = 14.sp
            )
        }
    }
}