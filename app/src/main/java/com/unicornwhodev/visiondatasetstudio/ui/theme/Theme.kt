package com.unicornwhodev.visiondatasetstudio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val StudioGraphite = Color(0xFF252D2C)
val StudioPorcelain = Color(0xFFF5F2EC)
val StudioVermilion = Color(0xFFD85836)
private val LightStudio = lightColorScheme(
    primary = Color(0xFFB94222), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D7), onPrimaryContainer = Color(0xFF71240D),
    secondary = Color(0xFF465A50), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE6DE), onSecondaryContainer = Color(0xFF243B2D),
    tertiary = Color(0xFF765B20), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E5BF), onTertiaryContainer = Color(0xFF493704),
    background = StudioPorcelain, onBackground = StudioGraphite,
    surface = Color(0xFFFCFAF6), onSurface = StudioGraphite,
    surfaceVariant = Color(0xFFE8E5DF), onSurfaceVariant = Color(0xFF5C625E),
    surfaceContainerLowest = Color(0xFFEDEAE4), surfaceContainerLow = Color(0xFFF0EDE7),
    surfaceContainer = Color(0xFFECE8E1), surfaceContainerHigh = Color(0xFFE4E0D8),
    surfaceContainerHighest = Color(0xFFDCD8D0),
    outline = Color(0xFF7B807B), outlineVariant = Color(0xFFD5D3CC),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFFDAD5), onErrorContainer = Color(0xFF5F100A)
)
private val DarkStudio = darkColorScheme(
    primary = Color(0xFFFFA180), onPrimary = Color(0xFF51200C),
    primaryContainer = Color(0xFF723323), onPrimaryContainer = Color(0xFFFFDACE),
    secondary = Color(0xFFB5C9BA), onSecondary = Color(0xFF23392C),
    secondaryContainer = Color(0xFF374D3F), onSecondaryContainer = Color(0xFFD9E9DB),
    tertiary = Color(0xFFE3CB91), onTertiary = Color(0xFF403312),
    tertiaryContainer = Color(0xFF56451D), onTertiaryContainer = Color(0xFFF7E3B1),
    background = Color(0xFF171D1C), onBackground = Color(0xFFF0EFE7),
    surface = Color(0xFF202726), onSurface = Color(0xFFF0EFE7),
    surfaceVariant = Color(0xFF343C39), onSurfaceVariant = Color(0xFFBBC3BC),
    surfaceContainerLowest = Color(0xFF131918), surfaceContainerLow = Color(0xFF1C2321),
    surfaceContainer = StudioGraphite, surfaceContainerHigh = Color(0xFF303A36),
    surfaceContainerHighest = Color(0xFF3C4641),
    outline = Color(0xFF8D988F), outlineVariant = Color(0xFF424D47),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF6C2823), onErrorContainer = Color(0xFFFFDAD5)
)

@Composable
fun VisionDatasetStudioTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkStudio else LightStudio, typography = Typography,
        shapes = Shapes(extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp)),
        content = content)
}
