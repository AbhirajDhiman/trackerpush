package com.fitness.pushuptracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Premium Color Scheme
 * Dark mode optimized for fitness tracking
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF99),        // Neon Green
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF00FF99).copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFF00FF99),
    
    secondary = Color(0xFF00E5FF),      // Cyber Cyan
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF00E5FF).copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF00E5FF),
    
    tertiary = Color(0xFFFF00FF),       // Hot Pink
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFFF00FF).copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFFF00FF),
    
    error = Color(0xFFFF2A68),          // Neon Red
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFF2A68).copy(alpha = 0.2f),
    onErrorContainer = Color(0xFFFF2A68),
    
    background = Color(0xFF050505),     // Ultra Dark
    onBackground = Color(0xFFFFFFFF),
    
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF222222)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00C853),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF002200),
    
    secondary = Color(0xFF0091EA),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB3E5FC),
    onSecondaryContainer = Color(0xFF001F2A),
    
    tertiary = Color(0xFFFF3D00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFCCBC),
    onTertiaryContainer = Color(0xFF2A0000),
    
    error = Color(0xFFD50000),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF2A0000),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

/**
 * Premium Typography
 * Using system fonts with optimized weights
 */
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)

/**
 * Main App Theme
 * Premium Material3 design with dark mode support
 */
@Composable
fun PushupTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
