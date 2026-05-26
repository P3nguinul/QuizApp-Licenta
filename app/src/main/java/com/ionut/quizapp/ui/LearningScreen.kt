package com.ionut.quizapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.LearningViewModel
import com.ionut.quizapp.viewmodels.ProgressData
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    learningViewModel: LearningViewModel,
    quizViewModel: QuizViewModel,
    authViewModel: AuthViewModel = viewModel(),
    isUtm: Boolean,
    onBack: () -> Unit,
    onDifficultySelect: (String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    val colors = QuizTheme.colors
    val categories = if (isUtm) learningViewModel.utmCategoriesUI else learningViewModel.normalCategoriesUI

    LaunchedEffect(isUtm) {
        learningViewModel.loadDetailedProgress(isUtm)
    }

    // Gradient subtil de fundal pentru un aspect premium, renunțăm la albul plat
    val bgGradient = Brush.verticalGradient(
        colors = listOf(colors.background, colors.background.copy(alpha = 0.8f))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = (if (isUtm) "UTM ACADEMY" else "LEARNING HUB").uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = colors.textMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = colors.background
    ) { padding ->
        if (learningViewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary, strokeWidth = 4.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {

                        if (authViewModel.isCurrentUserGuest) {
                            GuestWarningBanner(onLoginClick = onLoginClick)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = "Your Progress",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = colors.textMain
                        )
                        Text(
                            text = "Select a difficulty level to start studying.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                // itemsIndexed ne ajută să aplicăm animații de tip "cascade fade-in" în funcție de index
                itemsIndexed(categories) { index, category ->
                    val diffMap = learningViewModel.detailedProgressMap[category] ?: emptyMap()

                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { isVisible = true }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(300 + index * 50)) +
                                slideInVertically(animationSpec = tween(300 + index * 50), initialOffsetY = { 50 })
                    ) {
                        LearningCategoryCard(
                            categoryName = category,
                            difficultyMap = diffMap,
                            isUtm = isUtm,
                            onStart = { cat, diff ->
                                quizViewModel.loadLearningQuestions(cat, diff, isUtm)
                                onDifficultySelect(cat, diff)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LearningCategoryCard(
    categoryName: String,
    difficultyMap: Map<String, ProgressData>,
    isUtm: Boolean,
    onStart: (String, String) -> Unit
) {
    val colors = QuizTheme.colors
    // Folosim o paletă de culori modernă: Mov-Albastru regal pentru UTM, Teal/Neon Pink pentru Normal
    val accentColor = if (isUtm) colors.primary else Color(0xFF00BFA5)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.15f)),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Categorie
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getIconForCategory(categoryName),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textMain
                )
            }

            Spacer(Modifier.height(20.dp))

            // Generăm toate cele 3 dificultăți indiferent de baza de date
            val difficulties = listOf("Easy", "Medium", "Hard")
            difficulties.forEachIndexed { index, diff ->
                val data = difficultyMap[diff] ?: ProgressData(0, 0)

                // Determinăm culoarea specifică nivelului pentru o mai bună ierarhie vizuală
                val diffColor = when(diff) {
                    "Easy" -> Color(0xFF4CAF50)
                    "Medium" -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }

                DifficultyRow(
                    label = diff,
                    current = data.current,
                    total = data.total,
                    accentColor = diffColor,
                    onClick = {
                        // Permitem accesul doar dacă există întrebări scrise în sistem
                        if (data.total > 0) onStart(categoryName, diff)
                    }
                )

                if (index < difficulties.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.LightGray.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DifficultyRow(
    label: String,
    current: Int,
    total: Int,
    accentColor: Color,
    onClick: () -> Unit
) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val isCompleted = current >= total && total > 0
    val hasQuestions = total > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!hasQuestions) Color.Gray else QuizTheme.colors.textMain
                )
                if (isCompleted) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Indicator bară
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isCompleted) Color(0xFF4CAF50) else accentColor,
                trackColor = if (hasQuestions) accentColor.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasQuestions) "$current / $total Qs" else "No questions available",
                style = MaterialTheme.typography.labelSmall,
                color = QuizTheme.colors.textSecondary
            )
        }

        // Buton minimalist, specific aplicațiilor moderne comerciale
        Button(
            onClick = onClick,
            enabled = hasQuestions,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) Color(0xFF4CAF50).copy(0.12f) else accentColor.copy(0.12f),
                contentColor = if (isCompleted) Color(0xFF4CAF50) else accentColor,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.1f),
                disabledContentColor = Color.Gray.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (isCompleted) "REVIEW" else "LEARN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun getIconForCategory(name: String): ImageVector {
    return when (name) {
        "General Knowledge" -> Icons.Default.Lightbulb
        "Films" -> Icons.Default.Movie
        "Music" -> Icons.Default.MusicNote
        "Television" -> Icons.Default.Tv
        "Video Games" -> Icons.Default.Gamepad
        "Science and Nature" -> Icons.Default.Science
        "Computer Science" -> Icons.Default.Terminal
        "Mathematics" -> Icons.Default.Calculate
        "Sports" -> Icons.Default.EmojiEvents
        "Geography" -> Icons.Default.Public
        "History" -> Icons.Default.History
        "Animals" -> Icons.Default.Pets
        "Vehicles" -> Icons.Default.DirectionsCar
        "Japanese Anime & Manga" -> Icons.Default.Animation
        "Comics" -> Icons.Default.AutoStories
        "Programarea Calculatoarelor" -> Icons.Default.Code
        "Baze de Date" -> Icons.Default.Storage
        "Arhitectura Sistemelor" -> Icons.Default.Memory
        "Retele de Calculatoare" -> Icons.Default.Lan
        else -> Icons.Default.Category
    }
}