package com.atharvakale.facerecognition.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = White,
    primaryContainer = Purple200,
    secondary = Teal200,
    onSecondary = Black,
    secondaryContainer = Teal700,
    surface = White,
    onSurface = Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Black,
    primaryContainer = Purple700,
    secondary = Teal200,
    onSecondary = Black,
    secondaryContainer = Teal700,
    surface = Black,
    onSurface = White,
)

@Composable
fun FaceRecognitionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
