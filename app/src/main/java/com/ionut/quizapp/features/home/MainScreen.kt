package com.ionut.quizapp.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.data.GameMode
import com.ionut.quizapp.data.UserAvatar
import com.ionut.quizapp.data.gameModes
import com.ionut.quizapp.features.profile.LoginRequiredDialog
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager


@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToCustomQuiz: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onStartQuiz: (mode: String, categories: List<String>, isUtm: Boolean, count: Int) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    soundManager: SoundManager
) {
    val isUtm = menuViewModel.isUtmMode
    val haptic = LocalHapticFeedback.current

    // State-uri pentru controlul Dialogurilor
    var showModeDialog by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<GameMode?>(null) }
    var showGuestPremiumDialog by remember { mutableStateOf(false) }
    var showUtmInfoDialog by remember { mutableStateOf(false) }

    val playfulColors = listOf(
        QuizTheme.colors.primary,
        QuizTheme.colors.secondary,
        Color(0xFFFF9800),
        Color(0xFF4CAF50)
    )

    val categoriesList = if (isUtm) {
        listOf("All UTM", "Tehnici Avansate de Programare", "Inovare și Transformare Digitală", "Comerț Electronic", "Criptografie", "Administrarea Rețelelor de Calculatoare", "Metode Avansate De Programare (Java)", "Sisteme de Gestiune a Bazelor de Date", "Programare Orientată pe Obiecte (C++)", "Tehnologii Web", "Sisteme de Operare", "Fundamentele Programării", "Programare în Python", "Algoritmi și Structuri de Date", "Cloud Computing", "Baze de Date")
    } else {
        listOf("All Categories", "General Knowledge", "Films", "Music", "Television", "Video Games", "Science and Nature", "Computer Science", "Mathematics", "Sports", "Geography", "History", "Animals", "Vehicles", "Japanese Anime & Manga", "Comics")
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchProfile()
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {

                Spacer(modifier = Modifier.height(24.dp))

                // Header secțiune profil și acțiuni rapide
                HomeHeader(
                    username = authViewModel.currentUserProfile ?: "Explorer",
                    avatarId = authViewModel.currentUserAvatarId,
                    onProfileClick = onNavigateToProfile,
                    onLeaderboardClick = onNavigateToLeaderboard,
                    onSettingsClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "CHOOSE GAME MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = QuizTheme.colors.textSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )

                // Grila principală cu modurile de joc
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(gameModes) { index, mode ->
                        val itemColor = if (isUtm) QuizTheme.colors.primary else playfulColors[index % playfulColors.size]

                        GameModeCard(mode, itemColor) {
                            if (mode.title == "Learning") {
                                onStartQuiz("Learning", emptyList(), isUtm, 0)
                            } else {
                                selectedMode = mode
                                showModeDialog = true
                            }
                        }
                    }
                }

                // Secțiunea AI Generator (Premium)
                AiGeneratorCard(
                    onClick = {
                        if (authViewModel.isCurrentUserGuest) showGuestPremiumDialog = true
                        else onNavigateToCustomQuiz()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Switch pentru activarea modului UTM
                UTMModeSwitch(
                    isUtm = isUtm,
                    onToggle = { newValue ->
                        if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuViewModel.toggleUtmMode(newValue)
                        if (newValue) showUtmInfoDialog = true
                    }
                )
            }
        }
    }

    // Gestionare Dialoguri
    if (showUtmInfoDialog) UtmInfoDialog(onDismiss = { showUtmInfoDialog = false })

    if (showGuestPremiumDialog) {
        LoginRequiredDialog(
            featureName = "AI Quiz Generator",
            description = "Unlock the power of Gemini AI to create custom quizzes directly from your PDF documents.",
            onDismiss = { showGuestPremiumDialog = false },
            onGoToLogin = {
                showGuestPremiumDialog = false
                onNavigateToLogin()
            }
        )
    }

    if (showModeDialog && selectedMode != null) {
        GameModeSelectionDialog(
            mode = selectedMode!!,
            menuViewModel = menuViewModel,
            categoriesList = categoriesList,
            onDismiss = { showModeDialog = false },
            onStart = {
                showModeDialog = false
                onStartQuiz(selectedMode!!.title, menuViewModel.selectedCategories.toList(), isUtm, menuViewModel.questionCount)
            }
        )
    }
}

// ========================== COMPONENTE HEADER & BANNER ==========================

@Composable
fun HomeHeader(
    username: String,
    avatarId: Int,
    onProfileClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(50.dp)).clickable { onProfileClick() }.padding(end = 8.dp)
        ) {
            Image(
                painter = painterResource(id = UserAvatar.fromId(avatarId).drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(QuizTheme.colors.primary.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Welcome back,", style = MaterialTheme.typography.labelMedium, color = QuizTheme.colors.textSecondary)
                Text(text = username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = QuizTheme.colors.textMain)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLeaderboardClick) { Icon(Icons.Default.EmojiEvents, null, tint = QuizTheme.colors.primary, modifier = Modifier.size(28.dp)) }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null, tint = QuizTheme.colors.primary, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
fun AiGeneratorCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F0FF)),
        border = BorderStroke(1.dp, Color(0xFFE0C3FF).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color(0xFF8B5CF6).copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Quiz Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6D28D9))
                Text("Transform PDFs into questions", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6D28D9).copy(alpha = 0.7f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF6D28D9).copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

// ========================== COMPONENTE MODURI JOC & SWITCH ==========================

@Composable
fun GameModeCard(mode: GameMode, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(1f).clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(60.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(getIconForMode(mode.title), null, tint = color, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun UTMModeSwitch(isUtm: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isUtm) QuizTheme.colors.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, QuizTheme.colors.primary.copy(alpha = 0.4f)),
        modifier = Modifier.padding(bottom = 24.dp).width(160.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🎓 UTM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = QuizTheme.colors.primary)
            Switch(
                checked = isUtm,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.7f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = QuizTheme.colors.primary,
                    uncheckedThumbColor = QuizTheme.colors.primary,
                    uncheckedTrackColor = QuizTheme.colors.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// ========================== DIALOGURI & HELPERS ==========================

@Composable
fun UtmInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = QuizTheme.colors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UTM Academy Mode", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Text(
                "Welcome to the specialized preparation module! This section is exclusively designed for Computer Science students at Titu Maiorescu University in Bucharest.\n\nAll questions are based on the official licensing exam curriculum and are presented in Romanian to reflect your study materials.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("GOT IT", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeSelectionDialog(mode: GameMode, menuViewModel: MenuViewModel, categoriesList: List<String>, onDismiss: () -> Unit, onStart: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(28.dp),
        confirmButton = {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = QuizTheme.colors.primary)) {
                Text("START GAME", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Maybe later", color = QuizTheme.colors.textSecondary) } },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = QuizTheme.colors.primary.copy(0.1f), modifier = Modifier.size(60.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(getIconForMode(mode.title), null, tint = QuizTheme.colors.primary, modifier = Modifier.size(32.dp)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(mode.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = QuizTheme.colors.textMain)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(mode.description, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = QuizTheme.colors.textSecondary)
                Spacer(modifier = Modifier.height(24.dp))
                if (mode.title != "Against Time" && mode.title != "Sudden Death") {
                    QuestionCountDropdown(menuViewModel)
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Categories:", fontWeight = FontWeight.Bold, color = QuizTheme.colors.textMain)
                Box(modifier = Modifier.height(180.dp).padding(top = 8.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(categoriesList) { category ->
                            val isSelected = menuViewModel.selectedCategories.contains(category)
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { menuViewModel.toggleCategory(category) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) QuizTheme.colors.primary.copy(alpha = 0.08f) else Color.Transparent,
                                border = BorderStroke(width = 1.dp, color = if (isSelected) QuizTheme.colors.primary else Color.LightGray.copy(alpha = 0.6f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSelected, onCheckedChange = { menuViewModel.toggleCategory(category) }, colors = CheckboxDefaults.colors(checkedColor = QuizTheme.colors.primary))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = QuizTheme.colors.textMain)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = QuizTheme.colors.surface,
        shape = RoundedCornerShape(32.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionCountDropdown(menuViewModel: MenuViewModel) {
    val options = listOf(10, 15, 20, 30)
    ExposedDropdownMenuBox(expanded = menuViewModel.isExpended, onExpandedChange = { menuViewModel.isExpended = !menuViewModel.isExpended }) {
        OutlinedTextField(
            value = menuViewModel.questionCount.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Questions") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuViewModel.isExpended) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QuizTheme.colors.primary, focusedLabelColor = QuizTheme.colors.primary, focusedTrailingIconColor = QuizTheme.colors.primary)
        )
        ExposedDropdownMenu(expanded = menuViewModel.isExpended, onDismissRequest = { menuViewModel.isExpended = false }) {
            options.forEach { selectionOption ->
                DropdownMenuItem(text = { Text(selectionOption.toString()) }, onClick = { menuViewModel.updateQuestionCount(selectionOption) })
            }
        }
    }
}

fun getIconForMode(title: String): ImageVector {
    return when (title) {
        "Classic" -> Icons.Default.PlayArrow
        "Against Time" -> Icons.Default.Timer
        "Sudden Death" -> Icons.Default.Warning
        "Learning" -> Icons.AutoMirrored.Filled.MenuBook
        else -> Icons.Default.Quiz
    }
}