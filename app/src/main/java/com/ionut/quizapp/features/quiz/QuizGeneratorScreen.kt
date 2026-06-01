package com.ionut.quizapp.features.quiz

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.data.SupabaseClient
import com.ionut.quizapp.features.core.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel
import com.ionut.quizapp.viewmodels.SoundManager
import io.github.jan.supabase.gotrue.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGeneratorScreen(
    viewModel: QuizViewModel,
    soundManager: SoundManager,
    onBack: () -> Unit,
    onNavigateToGame: () -> Unit
) {
    val colors = QuizTheme.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Launcher pentru selectarea documentelor PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            handlePdfSelection(uri, context, viewModel)
        }
    )

    // Gestionare buton Back (Hardware & UI)
    BackHandler {
        handleExitOrDiscard(viewModel, soundManager, haptic, onBack)
    }

    // Resetare stări la părăsirea ecranului
    DisposableEffect(Unit) {
        onDispose { viewModel.resetGenerateQuizState() }
    }

    // Feedback haptic la succes
    LaunchedEffect(viewModel.generateQuizSuccess) {
        if (viewModel.generateQuizSuccess && soundManager.isVibrationEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            GeneratorTopBar(
                isSuccess = viewModel.generateQuizSuccess,
                onBackClick = { handleExitOrDiscard(viewModel, soundManager, haptic, onBack) }
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Partea 1: Ilustrație și Mesaj de Stare
            GeneratorHeader(
                isSuccess = viewModel.generateQuizSuccess,
                isGenerating = viewModel.isGeneratingQuiz
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Partea 2: Controler Dinamic (Loading / Start / Upload)
            GeneratorActionContent(
                viewModel = viewModel,
                onPickerLaunch = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onStartQuiz = {
                    viewModel.generatedQuizId?.let { viewModel.startCustomQuizMode(it) }
                    onNavigateToGame()
                }
            )
        }
    }
}

// ========================== COMPONENTE INTERNE (UI) ==========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorTopBar(isSuccess: Boolean, onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("AI CUSTOM QUIZ", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = QuizTheme.colors.textMain)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun GeneratorHeader(isSuccess: Boolean, isGenerating: Boolean) {
    val colors = QuizTheme.colors

    Icon(
        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.UploadFile,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = if (isSuccess) Color(0xFF4CAF50) else colors.primary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = if (isSuccess) "Quiz Generated Successfully!" else "Turn documents into quizzes",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = if (isSuccess) Color(0xFF4CAF50) else colors.textMain
    )

    if (!isSuccess && !isGenerating) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload a PDF (max. 5MB) and Gemini AI will create custom questions for you.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun GeneratorActionContent(
    viewModel: QuizViewModel,
    onPickerLaunch: () -> Unit,
    onStartQuiz: () -> Unit
) {
    val colors = QuizTheme.colors

    when {
        viewModel.isGeneratingQuiz -> {
            CircularProgressIndicator(color = colors.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gemini is analyzing your document...\nThis usually takes 10-15 seconds.",
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                textAlign = TextAlign.Center
            )
        }
        viewModel.generateQuizSuccess -> {
            Button(
                onClick = onStartQuiz,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("START GENERATED QUIZ", fontWeight = FontWeight.ExtraBold)
            }
        }
        else -> {
            // Afișare eroare dacă există
            viewModel.generateQuizError?.let { errorMsg ->
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "⚠️ $errorMsg",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onPickerLaunch,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (viewModel.generateQuizError != null) "TRY ANOTHER PDF" else "SELECT PDF DOCUMENT",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ========================== LOGICĂ ȘI UTILS ==========================

private fun handlePdfSelection(uri: Uri?, context: Context, viewModel: QuizViewModel) {
    uri?.let { selectedUri ->
        val fileName = getFileName(context, selectedUri) ?: "Custom_Quiz.pdf"
        val inputStream = context.contentResolver.openInputStream(selectedUri)
        val fileBytes = inputStream?.readBytes()
        inputStream?.close()

        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

        if (fileBytes != null && currentUserId != null) {
            val title = fileName.replace(".pdf", "", ignoreCase = true)
            viewModel.generateQuizFromPdf(
                fileBytes = fileBytes,
                originalFileName = fileName,
                userId = currentUserId,
                quizTitle = title
            )
        }
    }
}

private fun handleExitOrDiscard(
    viewModel: QuizViewModel,
    soundManager: SoundManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onExit: () -> Unit
) {
    if (viewModel.generateQuizSuccess) {
        if (soundManager.isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.discardCustomQuizAndExit(onExit)
    } else {
        onExit()
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally { cursor?.close() }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}