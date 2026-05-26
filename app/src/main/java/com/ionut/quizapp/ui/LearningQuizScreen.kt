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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningQuizScreen(
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    onExit: () -> Unit
) {
    val colors = QuizTheme.colors
    val currentQuestion = quizViewModel.questions.getOrNull(quizViewModel.currentLearningIndex)

    var showExitDialog by remember { mutableStateOf(false) }

    var showExplanationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isGuest by remember(authViewModel) {
        derivedStateOf { authViewModel.isCurrentUserGuest }
    }

    LaunchedEffect(quizViewModel.aiExplanation) {
        if (quizViewModel.aiExplanation != null) {
            showExplanationSheet = true
        }
    }

    // Dialog de ieșire personalizat (Modern M3 Design)
    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = QuizTheme.colors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icoană dinamică (Exit sau Save)
                    Surface(
                        shape = CircleShape,
                        color = if (isGuest) Color(0xFFFFEBEE) else colors.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGuest) Icons.Default.Close else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isGuest) Color(0xFFE53935) else colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isGuest) "Exit Learning Mode?" else "Save your progress?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = QuizTheme.colors.textMain,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isGuest)
                            "You are using a Guest account, so your progress will not be saved. Are you sure you want to exit?"
                        else
                            "Your current position will be saved so you can resume learning later.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = QuizTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Buton Principal Confirmare
                    Button(
                        onClick = {
                            showExitDialog = false
                            quizViewModel.saveProgressAndExit(isGuest, onExit)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGuest) Color(0xFFE53935) else colors.primary
                        )
                    ) {
                        Text(
                            text = if (isGuest) "EXIT WITHOUT SAVING" else "SAVE & EXIT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buton Secundar
                    TextButton(
                        onClick = { showExitDialog = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continue Learning", color = QuizTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                        onClick = { quizViewModel.navigateLearning(-1) },
                        enabled = quizViewModel.currentLearningIndex > 0,
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
                        onClick = { quizViewModel.generateAiExplanation() },
                        enabled = quizViewModel.isLearningAnswerLocked && !quizViewModel.isAiLoading,
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
                        if (quizViewModel.isAiLoading) {
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
                        onClick = {quizViewModel.navigateLearning(1) },
                        enabled = quizViewModel.currentLearningIndex < quizViewModel.questions.size - 1,
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
        if (quizViewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (quizViewModel.errorMessage != null) {
            // Stat de eroare integrat în limba engleză
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = quizViewModel.errorMessage ?: "Error", color = Color.Red, textAlign = TextAlign.Center)
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
                                text = "QUESTION ${quizViewModel.currentLearningIndex + 1} OF ${quizViewModel.questions.size}",
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
                            progress = { (quizViewModel.currentLearningIndex + 1).toFloat() / quizViewModel.questions.size },
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
                            quizViewModel.currentOptions.forEach { option ->
                                val isSelected = quizViewModel.selectedLearningAnswer == option
                                val isCorrect = option == targetQuestion.correct_answer

                                LearningOptionCard(
                                    option = option,
                                    isSelected = isSelected,
                                    isCorrect = isCorrect,
                                    isLocked = quizViewModel.isLearningAnswerLocked,
                                    onClick = { quizViewModel.submitLearningAnswer(option) }
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    // --- AI EXPLANATION BOTTOM SHEET ---
    if (showExplanationSheet && quizViewModel.aiExplanation != null) {
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

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = colors.textSecondary.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(16.dp))

                // Textul propriu-zis (acum are tot spațiul din lume)
                Text(
                    text = quizViewModel.aiExplanation ?: "",
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