package com.kivopass.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val KBg = Color(0xFF121214)
val KCard = Color(0xFF18181C)
val KBorder = Color(0xFF2C2C33)
val KAccent = Color(0xFF8C7DF7)
val KDim = Color(0xFF9B9BA6)
val KText = Color(0xFFF2F2F5)
val KDanger = Color(0xFFFF6B6B)

val LocalLang = compositionLocalOf { "es" }

fun tx(lang: String, es: String, en: String) = if (lang == "es") es else en

@Composable
fun tr(es: String, en: String): String = tx(LocalLang.current, es, en)

@Composable
fun KivoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = KAccent,
            onPrimary = Color(0xFF14102E),
            background = KBg,
            onBackground = KText,
            surface = KBg,
            onSurface = KText,
            surfaceVariant = KCard,
            onSurfaceVariant = KDim,
            outline = KBorder,
            secondaryContainer = Color(0xFF2A2650),
            onSecondaryContainer = KText,
            error = KDanger
        ),
        content = content
    )
}
