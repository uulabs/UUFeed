package com.lunchy.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lunchy.R

private val Chili = Color(0xFF9C3418)
private val ChiliDark = Color(0xFF5C1C0C)
private val Cream = Color(0xFFF6F0E6)
private val Paper = Color(0xFFFFFBF6)
private val Ink = Color(0xFF1E1914)
private val Sage = Color(0xFF1F6B4A)
private val Saffron = Color(0xFFC47B2B)
private val CreamDeep = Color(0xFFE7DCCB)

private val LightColors = lightColorScheme(
    primary = Chili,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6D7CC),
    onPrimaryContainer = ChiliDark,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EADF),
    onSecondaryContainer = Color(0xFF0E3A28),
    tertiary = Saffron,
    onTertiary = Color(0xFF2A1800),
    tertiaryContainer = Color(0xFFF8E3C4),
    onTertiaryContainer = Color(0xFF3D2604),
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = Color(0xFF51473E),
    outline = Color(0xFFD2C4B2),
    error = Color(0xFF9B2335),
    onError = Color.White,
    errorContainer = Color(0xFFF8D5DB),
    onErrorContainer = Color(0xFF4A0C16),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0A48C),
    onPrimary = Color(0xFF3F140A),
    primaryContainer = Color(0xFF6E2A18),
    onPrimaryContainer = Color(0xFFFFD8CC),
    secondary = Color(0xFF8FCBB0),
    onSecondary = Color(0xFF083224),
    secondaryContainer = Color(0xFF1C4A36),
    onSecondaryContainer = Color(0xFFD5EADF),
    tertiary = Color(0xFFE6B36A),
    onTertiary = Color(0xFF3A2504),
    tertiaryContainer = Color(0xFF5A3E14),
    onTertiaryContainer = Color(0xFFF8E3C4),
    background = Color(0xFF14110E),
    onBackground = Color(0xFFF6F0E6),
    surface = Color(0xFF1F1A16),
    onSurface = Color(0xFFF6F0E6),
    surfaceVariant = Color(0xFF3A322B),
    onSurfaceVariant = Color(0xFFD9CBBA),
    outline = Color(0xFF6E6256),
    error = Color(0xFFFFB3BE),
    onError = Color(0xFF4A0C16),
    errorContainer = Color(0xFF6E2230),
    onErrorContainer = Color(0xFFF8D5DB),
)

private val Fraunces = FontFamily(
    Font(R.font.fraunces, FontWeight.Normal),
    Font(R.font.fraunces, FontWeight.Medium),
    Font(R.font.fraunces, FontWeight.SemiBold),
    Font(R.font.fraunces, FontWeight.Bold),
)

private val Outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
)

private val LunchTypography = Typography(
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val LunchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun LunchyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = LunchTypography,
        shapes = LunchShapes,
        content = content,
    )
}

@Composable
fun LunchMark(modifier: Modifier = Modifier) {
    val plate = MaterialTheme.colorScheme.primary
    val well = MaterialTheme.colorScheme.surface
    val steam = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        drawCircle(color = plate, radius = radius)
        drawCircle(color = well, radius = radius * 0.58f)
        val startX = size.width * 0.38f
        val midX = size.width * 0.5f
        val endX = size.width * 0.62f
        val top = size.height * 0.22f
        listOf(startX, midX, endX).forEachIndexed { index, x ->
            drawLine(
                color = steam,
                start = Offset(x, size.height * 0.34f),
                end = Offset(x, top + index * 2f),
                strokeWidth = size.minDimension * 0.045f,
                cap = StrokeCap.Round,
            )
        }
    }
}
