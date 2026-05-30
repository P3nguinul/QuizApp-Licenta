package com.ionut.quizapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.logic.GameModeLogic
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager

@Composable
fun ResultScreen(
    viewModel: QuizViewModel,
    soundManager: SoundManager,
    onNavigateBack: () -> Unit
) {
    val score = viewModel.score
    val total = if (viewModel.isTimedMode) viewModel.totalAnsweredQuestions else viewModel.totalQuestionsCount
    val colors = QuizTheme.colors
    var showAnswers by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack() // Face exact ce face și butonul cel mare "BACK TO MENU"
    }

    LaunchedEffect(Unit) {
        soundManager.playFinish()
    }

    // Scaffold ne ajută să punem butonul fix la bază (bottomBar)
    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            // Butonul fixat la bază
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp) // Spațiu în jurul butonului
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("BACK TO MENU", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Respectă spațiul ocupat de bottomBar
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text("QUIZ COMPLETED!", color = colors.primary, fontWeight = FontWeight.ExtraBold)

            // Datele din ViewModel
            if (viewModel.isNewHighScore) {
                Text(
                    "🎉 NEW PERSONAL BEST! 🎉",
                    color = Color(0xFFFFD700), // Auriu
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = if (viewModel.isSuddenDeathMode) "$score STREAK 🔥" else "$score / $total",
                fontSize = if (viewModel.isSuddenDeathMode) 44.sp else 64.sp,
                fontWeight = FontWeight.Black,
                color = colors.textMain
            )

            if (!viewModel.isSuddenDeathMode) {
                Text(
                    text = "Accuracy: ${viewModel.accuracy}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Divider cu Check Answers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAnswers = !showAnswers }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(0.4f))
                Text(
                    " CHECK ANSWERS ",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (showAnswers) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = colors.textSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(0.4f))
            }

            // Aici e magia: weight(1f) face ca această zonă să ocupe tot spațiul liber
            // împingând elementele de sus în sus și lăsând loc butonului de jos
            AnimatedVisibility(
                visible = showAnswers,
                modifier = Modifier.weight(1f), // Ocupă restul ecranului
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.quizHistory) { history ->
                        AnswerReviewItem(history, colors, viewModel.isUtmMode)
                    }
                }
            }

            // Dacă răspunsurile nu sunt afișate, punem un Spacer ca să menținem layout-ul curat
            if (!showAnswers) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AnswerReviewItem(
    history: com.ionut.quizapp.viewmodels.AnswerHistory,
    colors: com.ionut.quizapp.ui.theme.QuizColors,
    isUtm: Boolean
){

    val errorColor = Color(0xFFB33939)
    val successColor = Color(0xFF218C74)

    val naturalSurface = if (isUtm) Color(0xFFFDF7FF) else colors.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = naturalSurface),
        shape = RoundedCornerShape(20.dp), // Colțuri mai rotunjite pentru un look modern
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Întrebarea: Folosim un gri foarte închis, nu negru
            Text(
                text = history.question.question_text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C2C2E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Răspunsul tău
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (history.isCorrect) successColor.copy(alpha = 0.1f) else errorColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (history.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (history.isCorrect) successColor else errorColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = history.selectedAnswer,
                    color = if (history.isCorrect) successColor else errorColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Răspunsul corect (afișat doar la eroare)
            if (!history.isCorrect) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = "Răspuns corect:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = history.question.correct_answer,
                        color = successColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}