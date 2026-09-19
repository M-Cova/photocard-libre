package org.photocardlibre.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val PhotoCardBlue = Color(0xFF1769C2)
private val PhotoCardBlueDark = Color(0xFF08265C)
private val PhotoCardBlueLight = Color(0xFFDDEBFF)
private val PhotoCardGreen = Color(0xFF2E7D4F)
private val PhotoCardGreenDark = Color(0xFF125B32)
private val PhotoCardGreenLight = Color(0xFFE2F4E9)
private val PhotoCardBackground = Color(0xFFF7FAFD)
private val PhotoCardSurface = Color(0xFFFFFFFF)
private val PhotoCardSurfaceSoft = Color(0xFFF1F6FB)
private val PhotoCardText = Color(0xFF152238)
private val PhotoCardTextSecondary = Color(0xFF526176)
private val PhotoCardOutline = Color(0xFF9BB7D8)
private val PhotoCardError = Color(0xFFBA1A1A)

private val PhotoCardColorScheme = lightColorScheme(
    primary = PhotoCardBlue,
    onPrimary = Color.White,
    primaryContainer = PhotoCardBlueLight,
    onPrimaryContainer = PhotoCardBlueDark,
    secondary = PhotoCardGreen,
    onSecondary = Color.White,
    secondaryContainer = PhotoCardGreenLight,
    onSecondaryContainer = PhotoCardGreenDark,
    background = PhotoCardBackground,
    onBackground = PhotoCardText,
    surface = PhotoCardSurface,
    onSurface = PhotoCardText,
    surfaceVariant = PhotoCardSurfaceSoft,
    onSurfaceVariant = PhotoCardTextSecondary,
    outline = PhotoCardOutline,
    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,
    error = PhotoCardError,
    onError = Color.White,
)

private val PhotoCardTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

private val PhotoCardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun PhotoCardLibreTheme(content: @Composable () -> Unit) {
    // PhotoCard Libre uses one high-contrast light identity, matching its print workflow.
    MaterialTheme(
        colorScheme = PhotoCardColorScheme,
        typography = PhotoCardTypography,
        shapes = PhotoCardShapes,
        content = content,
    )
}
