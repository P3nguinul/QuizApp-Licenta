package com.ionut.quizapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.data.GameMode
import com.ionut.quizapp.data.gameModes
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.MenuViewModel
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToCustomQuiz: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onStartQuiz: (mode: String, categories: List<String>, isUtm: Boolean, count: Int) -> Unit, // Am adăugat count aici
    authViewModel: AuthViewModel = viewModel(),
    menuViewModel: MenuViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel()
) {
    val isUtm = menuViewModel.isUtmMode
    var showDialog by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<GameMode?>(null) }
    var showGuestPremiumDialog by remember { mutableStateOf(false) }

    val playfulColors = listOf(
        QuizTheme.colors.primary,
        QuizTheme.colors.secondary,
        Color(0xFFFFA000),
        Color(0xFF388E3C)
    )

    val backgroundGradient = if (isUtm) {
        Brush.verticalGradient(listOf(QuizTheme.colors.background, Color.White))
    } else {
        Brush.verticalGradient(listOf(QuizTheme.colors.accent.copy(alpha = 0.3f), QuizTheme.colors.background))
    }

    val categoriesList = if (isUtm) {
        listOf("All UTM", "Programarea Calculatoarelor", "Baze de Date", "Arhitectura Sistemelor", "Retele de Calculatoare")
    } else {
        listOf("All Categories", "General Knowledge", "Films", "Music", "Television", "Video Games", "Science and Nature", "Computer Science", "Mathematics", "Sports", "Geography", "History", "Animals", "Vehicles", "Japanese Anime & Manga", "Comics")
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchProfile()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isUtm) "UTM ACADEMY" else "QUIZ ADVENTURE",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLeaderboard) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            modifier = Modifier.size(28.dp),
                            tint = QuizTheme.colors.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(32.dp), tint = QuizTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Hey there,", style = MaterialTheme.typography.titleMedium, color = QuizTheme.colors.textSecondary)
                Text(
                    text = authViewModel.currentUserProfile ?: "Explorer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.textMain
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- 1. GRILA DE MODURI DE JOC ---
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
                                showDialog = true
                            }
                        }
                    }
                }

                // --- 2. BUTON AI CUSTOM QUIZ (Afară din grilă) ---
                Button(
                    onClick = {
                        // Înlocuiește quizViewModel.isUserGuest cu authViewModel.isCurrentUserGuest
                        if (authViewModel.isCurrentUserGuest) {
                            showGuestPremiumDialog = true
                        } else {
                            onNavigateToCustomQuiz()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "GENERATE QUIZ FROM PDF",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- 3. SWITCH-UL UTM ---
                UTMModeSwitch(isUtm) { menuViewModel.toggleUtmMode(it) }
            }
        }
    }

    // --- DIALOGUL PREMIUM PENTRU GUESTS ---
    if (showGuestPremiumDialog) {
        LoginRequiredDialog(
            featureName = "AI Quiz Generator",
            description = "Unlock the power of Gemini AI to create custom quizzes directly from your PDF documents. Save them to your private library and study smarter!",
            onDismiss = { showGuestPremiumDialog = false },
            onGoToLogin = {
                showGuestPremiumDialog = false
                onNavigateToLogin()
            }
        )
    }

    // --- MODALUL REPROIECTAT ---
    if (showDialog && selectedMode != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(28.dp),
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onStartQuiz(
                            selectedMode!!.title,
                            menuViewModel.selectedCategories.toList(),
                            isUtm,
                            menuViewModel.questionCount
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuizTheme.colors.primary)
                ) { Text("START GAME", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Maybe later", color = QuizTheme.colors.textSecondary)
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = CircleShape, color = QuizTheme.colors.primary.copy(0.1f), modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(getIconForMode(selectedMode!!.title), null, tint = QuizTheme.colors.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(selectedMode!!.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(selectedMode!!.description, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = QuizTheme.colors.textSecondary)

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedMode!!.title != "Against Time" && selectedMode!!.title != "Sudden Death") {
                        QuestionCountDropdown(menuViewModel)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Categories:", fontWeight = FontWeight.Bold, color = QuizTheme.colors.textMain)

                    Box(modifier = Modifier.height(200.dp).padding(top = 8.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categoriesList) { category ->
                                val isSelected = menuViewModel.selectedCategories.contains(category)
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { menuViewModel.toggleCategory(category) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) QuizTheme.colors.primary.copy(0.1f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) QuizTheme.colors.primary else Color.LightGray.copy(0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { menuViewModel.toggleCategory(category) },
                                            colors = CheckboxDefaults.colors(checkedColor = QuizTheme.colors.primary)
                                        )
                                        Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
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
                    checkedBorderColor = QuizTheme.colors.primary,
                    uncheckedThumbColor = QuizTheme.colors.primary,
                    uncheckedTrackColor = QuizTheme.colors.primary.copy(alpha = 0.1f),
                    uncheckedBorderColor = QuizTheme.colors.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun GameModeCard(mode: GameMode, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconForMode(mode.title),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mode.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionCountDropdown(menuViewModel: MenuViewModel) {
    val options = listOf(10, 15, 20, 30)
    val colors = QuizTheme.colors

    Column {
        Text("Number of Questions:", fontWeight = FontWeight.Bold, color = colors.textMain)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = menuViewModel.isExpended,
            onExpandedChange = { menuViewModel.isExpended = !menuViewModel.isExpended }
        ) {
            OutlinedTextField(
                value = menuViewModel.questionCount.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuViewModel.isExpended) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.primary.copy(alpha = 0.5f),
                    focusedTrailingIconColor = colors.primary
                )
            )

            ExposedDropdownMenu(
                expanded = menuViewModel.isExpended,
                onDismissRequest = { menuViewModel.isExpended = false },
                modifier = Modifier.background(colors.surface)
            ) {
                options.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = selectionOption.toString(),
                                color = if (selectionOption == menuViewModel.questionCount) colors.primary else colors.textMain,
                                fontWeight = if (selectionOption == menuViewModel.questionCount) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            menuViewModel.updateQuestionCount(selectionOption)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
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