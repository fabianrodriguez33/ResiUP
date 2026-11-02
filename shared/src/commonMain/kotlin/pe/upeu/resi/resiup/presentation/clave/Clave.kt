package pe.upeu.resi.resiup.presentation.clave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.repository.AuthRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.aErrorApi

data class CambiarClaveUi(
    val actual: String = "",
    val nueva: String = "",
    val repetir: String = "",
    val errores: Map<String, String> = emptyMap(),
    val error: ErrorApi? = null,
    val trabajando: Boolean = false,
    val listo: Boolean = false,
)

/** Reglas de la contraseña nueva, comprobadas antes de llamar al servidor. */
fun validarClaveNueva(actual: String, nueva: String, repetir: String): Map<String, String> = buildMap {
    if (actual.isBlank()) put("actual", "Escribe tu contraseña actual")
    if (nueva.length < 8) put("nueva", "Debe tener al menos 8 caracteres")
    else if (nueva == actual) put("nueva", "Debe ser distinta de la actual")
    if (repetir != nueva) put("repetir", "No coincide con la nueva contraseña")
}

class CambiarClaveViewModel(private val repo: AuthRepository, private val gestor: GestorSesion) : ViewModel() {
    private val _estado = MutableStateFlow(CambiarClaveUi())
    val estado: StateFlow<CambiarClaveUi> = _estado.asStateFlow()

    fun actual(v: String) = _estado.update { it.copy(actual = v.take(100), errores = it.errores - "actual", error = null) }
    fun nueva(v: String) = _estado.update { it.copy(nueva = v.take(72), errores = it.errores - "nueva", error = null) }
    fun repetir(v: String) = _estado.update { it.copy(repetir = v.take(72), errores = it.errores - "repetir", error = null) }

    fun guardar() {
        val s = _estado.value
        if (s.trabajando) return
        val errores = validarClaveNueva(s.actual, s.nueva, s.repetir)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        _estado.update { it.copy(trabajando = true, error = null, errores = emptyMap()) }
        viewModelScope.launch {
            repo.cambiarClave(s.actual, s.nueva)
                .onSuccess {
                    gestor.claveCambiada()
                    _estado.value = CambiarClaveUi(listo = true)
                }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _estado.update {
                        if (err is ErrorApi.Validacion) it.copy(trabajando = false, errores = err.porCampo.mapKeys { (k, _) -> if (k == "claveActual") "actual" else if (k == "claveNueva") "nueva" else k })
                        else it.copy(trabajando = false, error = err)
                    }
                }
        }
    }
}

@Composable
private fun CamposClave(vm: CambiarClaveViewModel, s: CambiarClaveUi, etiquetaActual: String) {
    ResiCampoTexto(s.actual, vm::actual, etiquetaActual, error = s.errores["actual"], esClave = true, habilitado = !s.trabajando)
    ResiCampoTexto(s.nueva, vm::nueva, "Contraseña nueva", error = s.errores["nueva"], ayuda = "Mínimo 8 caracteres", esClave = true, habilitado = !s.trabajando)
    ResiCampoTexto(s.repetir, vm::repetir, "Repite la contraseña nueva", error = s.errores["repetir"], esClave = true, habilitado = !s.trabajando)
    s.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
}

/** Pantalla obligatoria tras entrar con una contraseña provisional: no se puede seguir sin elegir una nueva. */
@Composable
fun CambiarClaveForzadaScreen(onCerrarSesion: () -> Unit, vm: CambiarClaveViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(Espacio.l),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        TituloPantalla("Elige tu contraseña", subtitulo = "Entraste con una contraseña provisional. Escribe la provisional como contraseña actual y elige una nueva.")
        CamposClave(vm, s, "Contraseña provisional")
        ResiBoton("Guardar contraseña", vm::guardar, Modifier.fillMaxWidth(), cargando = s.trabajando)
        ResiBoton("Cerrar sesión", onCerrarSesion, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !s.trabajando)
    }
}

/** Cambio de contraseña desde el perfil, con la sesión iniciada. */
@Composable
fun CambiarClaveDialogo(onCerrar: () -> Unit, vm: CambiarClaveViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    LaunchedEffect(s.listo) { if (s.listo) onCerrar() }
    AlertDialog(
        onDismissRequest = { if (!s.trabajando) onCerrar() },
        title = { Text("Cambiar contraseña") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                CamposClave(vm, s, "Contraseña actual")
            }
        },
        confirmButton = { TextButton(vm::guardar, enabled = !s.trabajando) { Text(if (s.trabajando) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(onCerrar, enabled = !s.trabajando) { Text("Cancelar") } },
    )
}

data class OlvideClaveUi(val usuario: String = "", val enviando: Boolean = false, val enviado: Boolean = false, val error: ErrorApi? = null)

class OlvideClaveViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow(OlvideClaveUi())
    val estado: StateFlow<OlvideClaveUi> = _estado.asStateFlow()

    fun usuario(v: String) = _estado.update { it.copy(usuario = v.take(50), error = null) }

    fun enviar() {
        val s = _estado.value
        if (s.enviando || s.usuario.isBlank()) return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            repo.olvideClave(s.usuario)
                .onSuccess { _estado.update { it.copy(enviando = false, enviado = true) } }
                .onFailure { e -> _estado.update { it.copy(enviando = false, error = e.aErrorApi()) } }
        }
    }
}

@Composable
fun OlvideClaveDialogo(onCerrar: () -> Unit, vm: OlvideClaveViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Olvidé mi contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                if (s.enviado) {
                    BannerMensaje(
                        "Si tu cuenta tiene un correo verificado, te enviamos una contraseña provisional. Vale 30 minutos: entra con ella y elige una nueva. Revisa también el spam.",
                        EstadoVisual.Exito,
                    )
                } else {
                    Text("Escribe tu usuario. Te enviaremos una contraseña provisional al correo verificado de tu cuenta.", style = MaterialTheme.typography.bodyMedium)
                    ResiCampoTexto(s.usuario, vm::usuario, "Usuario o código", habilitado = !s.enviando)
                    s.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
                }
            }
        },
        confirmButton = {
            if (s.enviado) TextButton(onCerrar) { Text("Entendido") }
            else TextButton(vm::enviar, enabled = !s.enviando && s.usuario.isNotBlank()) { Text(if (s.enviando) "Enviando…" else "Enviar") }
        },
        dismissButton = { if (!s.enviado) TextButton(onCerrar) { Text("Cancelar") } },
    )
}
