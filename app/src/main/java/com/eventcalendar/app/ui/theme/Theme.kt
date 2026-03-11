package com.eventcalendar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentOrange = Color(0xFFE07A3D)
val AccentOrangeLight = Color(0xFFF5A66D)
val AccentOrangeDark = Color(0xFFC45E22)

val WarmWhite = Color(0xFFFAF8F5)
val WarmGray = Color(0xFFF5F2EE)
val WarmGrayLight = Color(0xFFEDE9E3)

val WarmTextPrimary = Color(0xFF2D2A26)
val WarmTextSecondary = Color(0xFF6B6560)
val WarmTextTertiary = Color(0xFF9A948E)

val DarkWarmBackground = Color(0xFF1A1714)
val DarkWarmSurface = Color(0xFF242019)
val DarkWarmSurfaceVariant = Color(0xFF2E2922)
val DarkWarmCard = Color(0xFF38322A)

val DarkTextPrimary = Color(0xFFF5F2EE)
val DarkTextSecondary = Color(0xFFB5AFA8)
val DarkTextTertiary = Color(0xFF7A756E)

val WarmBorder = Color(0xFFE5E0D8)
val DarkWarmBorder = Color(0xFF3D3830)

val EventColors = listOf(
    Color(0xFFE07A3D),
    Color(0xFF5B8C5A),
    Color(0xFF6B8CAE),
    Color(0xFFC4786B),
    Color(0xFF8B7CB3),
    Color(0xFFD4A259),
    Color(0xFF6BAEAE),
    Color(0xFFAE8B6B)
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeDark,
    onPrimaryContainer = AccentOrangeLight,
    secondary = Color(0xFF6B8CAE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A3A4A),
    onSecondaryContainer = Color(0xFFB5C8D8),
    tertiary = Color(0xFF5B8C5A),
    onTertiary = Color.White,
    background = DarkWarmBackground,
    onBackground = DarkTextPrimary,
    surface = DarkWarmSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkWarmSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkWarmBorder,
    outlineVariant = DarkWarmCard,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeLight,
    onPrimaryContainer = AccentOrangeDark,
    secondary = Color(0xFF6B8CAE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFF5),
    onSecondaryContainer = Color(0xFF3A4A5A),
    tertiary = Color(0xFF5B8C5A),
    onTertiary = Color.White,
    background = WarmWhite,
    onBackground = WarmTextPrimary,
    surface = WarmGray,
    onSurface = WarmTextPrimary,
    surfaceVariant = WarmGrayLight,
    onSurfaceVariant = WarmTextSecondary,
    outline = WarmBorder,
    outlineVariant = Color.White,
)

val Primary = AccentOrange
val PrimaryLight = AccentOrangeLight
val PrimaryDark = AccentOrangeDark
val TextSecondary = WarmTextSecondary
val TextSecondaryLight = DarkTextSecondary

@Composable
fun EventCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
