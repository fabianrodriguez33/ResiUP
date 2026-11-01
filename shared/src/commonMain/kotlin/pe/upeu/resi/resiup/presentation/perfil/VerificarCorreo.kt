package pe.upeu.resi.resiup.presentation.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.repository.PerfilCuentaRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.aErrorApi

data class VerificacionState(
    val codigoEnviado: Boolean = false,
    val enviando: Boolean = false,
    val codigo: String = "",
    val verificando: Boolean = false,
    val error: ErrorApi? = null,
    val verificado: Boolean = false,
)

/** Pide al servidor un código de 6 dígitos para el correo guardado y lo confirma. Sirve para todos los roles. */
class VerificacionCorreoViewModel(private val repo: PerfilCuentaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(VerificacionState())
    val estado: StateFlow<VerificacionState> = _estado.asStateFlow()

    fun reiniciar() { _estado.value = VerificacionState() }

    fun codigo(v: String) = _estado.update { it.copy(codigo = v.filter { c -> c.isDigit() }.take(6), error = null) }

    fun enviar() {
        if (_estado.value.enviando) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            repo.enviarCodigo()
                .onSuccess { _estado.update { it.copy(enviando = false, codigoEnviado = true, codigo = "") } }
                .onFailure { e -> _estado.update { it.copy(enviando = false, error = e.aErrorApi()) } }
        }
    }

    fun verificar() {
        val s = _estado.value
        if (s.verificando || s.codigo.length != 6) return
        _estado.update { it.copy(verificando = true, error = null) }
        viewModelScope.launch {
            repo.verificarCorreo(s.codigo)
                .onSuccess { _estado.update { it.copy(verificando = false, verificado = true) } }
                .onFailure { e -> _estado.update { it.copy(verificando = false, error = e.aErrorApi()) } }
        }
    }
}

internal fun ErrorApi.textoVisible() = if (this is ErrorApi.Conflicto) mensaje else aMensaje()

@Composable
fun VerificarCorreoDialogo(correo: String, onCerrar: () -> Unit, onVerificado: () -> Unit, vm: VerificacionCorreoViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    LaunchedEffect(Unit) { vm.reiniciar() }
    LaunchedEffect(s.verificado) { if (s.verificado) onVerificado() }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Verificar correo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                if (!s.codigoEnviado) {
                    Text("Te enviaremos un código de 6 dígitos a $correo para confirmar que el correo es tuyo.")
                } else {
                    Text("Escribe el código que enviamos a $correo. Vence en 15 minutos.")
                    ResiCampoTexto(s.codigo, vm::codigo, "Código de 6 dígitos", tipoTeclado = KeyboardType.Number, habilitado = !s.verificando)
                }
                s.error?.let { BannerMensaje(it.textoVisible(), EstadoVisual.Error) }
            }
        },
        confirmButton = {
            if (!s.codigoEnviado) TextButton(vm::enviar, enabled = !s.enviando) { Text(if (s.enviando) "Enviando…" else "Enviar código") }
            else TextButton(vm::verificar, enabled = s.codigo.length == 6 && !s.verificando) { Text(if (s.verificando) "Verificando…" else "Verificar") }
        },
        dismissButton = {
            Row {
                if (s.codigoEnviado) TextButton(vm::enviar, enabled = !s.enviando) { Text("Reenviar") }
                TextButton(onCerrar) { Text("Cancelar") }
            }
        },
    )
}
