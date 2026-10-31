package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio

enum class VarianteBoton { Primario, Secundario, Terciario, Destructivo }

@Composable
fun ResiBoton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variante: VarianteBoton = VarianteBoton.Primario,
    habilitado: Boolean = true,
    cargando: Boolean = false,
) {
    val activo = habilitado && !cargando
    val tamano = modifier.heightIn(min = 52.dp)
    val forma = MaterialTheme.shapes.medium
    val esquema = MaterialTheme.colorScheme
    val contenido: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = androidx.compose.material3.LocalContentColor.current)
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
    when (variante) {
        VarianteBoton.Primario -> Button(onClick, tamano, activo, shape = forma, content = { contenido() })
        VarianteBoton.Destructivo -> Button(
            onClick, tamano, activo, shape = forma,
            colors = ButtonDefaults.buttonColors(containerColor = esquema.error, contentColor = esquema.onError),
            content = { contenido() },
        )
        VarianteBoton.Secundario -> OutlinedButton(
            onClick, tamano, activo, shape = forma,
            border = BorderStroke(1.dp, if (activo) esquema.primary.copy(alpha = 0.5f) else esquema.outlineVariant),
            content = { contenido() },
        )
        VarianteBoton.Terciario -> TextButton(onClick, tamano, activo, shape = forma, content = { contenido() })
    }
}
