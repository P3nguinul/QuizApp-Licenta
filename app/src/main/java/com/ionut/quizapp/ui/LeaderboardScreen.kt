package com.ionut.quizapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.data.LeaderboardEntry
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel
// ... importurile tale + cele de mai sus ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    quizViewModel: QuizViewModel= viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    onBack: () -> Unit) {
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
        containerColor = QuizTheme.colors.background.copy(alpha = 0.95f),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GLOBAL RANKINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                modes.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (isLoading) {
                // Poți pune un LinearProgressIndicator aici
            } else if (leaderboardData.isEmpty()) {
                Text(
                    "No scores yet for this mode.",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                itemsIndexed(leaderboardData) { index, entry ->
                    LeaderboardItem(index + 1, entry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, entry: LeaderboardEntry) {
    // Definim culorile pentru Top 3
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold vibrant
        2 -> Color(0xFFB4B4B4) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> QuizTheme.colors.textSecondary.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        // Folosim culori cu contrast mare
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Fundal alb pentru contrast maxim
        ),
        // Adăugăm umbră și un border discret
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = if (rank <= 3) 2.dp else 0.dp,
            color = rankColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cercul cu Rank-ul
            Surface(
                shape = CircleShape,
                color = rankColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = rank.toString(),
                        fontWeight = FontWeight.Black,
                        color = if (rank <= 3) rankColor else QuizTheme.colors.textMain,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Numele Utilizatorului
            Text(
                text = entry.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QuizTheme.colors.textMain,
                modifier = Modifier.weight(1f)
            )

            // Scorul (Capsula de scor)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = QuizTheme.colors.primary.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, QuizTheme.colors.primary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "${entry.score} pts",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.primary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
