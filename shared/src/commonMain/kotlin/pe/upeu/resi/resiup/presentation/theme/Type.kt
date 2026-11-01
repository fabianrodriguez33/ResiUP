package pe.upeu.resi.resiup.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Familia del sistema hasta que se confirme una tipografía institucional (ver PENDIENTES.md).
private val Familia = FontFamily.Default

internal val ResiTipografia = Typography(
    displaySmall = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.25).sp), // título de pantalla
    titleLarge = TextStyle(fontFamily = Familia, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleMedium = TextStyle(fontFamily = Familia, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp), // subtítulo
    bodyLarge = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp), // texto principal
    bodyMedium = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp), // texto secundario
    labelLarge = TextStyle(fontFamily = Familia, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp), // botones
    labelMedium = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp), // etiquetas y metadatos
    labelSmall = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
)

/** Estilos fuera de la escala Material: cifras destacadas (horas, puntajes, estadísticas). */
object ResiEstilos {
    val Numero = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    val NumeroGrande = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1).sp)
}
