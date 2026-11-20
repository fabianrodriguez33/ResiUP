package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio

/**
 * Menú desplegable de los prototipos: campo blanco con la opción elegida (o el texto de ayuda) y una flecha; al tocarlo
 * se abre debajo la lista de opciones separadas por líneas.
 */
@Composable
fun <T> CampoDesplegable(
    ayuda: String,
    opciones: List<T>,
    seleccion: T?,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).sombraSuave(RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = habilitado, role = Role.DropdownList) { abierto = !abierto }
                .semantics { contentDescription = ayuda }
                .padding(horizontal = Espacio.m, vertical = Espacio.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                seleccion?.let(etiqueta) ?: ayuda, style = MaterialTheme.typography.bodyLarge,
                color = if (seleccion == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            ResiIcono(if (abierto) ResiIconos.ChevronArriba else ResiIconos.ChevronAbajo, null, tint = MaterialTheme.colorScheme.onSurface, tamano = 24.dp)
        }
        if (abierto) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp).sombraSuave(RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)) {
                opciones.forEachIndexed { i, o ->
                    Text(
                        etiqueta(o), Modifier.fillMaxWidth().clickable(role = Role.Button) { abierto = false; onSeleccion(o) }.padding(horizontal = Espacio.m, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (i < opciones.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
