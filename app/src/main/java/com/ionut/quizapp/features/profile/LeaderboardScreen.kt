package com.ionut.quizapp.features.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.data.LeaderboardEntry
import com.ionut.quizapp.data.UserAvatar
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    quizViewModel: QuizViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    onBack: () -> Unit
) {
    // State-uri logice
    var selectedTab by remember { mutableIntStateOf(0) }
    val modes = listOf("Timed", "Sudden Death")
    var leaderboardData by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val isUtmActive = menuViewModel.isUtmMode

    // State pentru gestul de Swipe
    var offsetX by remember { mutableFloatStateOf(0f) }

    // Sincronizare date la schimbarea tab-ului
    LaunchedEffect(selectedTab) {
        isLoading = true
        leaderboardData = quizViewModel.getLeaderboard(modes[selectedTab], isUtmActive)
        isLoading = false
    }

    Scaffold(
        containerColor = QuizTheme.colors.background,
        topBar = {
            LeaderboardTopBar(onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = 150f
                            if (offsetX > threshold) selectedTab = 0
                            else if (offsetX < -threshold) selectedTab = 1
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        }
                    )
                }
        ) {
            // Selector de Moduri (Pill Tabs)
            LeaderboardTabs(
                modes = modes,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Zona de conținut dinamic
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = QuizTheme.colors.primary
                    )
                } else if (leaderboardData.isEmpty()) {
                    Text(
                        text = "No scores yet for this mode.",
                        modifier = Modifier.align(Alignment.Center),
                        color = QuizTheme.colors.textSecondary
                    )
                } else {
                    val top3 = leaderboardData.take(3)
                    val restOfPlayers = leaderboardData.drop(3)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Secțiunea Podium (Locurile 1, 2, 3)
                        if (top3.isNotEmpty()) {
                            item { PodiumSection(top3 = top3, isUtm = isUtmActive) }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        // Lista extinsă (Locul 4+)
                        itemsIndexed(restOfPlayers) { index, entry ->
                            LeaderboardListTile(rank = index + 4, entry = entry)
                        }
                    }
                }
            }
        }
    }
}

// ========================== COMPONENTE HEADER & NAVIGARE ==========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "GLOBAL RANKINGS",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = QuizTheme.colors.textMain
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = QuizTheme.colors.textMain
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LeaderboardTabs(
    modes: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(QuizTheme.colors.textMain.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
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
                    .clickable { onTabSelected(index) }
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
}

// ========================== COMPONENTE PODIUM (GRAFICA) ==========================

@Composable
fun PodiumSection(top3: List<LeaderboardEntry>, isUtm: Boolean) {
    val gold = if (isUtm) Color(0xFFFFC107) else Color(0xFFFFD700)
    val silver = if (isUtm) Color(0xFF9E9E9E) else Color(0xFFC0C0C0)
    val bronze = if (isUtm) Color(0xFF8D6E63) else Color(0xFFCD7F32)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(300.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Locul 2
        if (top3.size >= 2) {
            PodiumPillar(rank = 2, entry = top3[1], color = silver, height = 110.dp, isUtm = isUtm)
        } else Spacer(modifier = Modifier.width(105.dp))

        // Locul 1
        if (top3.isNotEmpty()) {
            PodiumPillar(rank = 1, entry = top3[0], color = gold, height = 150.dp, isUtm = isUtm)
        }

        // Locul 3
        if (top3.size >= 3) {
            PodiumPillar(rank = 3, entry = top3[2], color = bronze, height = 85.dp, isUtm = isUtm)
        } else Spacer(modifier = Modifier.width(105.dp))
    }
}

@Composable
fun PodiumPillar(rank: Int, entry: LeaderboardEntry, color: Color, height: Dp, isUtm: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(105.dp)
    ) {
        // Zona Avatar + Badge Rank
        Box(modifier = Modifier.size(80.dp)) {
            Image(
                painter = painterResource(id = UserAvatar.fromId(entry.getAvatarId()).drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .border(3.dp, if (isUtm) Color.White else color.copy(alpha = 0.5f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = rank.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info Jucător
        Text(
            text = entry.username,
            fontWeight = FontWeight.ExtraBold,
            color = QuizTheme.colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )

        // Badge Scor (Contrast sporit pentru UTM)
        Surface(
            color = if (isUtm) Color.White.copy(alpha = 0.6f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "${entry.score} pts",
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pilonul vizual al podiumului
        Box(
            modifier = Modifier
                .width(85.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (isUtm) color.copy(alpha = 0.35f) else color.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = if (isUtm) color.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = rank.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                color = if (isUtm) color.copy(alpha = 0.6f) else color.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ========================== COMPONENTE LISTĂ (TILES) ==========================

@Composable
fun LeaderboardListTile(rank: Int, entry: LeaderboardEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QuizTheme.colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                fontWeight = FontWeight.Bold,
                color = QuizTheme.colors.textSecondary,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = UserAvatar.fromId(entry.getAvatarId()).drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(QuizTheme.colors.textMain.copy(alpha = 0.05f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = entry.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QuizTheme.colors.textMain,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${entry.score} pts",
                fontWeight = FontWeight.Black,
                color = QuizTheme.colors.primary,
                fontSize = 14.sp
            )
        }
    }
}