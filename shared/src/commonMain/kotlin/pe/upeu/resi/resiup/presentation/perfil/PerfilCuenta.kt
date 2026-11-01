package pe.upeu.resi.resiup.presentation.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.ContactoEditable
import pe.upeu.resi.resiup.domain.model.PerfilCuenta
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.repository.PerfilCuentaRepository
import pe.upeu.resi.resiup.presentation.components.AvatarInicial
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CampoFecha
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.personal.DiasSinCultoScreen
import pe.upeu.resi.resiup.presentation.residente.DatoPerfil
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.hoyLima
import pe.upeu.resi.resiup.presentation.util.validarContacto

data class ContactoForm(
    val email: String,
    val telefono: String,
    val fechaNacimiento: String,
    val errores: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val error: ErrorApi? = null,
)

/** Datos personales de la cuenta de cualquier rol que no sea residente. Todo se consulta y se guarda en línea. */
class PerfilCuentaViewModel(private val repo: PerfilCuentaRepository) : ViewModel() {
    private val _perfil = MutableStateFlow<Carga<PerfilCuenta>>(Carga.Cargando)
    val perfil: StateFlow<Carga<PerfilCuenta>> = _perfil.asStateFlow()

    /** null = no se está editando. */
    private val _form = MutableStateFlow<ContactoForm?>(null)
    val form: StateFlow<ContactoForm?> = _form.asStateFlow()

    fun cargar() {
        _perfil.value = Carga.Cargando
        _form.value = null
        viewModelScope.launch { _perfil.value = repo.perfil().aCarga() }
    }

    /** Actualiza los datos en segundo plano, sin volver a mostrar «cargando» (por ejemplo, tras verificar el correo). */
    fun refrescar() {
        viewModelScope.launch { repo.perfil().onSuccess { _perfil.value = Carga.Exito(it) } }
    }

    fun editar(p: PerfilCuenta) { _form.value = ContactoForm(p.email.orEmpty(), p.telefono.orEmpty(), p.fechaNacimiento.orEmpty()) }
    fun cancelar() { _form.value = null }

    fun email(v: String) = _form.update { it?.copy(email = v.take(150), errores = it.errores - "email", error = null) }
    fun telefono(v: String) = _form.update { it?.copy(telefono = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(20), errores = it.errores - "telefono", error = null) }
    fun fechaNacimiento(v: String) = _form.update { it?.copy(fechaNacimiento = v, errores = it.errores - "fechaNacimiento", error = null) }

    fun guardar() {
        val f = _form.value ?: return
        if (f.guardando) return
        val errores = validarContacto(f.email, f.telefono, f.fechaNacimiento, hoyLima())
        if (errores.isNotEmpty()) {
            _form.value = f.copy(errores = errores)
            return
        }
        _form.value = f.copy(guardando = true, error = null, errores = emptyMap())
        viewModelScope.launch {
            repo.actualizar(ContactoEditable(f.email, f.telefono, f.fechaNacimiento.takeIf { it.isNotBlank() }))
                .onSuccess { _perfil.value = Carga.Exito(it); _form.value = null }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _form.update {
                        if (err is ErrorApi.Validacion) it?.copy(guardando = false, errores = err.porCampo)
                        else it?.copy(guardando = false, error = err)
                    }
                }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PerfilCuentaScreen(rol: Rol, onCerrarSesion: () -> Unit, vm: PerfilCuentaViewModel = koinViewModel()) {
    val estado by vm.perfil.collectAsState()
    val form by vm.form.collectAsState()
    var confirmar by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(false) }
    var verDias by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.cargar() }
    BackHandler(enabled = verDias || form != null) { if (verDias) verDias = false else vm.cancelar() }

    if (verDias) {
        DiasSinCultoScreen(onVolver = { verDias = false })
        return
    }
    ContenidoCarga(estado, vm::cargar) { p ->
        val f = form
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            if (f == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    AvatarInicial(p.nombreCompleto, Acento.Azul, tamano = 60.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.nombreCompleto, style = MaterialTheme.typography.headlineMedium)
                        Text("Usuario ${p.usuario}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ChipEstado(rol.etiqueta, EstadoVisual.Info, Modifier.padding(top = Espacio.xs))
                    }
                }
                ResiTarjeta {
                    DatoPerfil("DNI", p.dni, ResiIconos.Perfil, Acento.Azul)
                    DatoPerfil("Fecha de nacimiento", p.fechaNacimiento?.fechaLegible(), ResiIconos.Calendario, Acento.Rosa)
                    DatoPerfil(
                        "Correo electrónico", p.email, ResiIconos.Correo, Acento.Violeta,
                        final = if (p.email.isNullOrBlank()) null else { { ChipEstado(if (p.emailVerificado) "Verificado" else "Sin verificar", if (p.emailVerificado) EstadoVisual.Exito else EstadoVisual.Advertencia) } },
                    )
                    DatoPerfil("Teléfono", p.telefono, ResiIconos.Telefono, Acento.Verde)
                }
                if (!p.email.isNullOrBlank() && !p.emailVerificado) {
                    ResiBoton("Verificar mi correo", { verificando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                }
                ResiBoton("Editar mis datos", { vm.editar(p) }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                Text(
                    "Tu nombre y tu DNI los gestiona la administración de la residencia.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (rol == Rol.Preceptor || rol == Rol.Administrador) {
                    ResiTarjeta(onClick = { verDias = true }) {
                        ElementoLista("Días sin culto", subtitulo = "Feriados y días sin cultos: no cortan la racha", icono = ResiIconos.Calendario, acento = Acento.Turquesa)
                    }
                }
                ResiBoton("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
            } else {
                TituloPantalla("Editar mis datos", subtitulo = p.nombreCompleto)
                ResiCampoTexto(f.email, vm::email, "Correo electrónico", error = f.errores["email"], ayuda = "Si lo cambias, tendrás que verificarlo otra vez", tipoTeclado = KeyboardType.Email, habilitado = !f.guardando)
                ResiCampoTexto(f.telefono, vm::telefono, "Teléfono", error = f.errores["telefono"], tipoTeclado = KeyboardType.Phone, habilitado = !f.guardando)
                CampoFecha("Fecha de nacimiento", f.fechaNacimiento, vm::fechaNacimiento, error = f.errores["fechaNacimiento"], habilitado = !f.guardando, anios = 1940..hoyLima().year)
                f.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else if (it == ErrorApi.SinConexion || it == ErrorApi.TiempoAgotado) "Necesitas conexión para guardar los cambios." else it.textoVisible(), EstadoVisual.Error) }
                ResiBoton("Guardar cambios", vm::guardar, Modifier.fillMaxWidth(), cargando = f.guardando)
                ResiBoton("Cancelar", vm::cancelar, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !f.guardando)
            }
        }
        if (verificando && p.email != null) {
            VerificarCorreoDialogo(p.email, { verificando = false }, { verificando = false; vm.refrescar() })
        }
    }
    if (confirmar) {
        DialogoConfirmacion("Cerrar sesión", "¿Deseas salir de ResiUP en este dispositivo?", "Cerrar sesión", { confirmar = false; onCerrarSesion() }, { confirmar = false }, destructivo = true)
    }
}
