package com.ionut.quizapp.features.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    soundManager: SoundManager,
    mode: String,
    isUtm: Boolean,
    categories: String,
    count: Int,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colors = QuizTheme.colors
    val scope = rememberCoroutineScope()

    var showExitDialog by remember { mutableStateOf(false) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        showExitDialog = true
    }

    LaunchedEffect(Unit) {
        if (categories != "Custom") {
            viewModel.loadQuestions(isUtm, categories.split(","), count, mode)
        }
    }

    LaunchedEffect(viewModel.isGameOver) {
        if (viewModel.isGameOver) {
            if (soundManager.isVibrationEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            soundManager.stopTimerWarning()
            onFinish()
        }
    }

    LaunchedEffect(viewModel.timeLeft) {
        if (viewModel.isTimedMode && viewModel.timeLeft == 10 && !viewModel.isGameOver) {
            soundManager.playTimerWarning()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Quit Quiz?") },
            text = { Text("Are you sure you want to leave? Your current progress and score will not be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (soundManager.isVibrationEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        showExitDialog = false
                        viewModel.resetQuizState()
                        onExit()
                    }
                ) {
                    Text("QUIT", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CONTINUE", color = colors.primary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(colors.background, colors.surface)
    )

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.primary)
        }
    } else if (viewModel.errorMessage != null) {
        // --- ACESTA ESTE ECRANUL NOU CARE APĂRE CÂND NU AI INTERNET ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF44336),
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.errorMessage ?: "An unexpected error occurred.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (categories != "Custom") {
                            viewModel.loadQuestions(isUtm, categories.split(","), count, mode)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RETRY", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = {
                    viewModel.resetQuizState()
                    onExit()
                }) {
                    Text("Back to Menu", color = colors.primary)
                }
            }
        }
    } else {
        if (viewModel.questions.isNotEmpty()) {
            val currentQuestion = viewModel.questions[0]

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = mode.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showExitDialog = true }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit")
                            }
                        },
                        actions = {
                            if (!viewModel.isTimedMode && mode != "Sudden Death") {
                                TextButton(onClick = {
                                    if (selectedAnswerIndex == null) {
                                        viewModel.skipQuestion()
                                    }
                                }) {
                                    Text("SKIP", color = colors.primary, fontWeight = FontWeight.Black)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgGradient)
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- HEADER (Rămâne fix sus) ---
                    if (viewModel.isTimedMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, tint = if (viewModel.timeLeft < 10) Color.Red else colors.primary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("${viewModel.timeLeft}s", fontWeight = FontWeight.Black, fontSize = 24.sp)
                        }
                        LinearProgressIndicator(
                            progress = { viewModel.timeLeft / 60f },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(CircleShape),
                            color = if (viewModel.timeLeft < 10) Color.Red else colors.primary
                        )
                    } else if (mode == "Sudden Death") {
                        Text("STREAK: ${viewModel.score} 🔥", fontWeight = FontWeight.Black, color = if (isUtm) colors.primary else Color(0xFFE53935))
                    } else {
                        val answeredCount = viewModel.quizHistory.size
                        LinearProgressIndicator(
                            progress = { answeredCount.toFloat() / count },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = if (isUtm) colors.primary else Color(0xFF4CAF50)
                        )
                        Text("${answeredCount + 1} / $count", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- ZONA CERINȚEI (CAPSULA FIXĂ CU SCROLL INTERN) ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp) // Am pus o limită de înălțime ca să nu fure tot ecranul
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()) // Scroll doar dacă cerința e gigantă
                        ) {
                            Text(
                                text = currentQuestion.question_text,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = if (isUtm) FontFamily.Monospace else FontFamily.Default,
                                    fontWeight = if (isUtm) FontWeight.Medium else FontWeight.ExtraBold,
                                    fontSize = if (isUtm) 15.sp else 20.sp,
                                    lineHeight = if (isUtm) 22.sp else 28.sp
                                ),
                                color = colors.textMain
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- ZONA RĂSPUNSURILOR (SINGURA CARE SCROLLEAZĂ LIBER) ---
                    // weight(1f) face ca această listă să ocupe tot spațiul rămas liber jos
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Folosim itemsIndexed în loc de forEachIndexed pentru LazyColumn
                        items(viewModel.currentOptions.size) { index ->
                            val option = viewModel.currentOptions[index]
                            val isCorrect = option == currentQuestion.correct_answer
                            val isSelected = selectedAnswerIndex == index

                            val containerColor = when {
                                isSelected && isCorrect -> Color(0xFF4CAF50)
                                isSelected && !isCorrect -> Color(0xFFF44336)
                                selectedAnswerIndex != null && isCorrect -> Color(0xFFE8F5E9)
                                else -> colors.surface
                            }

                            val contentColor = when {
                                isSelected -> Color.White
                                selectedAnswerIndex != null && isCorrect -> Color(0xFF2E7D32)
                                else -> colors.textMain
                            }

                            Surface(
                                onClick = {
                                    if (selectedAnswerIndex == null) {
                                        selectedAnswerIndex = index
                                        if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isCorrect) soundManager.playCorrect() else soundManager.playWrong()
                                        scope.launch {
                                            delay(1000)
                                            viewModel.submitAnswer(option)
                                            selectedAnswerIndex = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = containerColor,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.5f)
                                ),
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = if (isUtm) FontFamily.Monospace else FontFamily.Default,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isUtm) 15.sp else 18.sp
                                        ),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }

                    // Spațiu extra jos ca butoanele să nu stea lipite de marginea telefonului
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        } else if (!viewModel.isGameOver) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
    }
}
