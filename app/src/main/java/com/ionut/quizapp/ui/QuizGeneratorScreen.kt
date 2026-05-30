package com.ionut.quizapp.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.data.SupabaseClient
import com.ionut.quizapp.ui.theme.QuizTheme
import com.ionut.quizapp.viewmodels.QuizViewModel
import io.github.jan.supabase.gotrue.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGeneratorScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit,
    onNavigateToGame: () -> Unit
) {
    val colors = QuizTheme.colors
    val context = LocalContext.current

    BackHandler {
        // Presupunând că ai o variabilă care arată dacă testul e gata (adaptează numele dacă e diferit)
        if (viewModel.generateQuizSuccess) {
            viewModel.discardCustomQuizAndExit(onBack) // Îl ștergem și ieșim
        } else {
            onBack() // Dacă doar se juca cu setările și n-a generat nimic, ieșim normal
        }
    }

    // Curățăm stările de eroare/succes când utilizatorul părăsește ecranul
    DisposableEffect(Unit) {
        onDispose { viewModel.resetGenerateQuizState() }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                // 1. Extragem numele fișierului
                val fileName = getFileName(context, selectedUri) ?: "Custom_Quiz.pdf"

                // 2. Extragem datele fișierului (Bytes)
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val fileBytes = inputStream?.readBytes()
                inputStream?.close()

                // 3. Luăm ID-ul utilizatorului logat direct din Supabase
                val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id

                // 4. Pornim magia
                if (fileBytes != null && currentUserId != null) {
                    // Folosim numele fișierului (fără .pdf) ca titlu pentru Quiz
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
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI CUSTOM QUIZ", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.generateQuizSuccess) {
                            // Ștergem testul din DB dacă utilizatorul apasă pe săgeata de pe ecran
                            viewModel.discardCustomQuizAndExit(onBack)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textMain)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.background)
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
            // Iconița principală (diferă în funcție de succes)
            Icon(
                imageVector = if (viewModel.generateQuizSuccess) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                contentDescription = "Upload PDF",
                modifier = Modifier.size(80.dp),
                tint = if (viewModel.generateQuizSuccess) Color(0xFF4CAF50) else colors.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (viewModel.generateQuizSuccess) "Quiz Generated Successfully!" else "Turn any document into an interactive quiz",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (viewModel.generateQuizSuccess) Color(0xFF4CAF50) else colors.textMain
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!viewModel.generateQuizSuccess) {
                Text(
                    text = "Upload a PDF file (max. 5MB) and let the AI generate custom multiple-choice questions from your material.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // SCHIMBARE STĂRI UI
            if (viewModel.isGeneratingQuiz) {
                CircularProgressIndicator(color = colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gemini is analyzing your document...\nThis usually takes 10-15 seconds.",
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    textAlign = TextAlign.Center
                )
            } else if (viewModel.generateQuizSuccess) {
                Button(
                    onClick = {
                        // 1. Încărcăm testul în motorul de joc
                        viewModel.generatedQuizId?.let { quizId ->
                            viewModel.startCustomQuizMode(quizId)
                        }
                        // 2. Navigăm mai departe
                        onNavigateToGame()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("START GENERATED QUIZ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            } else {
                // Afișăm eroarea primită de la AI (Anti-Troll), dacă există
                viewModel.generateQuizError?.let { errorMsg ->
                    Surface(
                        color = Color(0xFFFFEBEE), // Fundal roșu deschis
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "⚠️ $errorMsg",
                            color = Color(0xFFD32F2F), // Roșu închis
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Button(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = if (viewModel.generateQuizError != null) "TRY ANOTHER PDF" else "SELECT PDF DOCUMENT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Funcție utilitară la finalul fișierului pentru a citi numele documentului selectat
private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}