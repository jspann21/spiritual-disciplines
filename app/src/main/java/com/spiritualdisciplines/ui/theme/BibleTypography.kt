package com.spiritualdisciplines.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.spiritualdisciplines.R

/** Font family used exclusively for the words of Scripture. */
val Literata = FontFamily(Font(R.font.literata))
val NotoSerif = FontFamily(Font(R.font.noto_serif))
val Merriweather = FontFamily(Font(R.font.merriweather))
val AtkinsonHyperlegible = FontFamily(Font(R.font.atkinson_hyperlegible))

val LocalBibleFontFamily = staticCompositionLocalOf { Literata }

fun bibleFontFamily(preference: String): FontFamily = when (preference) {
    "noto_serif" -> NotoSerif
    "merriweather" -> Merriweather
    "atkinson_hyperlegible" -> AtkinsonHyperlegible
    else -> Literata
}
