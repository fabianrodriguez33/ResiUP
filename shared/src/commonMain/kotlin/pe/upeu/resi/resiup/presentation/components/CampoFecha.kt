package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.aFechaIso
import pe.upeu.resi.resiup.presentation.util.fechaLegible

/** Botón que abre el selector de fecha y entrega «AAAA-MM-DD». [valorIso] vacío = todavía sin elegir. */
@Composable
fun CampoFecha(
    etiqueta: String,
    valorIso: String,
    onElegida: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    habilitado: Boolean = true,
    anios: IntRange? = null,
    mesInicialMillis: Long? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier) {
        ResiBoton(
            texto = "$etiqueta: ${if (valorIso.isBlank()) "elegir fecha" else valorIso.fechaLegible()}",
            onClick = { abierto = true },
            modifier = Modifier.fillMaxWidth(),
            variante = VarianteBoton.Secundario,
            habilitado = habilitado,
        )
        if (error != null) Text(error, Modifier.padding(start = Espacio.l, top = Espacio.xs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
    if (abierto) SelectorFecha({ onElegida(it.aFechaIso()); abierto = false }, { abierto = false }, anios, mesInicialMillis)
}
