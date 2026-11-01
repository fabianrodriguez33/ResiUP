package pe.upeu.resi.resiup.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio

data class ItemNavegacion(val id: String, val etiqueta: String, val icono: ImageVector)

/**
 * Barra inferior flotante de vidrio. Minimalista: solo la pestaña activa muestra su texto (en una píldora);
 * las demás son un ícono. Cada ícono conserva su nombre para los lectores de pantalla.
 */
@Composable
fun BarraNavegacionResi(
    items: List<ItemNavegacion>,
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TarjetaVidrio(modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.s), forma = RoundedCornerShape(32.dp)) {
        // Con textos de sistema muy grandes se limita la escala para que la etiqueta activa quepa.
        val densidad = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, minOf(densidad.fontScale, 1.15f))) {
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { item ->
                    val activo = item.id == seleccionado
                    val peso = animateFloatAsState(if (activo) 2.5f else 1f)
                    val fondo = animateColorAsState(if (activo) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                    val color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        Modifier
                            .weight(peso.value)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(fondo.value)
                            .semantics(mergeDescendants = true) { selected = activo; contentDescription = item.etiqueta }
                            .clickable(role = Role.Tab) { onSeleccion(item.id) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResiIcono(item.icono, null, tint = color, tamano = 24.dp)
                        if (activo) {
                            Spacer(Modifier.width(6.dp))
                            Text(item.etiqueta, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1, overflow = TextOverflow.Clip)
                        }
                    }
                }
            }
        }
    }
}
