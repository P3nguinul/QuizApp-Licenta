package com.ionut.quizapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningQuizScreen(
    viewModel: QuizViewModel,
    onExit: () -> Unit
) {
    val colors = QuizTheme.colors
    val currentQuestion = viewModel.questions.getOrNull(viewModel.currentLearningIndex)

    var showExitDialog by remember { mutableStateOf(false) }

    var showExplanationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Când AI-ul răspunde cu succes, declanșăm deschiderea sertarului
    LaunchedEffect(viewModel.aiExplanation) {
        if (viewModel.aiExplanation != null) {
            showExplanationSheet = true
        }
    }

    // Dialog de ieșire personalizat (Modern M3 Design)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveProgressAndExit { onExit() } },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save & Exit", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            title = { Text("Save your progress?", fontWeight = FontWeight.Black) },
            text = { Text("Your current position will be saved so you can resume learning later.") },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "STUDY MODULE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = colors.textMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Study", tint = colors.textMain)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // --- CAROUSEL NAVIGATION CONTROLS (BOTTOM BAR) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Săgeată Înapoi
                    FilledIconButton(
                        onClick = { viewModel.navigateLearning(-1) },
                        enabled = viewModel.currentLearningIndex > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.primary.copy(alpha = 0.1f),
                            contentColor = colors.primary,
                            disabledContainerColor = colors.textSecondary.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }

                    // BUTONUL MAGIC GEMINI AI EXPLAIN
                    Button(
                        onClick = { viewModel.generateAiExplanation() },
                        enabled = viewModel.isLearningAnswerLocked && !viewModel.isAiLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE), // Culoare dedicată AI (Royal Purple)
                            contentColor = Color.White,
                            disabledContainerColor = colors.textSecondary.copy(alpha = 0.1f),
                            disabledContentColor = colors.textSecondary.copy(alpha = 0.4f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (viewModel.isAiLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }

                        Spacer(Modifier.width(8.dp))
                        Text("AI EXPLAIN", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    }

                    // Săgeată Înainte
                    FilledIconButton(
                        onClick = { viewModel.navigateLearning(1) },
                        enabled = viewModel.currentLearningIndex < viewModel.questions.size - 1,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.primary.copy(alpha = 0.1f),
                            contentColor = colors.primary,
                            disabledContainerColor = colors.textSecondary.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        },
        containerColor = colors.background
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (viewModel.errorMessage != null) {
            // Stat de eroare integrat în limba engleză
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = viewModel.errorMessage ?: "Error", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {
            currentQuestion?.let { q ->
                // Animație lină de tip slide-in când se schimbă întrebarea (Carousel Effect)
                AnimatedContent(
                    targetState = q,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "QuestionTransition"
                ) { targetQuestion ->

                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // Progress Indicator Superior
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUESTION ${viewModel.currentLearningIndex + 1} OF ${viewModel.questions.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = targetQuestion.difficulty.uppercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (viewModel.currentLearningIndex + 1).toFloat() / viewModel.questions.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = colors.primary,
                            trackColor = colors.primary.copy(alpha = 0.1f)
                        )

                        Spacer(Modifier.height(28.dp))

                        // Textul Întrebării pus într-un container elegant
                        Text(
                            text = targetQuestion.question_text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textMain,
                            lineHeight = 28.sp
                        )

                        Spacer(Modifier.height(32.dp))

                        // Lista Opțiunilor de Răspuns
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            viewModel.currentOptions.forEach { option ->
                                val isSelected = viewModel.selectedLearningAnswer == option
                                val isCorrect = option == targetQuestion.correct_answer

                                LearningOptionCard(
                                    option = option,
                                    isSelected = isSelected,
                                    isCorrect = isCorrect,
                                    isLocked = viewModel.isLearningAnswerLocked,
                                    onClick = { viewModel.submitLearningAnswer(option) }
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    // --- AI EXPLANATION BOTTOM SHEET ---
    if (showExplanationSheet && viewModel.aiExplanation != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showExplanationSheet = false
                // Opțional: viewModel.aiExplanation = null dacă vrei să resetezi când se închide
            },
            sheetState = sheetState,
            containerColor = colors.surface, // Se asortează cu tema ta
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp) // Spațiu generos jos
            ) {
                // Header-ul panoului
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6200EE).copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Icon",
                            tint = Color(0xFF6200EE),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "AI Explanation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.textMain
                    )
                }

                Divider(color = colors.textSecondary.copy(alpha = 0.1f))
                Spacer(Modifier.height(16.dp))

                // Textul propriu-zis (acum are tot spațiul din lume)
                Text(
                    text = viewModel.aiExplanation ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMain,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
fun LearningOptionCard(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val colors = QuizTheme.colors

    // Determinăm culorile în funcție de starea răspunsului (Verde = Corect, Roșu = Greșit)
    val cardColor = when {
        !isLocked -> colors.surface
        isCorrect -> Color(0xFFE8F5E9) // Verde foarte deschis premium
        isSelected && !isCorrect -> Color(0xFFFFEBEE) // Roșu foarte deschis premium
        else -> colors.surface
    }

    val borderColor = when {
        !isLocked -> if (isSelected) colors.primary else Color.LightGray.copy(alpha = 0.2f)
        isCorrect -> Color(0xFF4CAF50)
        isSelected && !isCorrect -> Color(0xFFF44336)
        else -> Color.LightGray.copy(alpha = 0.1f)
    }

    val textColor = when {
        !isLocked -> colors.textMain
        isCorrect -> Color(0xFF2E7D32)
        isSelected && !isCorrect -> Color(0xFFC62828)
        else -> colors.textMain.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(if (isSelected || (isLocked && isCorrect)) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isSelected && !isLocked) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || (isLocked && isCorrect)) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}