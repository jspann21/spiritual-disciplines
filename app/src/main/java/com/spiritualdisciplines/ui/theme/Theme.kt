package com.spiritualdisciplines.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

private const val MinimumUiContrast = 4.5f

private fun contrastRatio(foreground: Color, background: Color): Float {
  val lighter = maxOf(foreground.luminance(), background.luminance())
  val darker = minOf(foreground.luminance(), background.luminance())
  return (lighter + 0.05f) / (darker + 0.05f)
}

private fun readableContentColor(background: Color): Color {
  val darkContrast = contrastRatio(GeoTextPrimary, background)
  val lightContrast = contrastRatio(GeoSurface, background)
  return if (darkContrast >= lightContrast) GeoTextPrimary else GeoSurface
}

private fun contrastSafeAccent(accent: Color, background: Color, target: Color): Color {
  val opaqueAccent = accent.copy(alpha = 1f)
  if (contrastRatio(opaqueAccent, background) >= MinimumUiContrast) return opaqueAccent

  for (step in 1..20) {
    val candidate = lerp(opaqueAccent, target, step / 20f)
    if (contrastRatio(candidate, background) >= MinimumUiContrast) return candidate
  }

  return target
}

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPrimary,
    onPrimary = GeoSurface,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoTextPrimary,
    secondary = GeoSecondary,
    onSecondary = GeoSurface,
    secondaryContainer = GeoPrimaryContainer,
    onSecondaryContainer = GeoTextPrimary,
    tertiary = GeoAccent,
    onTertiary = GeoSurface,
    tertiaryContainer = GeoAccentDark,
    background = GeoBackground,
    onBackground = GeoTextPrimary,
    surface = GeoSurface,
    onSurface = GeoTextPrimary,
    surfaceVariant = GeoPrimaryContainer,
    onSurfaceVariant = GeoTextSecondary,
    outline = GeoOutline,
    outlineVariant = GeoOutline
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoPrimaryDark,
    onPrimary = GeoSurfaceDark,
    primaryContainer = GeoPrimaryContainerDark,
    onPrimaryContainer = GeoTextPrimaryDark,
    secondary = GeoSecondaryDark,
    onSecondary = GeoSurfaceDark,
    secondaryContainer = GeoPrimaryContainerDark,
    onSecondaryContainer = GeoTextPrimaryDark,
    tertiary = GeoAccent,
    onTertiary = GeoSurfaceDark,
    tertiaryContainer = GeoAccentDark,
    background = GeoBackgroundDark,
    onBackground = GeoTextPrimaryDark,
    surface = GeoSurfaceDark,
    onSurface = GeoTextPrimaryDark,
    surfaceVariant = GeoPrimaryContainerDark,
    onSurfaceVariant = GeoTextSecondaryDark,
    outline = GeoOutlineDark,
    outlineVariant = GeoOutlineDark
  )

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  accentColor: Int? = null,
  content: @Composable () -> Unit,
) {
  var baseLight = LightColorScheme
  var baseDark = DarkColorScheme

  if (accentColor != null) {
      val c = Color(accentColor)
      val cPrimaryLight = contrastSafeAccent(c, GeoSurface, GeoTextPrimary)
      val cPrimaryDark = contrastSafeAccent(c, GeoSurfaceDark, GeoSurface)
      val hsv = FloatArray(3)
      android.graphics.Color.colorToHSV(accentColor, hsv)
      
      val hsvContainerLight = hsv.clone()
      hsvContainerLight[1] = hsvContainerLight[1] * 0.2f
      hsvContainerLight[2] = 0.95f
      val cContainerLight = Color(android.graphics.Color.HSVToColor(hsvContainerLight))

      val hsvContainerDark = hsv.clone()
      hsvContainerDark[1] = hsvContainerDark[1] * 0.5f
      hsvContainerDark[2] = hsvContainerDark[2] * 0.2f
      val cContainerDark = Color(android.graphics.Color.HSVToColor(hsvContainerDark))

      baseLight = baseLight.copy(
          primary = cPrimaryLight,
          onPrimary = readableContentColor(cPrimaryLight),
          primaryContainer = cContainerLight,
          onPrimaryContainer = readableContentColor(cContainerLight),
          secondaryContainer = cContainerLight,
          onSecondaryContainer = readableContentColor(cContainerLight),
          surfaceVariant = cContainerLight,
          tertiary = cPrimaryLight,
          onTertiary = readableContentColor(cPrimaryLight),
          tertiaryContainer = cContainerLight,
          onTertiaryContainer = readableContentColor(cContainerLight)
      )
      baseDark = baseDark.copy(
          primary = cPrimaryDark,
          onPrimary = readableContentColor(cPrimaryDark),
          primaryContainer = cContainerDark,
          onPrimaryContainer = readableContentColor(cContainerDark),
          secondaryContainer = cContainerDark,
          onSecondaryContainer = readableContentColor(cContainerDark),
          surfaceVariant = cContainerDark,
          tertiary = cPrimaryDark,
          onTertiary = readableContentColor(cPrimaryDark),
          tertiaryContainer = cContainerDark,
          onTertiaryContainer = readableContentColor(cContainerDark)
      )
  }

  val colorScheme = if (darkTheme) baseDark else baseLight

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    motionScheme = MotionScheme.expressive(),
    content = content
  )
}
