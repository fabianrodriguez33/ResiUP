package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.PerfilEditable
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Sincronizador
import pe.upeu.resi.resiup.presentation.util.aErrorApi

data class EditarPerfilState(
    val iniciado: Boolean = false,
    val email: String = "",
    val telefono: String = "",
    val codigo: String = "",
    val carrera: String = "",
    val ciclo: String = "",
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val error: ErrorApi? = null,
    val guardado: Boolean = false,
)

/** Edita correo, teléfono, código universitario (9 dígitos), carrera y ciclo. Requiere conexión: es una operación de escritura. */
class EditarPerfilViewModel(private val repo: ResidenteRepository, private val sync: Sincronizador) : ViewModel() {
    private val _estado = MutableStateFlow(EditarPerfilState())
    val estado: StateFlow<EditarPerfilState> = _estado.asStateFlow()

    /** Carga los valores actuales una sola vez por pantalla. */
    fun iniciar(p: Perfil) = _estado.update {
        if (it.iniciado) it
        else EditarPerfilState(iniciado = true, email = p.email.orEmpty(), telefono = p.telefono.orEmpty(), codigo = p.codigo, carrera = p.carrera.orEmpty(), ciclo = p.ciclo?.toString().orEmpty())
    }

    /** Vuelve a pedir los valores al abrir la pantalla de nuevo. */
    fun reiniciar() { _estado.value = EditarPerfilState() }

    fun email(v: String) = _estado.update { it.copy(email = v.take(150), errores = it.errores - "email", error = null) }
    fun telefono(v: String) = _estado.update { it.copy(telefono = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(20), errores = it.errores - "telefono", error = null) }
    fun codigo(v: String) = _estado.update { it.copy(codigo = v.filter { c -> c.isDigit() }.take(9), errores = it.errores - "codigo", error = null) }
    fun carrera(v: String) = _estado.update { it.copy(carrera = v.take(100), errores = it.errores - "carrera", error = null) }
    fun ciclo(v: String) = _estado.update { it.copy(ciclo = v.filter { c -> c.isDigit() }.take(2), errores = it.errores - "ciclo", error = null) }

    fun guardar() {
        val s = _estado.value
        if (s.guardando) return
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        _estado.update { it.copy(guardando = true, error = null, errores = emptyMap()) }
        viewModelScope.launch {
            repo.actualizarPerfil(PerfilEditable(s.email, s.telefono, s.codigo, s.carrera, s.ciclo.toIntOrNull()))
                .onSuccess { sync.refrescar(forzar = true); _estado.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _estado.update {
                        if (err is ErrorApi.Validacion) it.copy(guardando = false, errores = err.porCampo)
                        else it.copy(guardando = false, error = err)
                    }
                }
        }
    }

    private fun validar(s: EditarPerfilState): Map<String, String> = buildMap {
        if (s.email.isNotBlank() && !s.email.trim().matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) put("email", "Escribe un correo válido, por ejemplo nombre@upeu.edu.pe")
        if (s.telefono.isNotBlank() && s.telefono.count { it.isDigit() } < 6) put("telefono", "Escribe un teléfono válido")
        if (s.codigo.length != 9) put("codigo", "El código universitario tiene 9 dígitos, por ejemplo 202410354")
        val ciclo = s.ciclo.toIntOrNull()
        if (s.ciclo.isNotBlank() && (ciclo == null || ciclo !in 1..14)) put("ciclo", "El ciclo va de 1 a 14")
    }
}

@Composable
fun EditarPerfilScreen(perfil: Perfil, onTerminar: () -> Unit, vm: EditarPerfilViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    LaunchedEffect(perfil.id) { vm.reiniciar(); vm.iniciar(perfil) }
    LaunchedEffect(s.guardado) { if (s.guardado) onTerminar() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Editar perfil", subtitulo = perfil.nombreCompleto)
        Text("Tu nombre completo lo gestiona la administración de la residencia.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ResiCampoTexto(s.codigo, vm::codigo, "Código universitario", error = s.errores["codigo"], ayuda = "9 dígitos, por ejemplo 202410354", tipoTeclado = KeyboardType.Number, habilitado = !s.guardando)
        ResiCampoTexto(s.email, vm::email, "Correo electrónico", error = s.errores["email"], tipoTeclado = KeyboardType.Email, habilitado = !s.guardando)
        ResiCampoTexto(s.telefono, vm::telefono, "Teléfono", error = s.errores["telefono"], tipoTeclado = KeyboardType.Phone, habilitado = !s.guardando)
        ResiCampoTexto(s.carrera, vm::carrera, "Carrera universitaria", error = s.errores["carrera"], habilitado = !s.guardando)
        ResiCampoTexto(s.ciclo, vm::ciclo, "Ciclo", error = s.errores["ciclo"], ayuda = "De 1 a 14", tipoTeclado = KeyboardType.Number, habilitado = !s.guardando)
        s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else if (it == ErrorApi.SinConexion || it == ErrorApi.TiempoAgotado) "Necesitas conexión para guardar los cambios." else it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Guardar cambios", vm::guardar, Modifier.fillMaxWidth(), cargando = s.guardando)
        ResiBoton("Cancelar", onTerminar, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !s.guardando)
    }
}
