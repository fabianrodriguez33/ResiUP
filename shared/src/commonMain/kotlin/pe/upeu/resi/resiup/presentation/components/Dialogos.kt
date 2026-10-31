package pe.upeu.resi.resiup.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable

@Composable
fun DialogoConfirmacion(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destructivo: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onConfirmar) {
                Text(
                    textoConfirmar,
                    color = if (destructivo) androidx.compose.material3.MaterialTheme.colorScheme.error
                    else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onCancelar) { Text("Cancelar") } },
    )
}

/** Devuelve la fecha elegida como milisegundos UTC a medianoche (convención de Material 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(onFechaElegida: (Long) -> Unit, onCancelar: () -> Unit) {
    val estado = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { estado.selectedDateMillis?.let(onFechaElegida) }, enabled = estado.selectedDateMillis != null) {
                Text("Aceptar")
            }
        },
        dismissButton = { TextButton(onCancelar) { Text("Cancelar") } },
    ) { DatePicker(estado) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorHora(onHoraElegida: (hora: Int, minuto: Int) -> Unit, onCancelar: () -> Unit) {
    val estado = rememberTimePickerState(is24Hour = false)
    AlertDialog(
        onDismissRequest = onCancelar,
        text = { TimePicker(estado) },
        confirmButton = { TextButton({ onHoraElegida(estado.hour, estado.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onCancelar) { Text("Cancelar") } },
    )
}
