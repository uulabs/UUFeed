package au.com.uulabs.uufeed.ui.theme

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
import au.com.uulabs.uufeed.R

private val Night = Color(0xFF100E16)
private val NightRaised = Color(0xFF1C1728)
private val Ink = Color(0xFFF4F0FA)
private val InkMuted = Color(0xFFC9BEDD)
private val Violet = Color(0xFF7048E8)
private val VioletDeep = Color(0xFF3A2878)
private val Lilac = Color(0xFFD8CCFF)

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletDeep,
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = Lilac,
    onSecondary = Color(0xFF1C1033),
    secondaryContainer = Violet,
    onSecondaryContainer = Color.White,
    tertiary = Lilac,
    onTertiary = Color(0xFF1C1033),
    tertiaryContainer = Color(0xFF2A2144),
    onTertiaryContainer = Color(0xFFEDE7FF),
    background = Night,
    onBackground = Ink,
    surface = NightRaised,
    onSurface = Ink,
    surfaceVariant = Color(0xFF322A44),
    onSurfaceVariant = InkMuted,
    surfaceTint = Violet,
    outline = Color(0xFF7A7090),
    outlineVariant = Color(0xFF453C58),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Ink,
    inverseOnSurface = Night,
    inversePrimary = VioletDeep,
    surfaceDim = Night,
    surfaceBright = Color(0xFF3A334C),
    surfaceContainerLowest = Color(0xFF0B0910),
    surfaceContainerLow = Color(0xFF161221),
    surfaceContainer = Color(0xFF1C1728),
    surfaceContainerHigh = Color(0xFF261F36),
    surfaceContainerHighest = Color(0xFF322A44),
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

private val FeedTypography = Typography(
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

private val FeedShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun UuFeedTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bars = android.graphics.Color.parseColor("#100E16")
            window.statusBarColor = bars
            window.navigationBarColor = bars
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColors,
        typography = FeedTypography,
        shapes = FeedShapes,
        content = content,
    )
}

@Composable
fun FeedMark(modifier: Modifier = Modifier) {
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
