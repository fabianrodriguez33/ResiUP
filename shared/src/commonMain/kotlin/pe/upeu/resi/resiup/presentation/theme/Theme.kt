package pe.upeu.resi.resiup.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

private val ResiFormas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val LocalResiColores = staticCompositionLocalOf { ResiColoresClaro }

object ResiTheme {
    val colores: ResiColores
        @Composable @ReadOnlyComposable get() = LocalResiColores.current
}

@Composable
fun ResiTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalResiColores provides if (oscuro) ResiColoresOscuro else ResiColoresClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = ResiTipografia,
            shapes = ResiFormas,
        ) {
            // Los textos sin color explícito (fuera de un Surface) deben seguir el tema, no quedar en negro sobre fondo oscuro.
            val esquema = MaterialTheme.colorScheme
            CompositionLocalProvider(LocalContentColor provides esquema.onBackground, content = content)
        }
    }
}
