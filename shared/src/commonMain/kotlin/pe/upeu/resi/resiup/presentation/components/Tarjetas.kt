package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiEstilos
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

private val FormaTarjeta = RoundedCornerShape(20.dp)

@Composable
fun ResiTarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    val base = modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base,
        shape = FormaTarjeta,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Espacio.l)) { contenido() }
    }
}

/** Superficie translúcida con borde de reflejo. Usar con moderación: tarjetas destacadas, barras y diálogos. */
@Composable
fun TarjetaVidrio(
    modifier: Modifier = Modifier,
    forma: androidx.compose.ui.graphics.Shape = FormaTarjeta,
    contenido: @Composable () -> Unit,
) {
    val colores = ResiTheme.colores
    Box(
        modifier
            .background(colores.vidrio, forma)
            .border(
                1.dp,
                Brush.linearGradient(listOf(colores.vidrioBorde, colores.vidrioBorde.copy(alpha = colores.vidrioBorde.alpha * 0.25f))),
                forma,
            ),
    ) { contenido() }
}

@Composable
fun TarjetaEstadistica(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
) {
    ResiTarjeta(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Espacio.xs))
        Text(valor, style = ResiEstilos.Numero, color = MaterialTheme.colorScheme.onSurface)
        if (detalle != null) {
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ElementoLista(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icono: ImageVector? = null,
    final: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        modifier = if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        if (icono != null) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                ResiIcono(icono, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (final != null) {
            Spacer(Modifier.width(Espacio.s))
            final()
        }
    }
}

/** Tipos visuales de estado compartidos por permisos, asistencia y solicitudes. */
enum class EstadoVisual { Exito, Advertencia, Error, Info, Neutro }

@Composable
fun ChipEstado(texto: String, estado: EstadoVisual, modifier: Modifier = Modifier) {
    val c = ResiTheme.colores
    val e = MaterialTheme.colorScheme
    val (fondo, texto0) = when (estado) {
        EstadoVisual.Exito -> c.exitoContenedor to c.exito
        EstadoVisual.Advertencia -> c.advertenciaContenedor to c.advertencia
        EstadoVisual.Error -> e.errorContainer to e.onErrorContainer
        EstadoVisual.Info -> c.infoContenedor to c.info
        EstadoVisual.Neutro -> e.surfaceVariant to e.onSurfaceVariant
    }
    Surface(modifier, shape = RoundedCornerShape(50), color = fondo) {
        Text(
            texto,
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = texto0,
        )
    }
}

@Composable
fun TituloPantalla(titulo: String, modifier: Modifier = Modifier, subtitulo: String? = null) {
    Column(modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (subtitulo != null) {
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
