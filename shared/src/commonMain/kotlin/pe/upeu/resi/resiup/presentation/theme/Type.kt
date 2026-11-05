package pe.upeu.resi.resiup.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font as FuenteRecurso
import resiup.shared.generated.resources.Res
import resiup.shared.generated.resources.prompt_bold
import resiup.shared.generated.resources.prompt_medium
import resiup.shared.generated.resources.prompt_regular
import resiup.shared.generated.resources.prompt_semibold
import resiup.shared.generated.resources.roboto_variable

// Títulos: Prompt (geométrica redondeada, como la app UPeU). Texto: Roboto variable.
@Composable
private fun familiaTitulos(): FontFamily = FontFamily(
    FuenteRecurso(Res.font.prompt_regular, FontWeight.Normal),
    FuenteRecurso(Res.font.prompt_medium, FontWeight.Medium),
    FuenteRecurso(Res.font.prompt_semibold, FontWeight.SemiBold),
    FuenteRecurso(Res.font.prompt_bold, FontWeight.Bold),
)

@Composable
private fun familiaTexto(): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { peso ->
        FuenteRecurso(Res.font.roboto_variable, peso, variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight)))
    },
)

@Composable
internal fun resiTipografia(): Typography {
    val titulos = familiaTitulos()
    val texto = familiaTexto()
    return Typography(
        displaySmall = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.25).sp), // título de pantalla
        titleLarge = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        titleMedium = TextStyle(fontFamily = titulos, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp), // subtítulo
        bodyLarge = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp), // texto principal
        bodyMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp), // texto secundario
        labelLarge = TextStyle(fontFamily = titulos, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp), // botones
        labelMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp), // etiquetas y metadatos
        labelSmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
    )
}

/** Estilos fuera de la escala Material: cifras destacadas (horas, puntajes, estadísticas). */
object ResiEstilos {
    val Numero: TextStyle @Composable get() = TextStyle(fontFamily = familiaTitulos(), fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    val NumeroGrande: TextStyle @Composable get() = TextStyle(fontFamily = familiaTitulos(), fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1).sp)
}
