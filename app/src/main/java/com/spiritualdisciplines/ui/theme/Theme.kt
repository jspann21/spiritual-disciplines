package com.spiritualdisciplines.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  accentColor: Int? = null,
  content: @Composable () -> Unit,
) {
  var baseLight = LightColorScheme
  var baseDark = DarkColorScheme

  if (accentColor != null) {
      val c = Color(accentColor)
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

      val hsvPrimaryDark = hsv.clone()
      hsvPrimaryDark[1] = hsvPrimaryDark[1] * 0.6f
      hsvPrimaryDark[2] = 1f
      val cPrimaryDark = Color(android.graphics.Color.HSVToColor(hsvPrimaryDark))

      baseLight = baseLight.copy(
          primary = c,
          primaryContainer = cContainerLight,
          secondaryContainer = cContainerLight,
          surfaceVariant = cContainerLight,
          tertiary = c, 
          tertiaryContainer = cContainerLight
      )
      baseDark = baseDark.copy(
          primary = cPrimaryDark,
          primaryContainer = cContainerDark,
          secondaryContainer = cContainerDark,
          surfaceVariant = cContainerDark,
          tertiary = cPrimaryDark, 
          tertiaryContainer = cContainerDark
      )
  }

  val colorScheme = if (darkTheme) baseDark else baseLight

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
