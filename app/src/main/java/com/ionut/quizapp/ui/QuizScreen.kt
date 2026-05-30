package com.ionut.quizapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionut.quizapp.ui.theme.QuizTheme
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

    // Stare locală pentru a colora butonul selectat înainte de a trece la următoarea întrebare
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        showExitDialog = true // Afișăm modalul tău în loc să ieșim din ecran
    }

    // Încărcăm întrebările la pornire
    LaunchedEffect(Unit) {
        if (categories != "Custom") {
            viewModel.loadQuestions(isUtm, categories.split(","), count, mode)
        }
    }

    LaunchedEffect(viewModel.isGameOver) {
        if (viewModel.isGameOver) {
            onFinish()
        }
    }

    LaunchedEffect(viewModel.timeLeft) {
        if (viewModel.isTimedMode && viewModel.timeLeft == 10 && !viewModel.isGameOver) {
            soundManager.playTimerWarning()
        }
    }

    LaunchedEffect(viewModel.isGameOver) {
        if (viewModel.isGameOver) {
            soundManager.stopTimerWarning() // Tăiem firul sunetului!
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
                        showExitDialog = false
                        viewModel.resetQuizState() // Curățăm starea
                        onExit() // Navigăm înapoi la meniu
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
    } else {

        if (viewModel.questions.isNotEmpty()) {
            // Luăm mereu prima întrebare din listă (Coadă)
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
                            IconButton(onClick = {
                                showExitDialog = true
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit")
                            }
                        },
                        actions = {
                            // Afișăm butonul SKIP DOAR dacă NU suntem în modul contra-cronometru
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

                    // --- ZONA DINAMICĂ: CRONOMETRU vs BARA DE PROGRES ---
                    if (viewModel.isTimedMode) {
                        // Design pentru modul AGAINST TIME
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (viewModel.timeLeft < 10) Color.Red else colors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${viewModel.timeLeft}s",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = if (viewModel.timeLeft < 10) Color.Red else colors.textMain
                            )
                        }

                        // Opțional: O bară care scade vizual odată cu timpul
                        LinearProgressIndicator(
                            progress = { viewModel.timeLeft / 60f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (viewModel.timeLeft < 10) Color.Red else colors.primary,
                            trackColor = colors.primary.copy(alpha = 0.1f)
                        )
                    } else if (mode == "Sudden Death") {
                        // --- DESIGN EXCLUSIV PENTRU SUDDEN DEATH ---
                        val currentStreak = viewModel.score

                        // Desenăm o bară plină/activă care sugerează un modul de tip survival/trofeu
                        LinearProgressIndicator(
                            progress = { 1f }, // O lăsăm plină în permanență
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            // Roșu aprins pentru modul normal (pericol de moarte subită) sau Culoarea temei pentru UTM
                            color = if (isUtm) colors.primary else Color(0xFFE53935),
                            trackColor = colors.primary.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STREAK: $currentStreak 🔥",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isUtm) colors.primary else Color(0xFFE53935)
                            )
                            Text(
                                text = "No mistakes allowed!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        // Design pentru modul CLASSIC (Bara de progres normală)
                        val answeredCount = viewModel.quizHistory.size
                        val progress = answeredCount.toFloat() / count

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = if (isUtm) colors.primary else Color(0xFF4CAF50),
                            trackColor = colors.primary.copy(alpha = 0.1f)
                        )

                        Text(
                            text = "${answeredCount + 1} / $count",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- ÎNTREBAREA ---
                    Text(
                        text = currentQuestion.question_text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = colors.textMain,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- LISTA DE OPȚIUNI (Rămâne la fel) ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        viewModel.currentOptions.forEachIndexed { index, option ->
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
                                    if (selectedAnswerIndex == null) { // Dacă n-a apăsat deja pe ceva
                                        selectedAnswerIndex = index

                                        // 1. Vibrație instantanee la apăsare
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                        // 2. Redăm sunetul instantaneu!
                                        if (isCorrect) { // Folosim `isCorrect` care era deja definit mai sus de tine
                                            soundManager.playCorrect()
                                        } else {
                                            soundManager.playWrong()
                                        }

                                        // 3. Așteptăm o secundă ca să vadă culoarea, apoi trimitem răspunsul
                                        scope.launch {
                                            delay(1000)
                                            viewModel.submitAnswer(option)
                                            selectedAnswerIndex = null
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = containerColor,
                                // ADĂUGĂM BORDURĂ ȘI ELEVATION PENTRU CLARITATE
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.5f)
                                ),
                                shadowElevation = 4.dp, // Aceasta va crea profunzime
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp) // Reducem puțin padding-ul vertical să nu fie prea răsfirate
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp // Puțin mai mare pentru lizibilitate
                                        ),
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (!viewModel.isGameOver) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
    }
}
