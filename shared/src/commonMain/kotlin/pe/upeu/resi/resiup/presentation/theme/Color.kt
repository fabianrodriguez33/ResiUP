package pe.upeu.resi.resiup.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Paleta PROPUESTA (no oficial). Reemplazar por los valores del manual de marca UPeU cuando exista.
internal object Paleta {
    val AzulInstitucional = Color(0xFF0A2F6B)
    val AzulSecundario = Color(0xFF2F7BFF)
    val AzulVivo = Color(0xFF1F63E0)

    val LuzPrimarioContenedor = Color(0xFFDDE8FF)
    val LuzSecundarioContenedor = Color(0xFFDCE9FB)
    val LuzFondo = Color(0xFFF1F5FF)
    val LuzSuperficie = Color(0xFFFFFFFF)
    val LuzSuperficieVariante = Color(0xFFEEF1F6)
    val LuzTexto = Color(0xFF111827)
    val LuzTextoSecundario = Color(0xFF4B5563)
    val LuzContorno = Color(0xFF8B94A3)
    val LuzDivisor = Color(0xFFE5E7EB)
    val LuzError = Color(0xFFC62828)
    val LuzErrorContenedor = Color(0xFFFDE4E1)

    val NocheFondo = Color(0xFF0B1220)
    val NocheSuperficie = Color(0xFF121A2B)
    val NocheSuperficieVariante = Color(0xFF1A2438)
    val NocheTexto = Color(0xFFE8ECF4)
    val NocheTextoSecundario = Color(0xFFA9B3C5)
    val NocheContorno = Color(0xFF6B7588)
    val NocheDivisor = Color(0xFF263044)
}

/** Colores semánticos que Material 3 no define: éxito, advertencia, información y vidrio. */
@Immutable
class ResiColores(
    val exito: Color,
    val exitoContenedor: Color,
    val advertencia: Color,
    val advertenciaContenedor: Color,
    val info: Color,
    val infoContenedor: Color,
    val vidrio: Color,
    val vidrioBorde: Color,
    val marca: Color,
    val marcaClara: Color,
    val sobreMarca: Color,
)

internal val ResiColoresClaro = ResiColores(
    exito = Color(0xFF14663C),
    exitoContenedor = Color(0xFFDDF3E6),
    advertencia = Color(0xFF8A5100),
    advertenciaContenedor = Color(0xFFFFF0D1),
    info = Color(0xFF1857A8),
    infoContenedor = Paleta.LuzSecundarioContenedor,
    vidrio = Color.White.copy(alpha = 0.72f),
    vidrioBorde = Color.White.copy(alpha = 0.85f),
    marca = Paleta.AzulInstitucional,
    marcaClara = Paleta.AzulSecundario,
    sobreMarca = Color.White,
)

internal val ResiColoresOscuro = ResiColores(
    exito = Color(0xFF6FD39B),
    exitoContenedor = Color(0xFF16382A),
    advertencia = Color(0xFFF2B84B),
    advertenciaContenedor = Color(0xFF40300F),
    info = Color(0xFF8CB8F7),
    infoContenedor = Color(0xFF1D3F6E),
    vidrio = Paleta.NocheSuperficie.copy(alpha = 0.62f),
    vidrioBorde = Color.White.copy(alpha = 0.14f),
    marca = Paleta.AzulInstitucional,
    marcaClara = Color(0xFF2A5FCB),
    sobreMarca = Color.White,
)

internal val EsquemaClaro: ColorScheme = lightColorScheme(
    primary = Paleta.AzulVivo,
    onPrimary = Color.White,
    primaryContainer = Paleta.LuzPrimarioContenedor,
    onPrimaryContainer = Color(0xFF06214D),
    secondary = Paleta.AzulSecundario,
    onSecondary = Color.White,
    secondaryContainer = Paleta.LuzSecundarioContenedor,
    onSecondaryContainer = Color(0xFF0B3470),
    background = Paleta.LuzFondo,
    onBackground = Paleta.LuzTexto,
    surface = Paleta.LuzSuperficie,
    onSurface = Paleta.LuzTexto,
    surfaceVariant = Paleta.LuzSuperficieVariante,
    onSurfaceVariant = Paleta.LuzTextoSecundario,
    outline = Paleta.LuzContorno,
    outlineVariant = Paleta.LuzDivisor,
    error = Paleta.LuzError,
    onError = Color.White,
    errorContainer = Paleta.LuzErrorContenedor,
    onErrorContainer = Color(0xFF5C0E0A),
)

internal val EsquemaOscuro: ColorScheme = darkColorScheme(
    primary = Color(0xFF8CB4F5),
    onPrimary = Color(0xFF06214D),
    primaryContainer = Color(0xFF16408A),
    onPrimaryContainer = Color(0xFFDCE7FB),
    secondary = Color(0xFF7FB0F5),
    onSecondary = Color(0xFF0A2447),
    secondaryContainer = Color(0xFF1D3F6E),
    onSecondaryContainer = Color(0xFFDCE9FB),
    background = Paleta.NocheFondo,
    onBackground = Paleta.NocheTexto,
    surface = Paleta.NocheSuperficie,
    onSurface = Paleta.NocheTexto,
    surfaceVariant = Paleta.NocheSuperficieVariante,
    onSurfaceVariant = Paleta.NocheTextoSecundario,
    outline = Paleta.NocheContorno,
    outlineVariant = Paleta.NocheDivisor,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF4A1D1A),
    onErrorContainer = Color(0xFFFFDAD6),
)
