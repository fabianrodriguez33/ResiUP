package pe.upeu.resi.resiup.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio

data class ItemNavegacion(val id: String, val etiqueta: String, val icono: ImageVector)

/** Barra inferior flotante con efecto vidrio. */
@Composable
fun BarraNavegacionResi(
    items: List<ItemNavegacion>,
    seleccionado: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TarjetaVidrio(modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.s), forma = RoundedCornerShape(28.dp)) {
        Row(Modifier.fillMaxWidth().padding(Espacio.xs), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val activo = item.id == seleccionado
                val fondo = animateColorAsState(if (activo) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                val color = if (activo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .background(fondo.value, RoundedCornerShape(22.dp))
                        .semantics { selected = activo }
                        .clickable(role = Role.Tab) { onSeleccion(item.id) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ResiIcono(item.icono, null, tint = color, tamano = 22.dp)
                    Text(item.etiqueta, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
                }
            }
        }
    }
}
