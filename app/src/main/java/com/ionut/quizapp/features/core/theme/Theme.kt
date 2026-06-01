package com.ionut.quizapp.features.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// Structura care grupează culorile noastre
data class QuizColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val textMain: Color,
    val textSecondary: Color,
    val isUtm: Boolean
)

// Definirea temei NORMALE
val NormalPalette = QuizColors(
    primary = PinkLight,      // FF537B
    secondary = PinkDark,     // E13F7C
    accent = YellowDark,      // FFE97D
    background = BackgroundOffWhite,
    surface = AppWhite,
    textMain = TextDark,
    textSecondary = Color(0xFF78909C),
    isUtm = false
)

// Definirea temei UTM
val UtmPalette = QuizColors(
    primary = UtmPurple,      // 9B51E0
    secondary = UtmPurpleDeep, // 673AB7
    accent = YellowLight,     // FFEF9F
    background = UtmPurplePale, // F3E5F5
    surface = AppWhite,
    textMain = TextDark,
    textSecondary = UtmPurpleDeep.copy(alpha = 0.7f),
    isUtm = true
)

// Acces global la culori
val LocalQuizColors = staticCompositionLocalOf { NormalPalette }

object QuizTheme {
    val colors: QuizColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQuizColors.current
}

@Composable
fun QuizAppTheme(
    isUtmMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val customColors = if (isUtmMode) UtmPalette else NormalPalette

    // Configurăm și schema Material3 standard pentru componentele de sistem (Switch, Checkbox)
    val materialScheme = lightColorScheme(
        primary = customColors.primary,
        secondary = customColors.secondary,
        background = customColors.background,
        surface = customColors.surface
    )

    CompositionLocalProvider(LocalQuizColors provides customColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content
        )
    }
}