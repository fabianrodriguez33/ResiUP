package pe.upeu.resi.resiup.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio

data class ItemNavegacion(val id: String, val etiqueta: String, val icono: ImageVector)

private val AnchoPorDestino = 72.dp
private val AltoDestino = 52.dp
private val FormaBarra = RoundedCornerShape(30.dp)

/**
 * Barra inferior flotante solo de íconos. El destino activo se marca con una píldora de color de marca detrás de su ícono;
 * los demás son íconos discretos. No muestra textos: cada destino conserva su nombre para los lectores de pantalla.
 * El ancho se ajusta a la cantidad de destinos (2 a 5) sin desbordar la pantalla.
 */
@Composable
fun BarraNavegacionResi(
    items: List<ItemNavegacion>,
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.s), contentAlignment = Alignment.Center) {
        val relleno = 6.dp
        val ancho = minOf(maxWidth, AnchoPorDestino * items.size + relleno * 2)
        Row(
            Modifier
                .width(ancho)
                .shadow(10.dp, FormaBarra, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                .background(MaterialTheme.colorScheme.surface, FormaBarra)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, FormaBarra)
                .padding(relleno),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val activo = item.id == seleccionado
                val fondo = animateColorAsState(if (activo) MaterialTheme.colorScheme.primary else Color.Transparent)
                val tinta = animateColorAsState(if (activo) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier
                        .weight(1f)
                        .height(AltoDestino)
                        .semantics(mergeDescendants = true) { selected = activo; contentDescription = item.etiqueta }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { onSeleccion(item.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = 52.dp, height = 40.dp).clip(RoundedCornerShape(20.dp)).background(fondo.value),
                        contentAlignment = Alignment.Center,
                    ) {
                        ResiIcono(item.icono, null, tint = tinta.value, tamano = 24.dp)
                    }
                }
            }
        }
    }
}
