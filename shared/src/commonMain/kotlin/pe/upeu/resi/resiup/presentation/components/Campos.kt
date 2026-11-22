package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ResiCampoTexto(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    esClave: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onValorCambia,
        modifier = modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        isError = error != null,
        enabled = habilitado,
        singleLine = unaLinea,
        shape = MaterialTheme.shapes.medium,
        // Campo plano: relleno suave y sin borde hasta que recibe el foco.
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = if (esClave) KeyboardType.Password else tipoTeclado),
        visualTransformation = if (esClave && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (esClave) {
            {
                androidx.compose.material3.IconButton(onClick = { visible = !visible }) {
                    ResiIcono(if (visible) ResiIconos.OjoTachado else ResiIconos.Ojo, if (visible) "Ocultar contraseña" else "Mostrar contraseña", tint = pe.upeu.resi.resiup.presentation.theme.Acento.Azul.color(), tamano = 26.dp)
                }
            }
        } else null,
        supportingText = (error ?: ayuda)?.let { texto -> { Text(texto) } },
    )
}
