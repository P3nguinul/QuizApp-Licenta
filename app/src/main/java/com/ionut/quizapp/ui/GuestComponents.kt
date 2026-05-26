package com.ionut.quizapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionut.quizapp.ui.theme.QuizTheme
import androidx.compose.ui.window.Dialog
@Composable
fun GuestWarningBanner(onLoginClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF3E0), // Un portocaliu/amber deschis de avertizare prietenoasă
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color(0xFFF57C00), // Portocaliu mai închis pentru contrast
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "You are using a Guest account",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    fontSize = 14.sp
                )
                Text(
                    text = "Your progress won't be saved. Log in to track your stats and unlock AI features!",
                    color = Color(0xFFE65100).copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onLoginClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
                Text("LOG IN", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun LoginRequiredDialog(
    featureName: String,
    description: String,
    onDismiss: () -> Unit,
    onGoToLogin: () -> Unit
) {
    // Folosim Dialog simplu pentru a avea control 100% asupra designului (fără marginile urâte din AlertDialog)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp), // Rotunjire modernă maximă
            color = QuizTheme.colors.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Iconiță Premium / Header vizual
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF8E1), // Un galben pal
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium, // Icoană de Premium/Unlock
                            contentDescription = null,
                            tint = Color(0xFFFFB300), // Galben auriu
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Titlu
                Text(
                    text = "Account Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = QuizTheme.colors.textMain,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Mesaj principal
                Text(
                    text = "The $featureName is exclusive to registered members.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = QuizTheme.colors.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Descriere detaliată
                Text(
                    text = description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = QuizTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Butonul Principal (Call to Action)
                Button(
                    onClick = onGoToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QuizTheme.colors.primary)
                ) {
                    Text("CREATE ACCOUNT", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Butonul Secundar (Cancel)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Maybe Later", color = QuizTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}