package com.ionut.quizapp.features.learning.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.auth.AuthViewModel
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningQuizScreen(
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    soundManager: SoundManager,
    onExit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colors = QuizTheme.colors
    val currentQuestion = quizViewModel.questions.getOrNull(quizViewModel.currentLearningIndex)

    // Dialog States
    var showExitDialog by remember { mutableStateOf(false) }
    var showExplanationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isGuest by remember(authViewModel) {
        derivedStateOf { authViewModel.isCurrentUserGuest }
    }

    // Gestionare buton Back hardware/gesturi
    BackHandler {
        handleExitLogic(quizViewModel, onExit) { showExitDialog = true }
    }

    // Auto-deschidere Bottom Sheet când AI returnează răspunsul
    LaunchedEffect(quizViewModel.aiExplanation) {
        if (quizViewModel.aiExplanation != null) showExplanationSheet = true
    }

    Scaffold(
        topBar = {
            LearningTopBar(
                isCustomMode = quizViewModel.isCustomQuizMode,
                onExitClick = { handleExitLogic(quizViewModel, onExit) { showExitDialog = true } }
            )
        },
        bottomBar = {
            LearningNavigationBottomBar(
                quizViewModel = quizViewModel,
                isGuest = isGuest,
                onExit = onExit
            )
        },
        containerColor = colors.background
    ) { padding ->

        // Conținut Principal
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (quizViewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = colors.primary)
            } else if (quizViewModel.errorMessage != null) {
                Text(
                    text = quizViewModel.errorMessage ?: "Unknown Error",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                currentQuestion?.let { question ->
                    AnimatedContent(
                        targetState = question,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "QuestionTransition"
                    ) { targetQuestion ->
                        LearningContent(
                            targetQuestion = targetQuestion,
                            quizViewModel = quizViewModel,
                            soundManager = soundManager,
                            haptic = haptic
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGURI ȘI PANOURI ---

    if (showExitDialog) {
        LearningExitDialog(
            isGuest = isGuest,
            onDismiss = { showExitDialog = false },
            onConfirm = {
                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                quizViewModel.saveProgressAndExit(isGuest, onExit)
            }
        )
    }

    if (quizViewModel.showSaveCustomQuizDialog) {
        SaveCustomQuizDialog(
            quizViewModel = quizViewModel,
            onDismiss = { quizViewModel.showSaveCustomQuizDialog = false },
            onSave = { quizViewModel.saveCustomQuizAndExit(onExit) },
            onDiscard = {
                if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                quizViewModel.discardCustomQuizAndExit(onExit)
            }
        )
    }

    if (showExplanationSheet && quizViewModel.aiExplanation != null) {
        AiExplanationSheet(
            explanation = quizViewModel.aiExplanation!!,
            sheetState = sheetState,
            onDismiss = { showExplanationSheet = false }
        )
    }
}

// ========================== COMPONENTE STRUCTURALE ==========================

@Composable
private fun LearningContent(
    targetQuestion: com.ionut.quizapp.data.Question,
    quizViewModel: QuizViewModel,
    soundManager: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val colors = QuizTheme.colors

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))

        // Progress Area
        Column {
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
                if (!quizViewModel.isCustomQuizMode) {
                    Surface(shape = RoundedCornerShape(8.dp), color = colors.primary.copy(alpha = 0.1f)) {
                        Text(
                            text = targetQuestion.difficulty.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (quizViewModel.currentLearningIndex + 1).toFloat() / quizViewModel.questions.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = colors.primary
            )
        }

        Spacer(Modifier.height(28.dp))

        // Question Card
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = targetQuestion.question_text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = if (quizViewModel.isUtmMode) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = if (quizViewModel.isUtmMode) FontWeight.Medium else FontWeight.ExtraBold,
                        fontSize = if (quizViewModel.isUtmMode) 16.sp else 22.sp,
                        lineHeight = 28.sp
                    ),
                    color = colors.textMain
                )
            }
        }

        // Options List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(quizViewModel.currentOptions.size) { index ->
                val option = quizViewModel.currentOptions[index]
                LearningOptionCard(
                    option = option,
                    isSelected = quizViewModel.selectedLearningAnswer == option,
                    isCorrect = option == targetQuestion.correct_answer,
                    isLocked = quizViewModel.isLearningAnswerLocked,
                    onClick = {
                        if (!quizViewModel.isLearningAnswerLocked) {
                            if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (option == targetQuestion.correct_answer) soundManager.playCorrect() else soundManager.playWrong()
                            quizViewModel.submitLearningAnswer(option)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningTopBar(isCustomMode: Boolean, onExitClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (isCustomMode) "CUSTOM AI QUIZ" else "STUDY MODULE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onExitClick) {
                Icon(Icons.Default.Close, contentDescription = "Exit")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun LearningNavigationBottomBar(
    quizViewModel: QuizViewModel,
    isGuest: Boolean,
    onExit: () -> Unit
) {
    val colors = QuizTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Row(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { quizViewModel.navigateLearning(-1) },
                enabled = quizViewModel.currentLearningIndex > 0,
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }

            // AI Button
            Button(
                onClick = { quizViewModel.generateAiExplanation() },
                enabled = quizViewModel.isLearningAnswerLocked && !quizViewModel.isAiLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                modifier = Modifier.height(48.dp)
            ) {
                if (quizViewModel.isAiLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("AI EXPLAIN", fontWeight = FontWeight.Black)
            }

            // Next / Finish Logic
            val isLast = quizViewModel.currentLearningIndex == quizViewModel.questions.size - 1
            if (isLast) {
                Button(
                    onClick = { handleExitLogic(quizViewModel, onExit) { /* Deja la ultimul element */ } },
                    modifier = Modifier.height(48.dp)
                ) { Text("FINISH", fontWeight = FontWeight.Black) }
            } else {
                FilledIconButton(
                    onClick = { quizViewModel.navigateLearning(1) },
                    modifier = Modifier.size(48.dp)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
            }
        }
    }
}

// ========================== DIALOGURI (COMPONENTS) ==========================

@Composable
private fun LearningExitDialog(isGuest: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = QuizTheme.colors.surface, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = if (isGuest) Color(0xFFFFEBEE) else QuizTheme.colors.primary.copy(0.1f), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(if (isGuest) Icons.Default.Close else Icons.Default.Info, null, tint = if (isGuest) Color(0xFFE53935) else QuizTheme.colors.primary, modifier = Modifier.size(36.dp)) }
                }
                Spacer(Modifier.height(24.dp))
                Text(if (isGuest) "Exit Learning Mode?" else "Save your progress?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(if (isGuest) "Your progress will not be saved." else "Resume learning later.", textAlign = TextAlign.Center, color = QuizTheme.colors.textSecondary)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isGuest) Color(0xFFE53935) else QuizTheme.colors.primary)) {
                    Text(if (isGuest) "EXIT WITHOUT SAVING" else "SAVE & EXIT", fontWeight = FontWeight.ExtraBold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Continue Learning", color = QuizTheme.colors.textSecondary) }
            }
        }
    }
}

@Composable
private fun SaveCustomQuizDialog(quizViewModel: QuizViewModel, onDismiss: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = QuizTheme.colors.surface, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome, null, tint = QuizTheme.colors.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Save this Quiz?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = quizViewModel.customQuizTitleInput, onValueChange = { quizViewModel.customQuizTitleInput = it }, label = { Text("Quiz Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(32.dp))
                Button(onClick = onSave, enabled = quizViewModel.customQuizTitleInput.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("SAVE TO LIBRARY", fontWeight = FontWeight.ExtraBold) }
                TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("DELETE & EXIT", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiExplanationSheet(explanation: String, sheetState: SheetState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color(0xFF6200EE).copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6200EE), modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("AI Explanation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = QuizTheme.colors.textSecondary.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))
            Text(text = explanation, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
        }
    }
}

// ========================== ELEMENTE DE LISTĂ ȘI LOGICĂ UTILS ==========================

@Composable
fun LearningOptionCard(option: String, isSelected: Boolean, isCorrect: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val colors = QuizTheme.colors
    val cardColor = when {
        !isLocked -> colors.surface
        isCorrect -> Color(0xFFE8F5E9)
        isSelected && !isCorrect -> Color(0xFFFFEBEE)
        else -> colors.surface
    }
    val borderColor = when {
        !isLocked -> if (isSelected) colors.primary else Color.LightGray.copy(alpha = 0.2f)
        isCorrect -> Color(0xFF4CAF50)
        isSelected && !isCorrect -> Color(0xFFF44336)
        else -> Color.LightGray.copy(alpha = 0.1f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(if (isSelected || (isLocked && isCorrect)) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isSelected && !isLocked) 4.dp else 1.dp
    ) {
        Text(
            text = option,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected || (isLocked && isCorrect)) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isLocked && isCorrect) Color(0xFF2E7D32) else if (isLocked && isSelected) Color(0xFFC62828) else colors.textMain
        )
    }
}

private fun handleExitLogic(quizViewModel: QuizViewModel, onExit: () -> Unit, onShowExitDialog: () -> Unit) {
    if (quizViewModel.isCustomQuizMode && !quizViewModel.isReplayingCustomQuiz) {
        quizViewModel.showSaveCustomQuizDialog = true
    } else if (quizViewModel.isCustomQuizMode && quizViewModel.isReplayingCustomQuiz) {
        quizViewModel.resetQuizState()
        onExit()
    } else {
        onShowExitDialog()
    }
}