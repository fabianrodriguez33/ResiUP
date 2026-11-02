package pe.upeu.resi.resiup.presentation.tramites

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.AmpliacionPendiente
import pe.upeu.resi.resiup.domain.model.ImagenElegida
import pe.upeu.resi.resiup.domain.model.JustificacionPendiente
import pe.upeu.resi.resiup.domain.repository.TramitesRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CampoFechaHora
import pe.upeu.resi.resiup.presentation.components.DialogoMotivo
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.rememberSelectorImagen
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.formatoLima

// ---------------------------------------------------------------- foto

/** Foto de una evidencia guardada en el servidor (solo la ve el residente que la subió y el pastor). */
@Composable
fun EvidenciaImagen(id: Long, modifier: Modifier = Modifier) {
    val repo: TramitesRepository = koinInject()
    val bitmap by produceState<Result<androidx.compose.ui.graphics.ImageBitmap>?>(null, id) {
        value = repo.descargarEvidencia(id).mapCatching { it.decodeToImageBitmap() }
    }
    when (val r = bitmap) {
        null -> Text("Cargando foto…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> r.fold(
            { Image(it, "Foto adjunta", modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit) },
            { Text("No se pudo cargar la foto.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) },
        )
    }
}

@Composable
private fun VistaPrevia(imagen: ImagenElegida, modifier: Modifier = Modifier) {
    val bitmap = remember(imagen) { runCatching { imagen.bytes.decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, "Foto elegida", modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
}

// ---------------------------------------------------------------- residente: ampliar o justificar

data class TramiteUi(
    val motivo: String = "",
    val retorno: Instant? = null,
    val foto: ImagenElegida? = null,
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val enviado: Boolean = false,
)

class TramiteViewModel(private val solicitudId: Long, private val repo: TramitesRepository) : ViewModel() {
    private val _estado = MutableStateFlow(TramiteUi())
    val estado: StateFlow<TramiteUi> = _estado.asStateFlow()

    fun motivo(v: String) = _estado.update { it.copy(motivo = v.take(300), errores = it.errores - "motivo", error = null) }
    fun retorno(v: Instant) = _estado.update { it.copy(retorno = v, errores = it.errores - "retornoNuevo", error = null) }
    fun foto(v: ImagenElegida?) { if (v != null) _estado.update { it.copy(foto = v, errores = it.errores - "evidenciaId", error = null) } }
    fun quitarFoto() = _estado.update { it.copy(foto = null) }

    /** La ampliación exige foto y un horario posterior al autorizado. */
    fun ampliar(retornoAutorizado: Instant) {
        val s = _estado.value
        val errores = buildMap {
            if (s.motivo.isBlank()) put("motivo", "Cuenta por qué necesitas más tiempo")
            if (s.retorno == null) put("retornoNuevo", "Elige hasta qué hora necesitas")
            else if (s.retorno <= retornoAutorizado) put("retornoNuevo", "Debe ser después de la hora autorizada")
            if (s.foto == null) put("evidenciaId", "Adjunta una foto que respalde el pedido")
        }
        if (errores.isNotEmpty()) { _estado.update { it.copy(errores = errores) }; return }
        enviar { evidencia -> repo.ampliar(solicitudId, s.motivo, s.retorno!!, evidencia) }
    }

    /** La justificación lleva la foto solo si la persona quiere adjuntarla. */
    fun justificar() {
        val s = _estado.value
        if (s.motivo.isBlank()) { _estado.update { it.copy(errores = mapOf("motivo" to "Cuenta qué ocurrió")) }; return }
        enviar { evidencia -> repo.justificar(solicitudId, s.motivo, evidencia) }
    }

    private fun enviar(llamada: suspend (Long?) -> Result<Unit>) {
        val s = _estado.value
        if (s.enviando) return
        _estado.update { it.copy(enviando = true, error = null, errores = emptyMap()) }
        viewModelScope.launch {
            val evidencia = s.foto?.let { f -> repo.subirEvidencia(f).getOrElse { e -> return@launch falla(e) } }
            llamada(evidencia)
                .onSuccess { _estado.value = TramiteUi(enviado = true) }
                .onFailure { falla(it) }
        }
    }

    private fun falla(e: Throwable) {
        val err = e.aErrorApi()
        _estado.update {
            if (err is ErrorApi.Validacion) it.copy(enviando = false, errores = err.porCampo)
            else it.copy(enviando = false, error = err)
        }
    }
}

/** Formulario que el residente ve en el detalle de su permiso: ampliar antes de que venza, o justificar después. */
@Composable
fun TramiteResidente(solicitudId: Long, justificacion: Boolean, retornoAutorizado: Instant, onEnviado: () -> Unit) {
    val vm = koinViewModel<TramiteViewModel>(key = "tramite-$solicitudId-$justificacion", parameters = { parametersOf(solicitudId) })
    val s by vm.estado.collectAsState()
    val abrirGaleria = rememberSelectorImagen(vm::foto)
    LaunchedEffect(s.enviado) { if (s.enviado) onEnviado() }
    ResiTarjeta {
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Text(if (justificacion) "El permiso venció" else "¿Necesitas más tiempo?", style = MaterialTheme.typography.titleMedium)
            Text(
                if (justificacion) "Si regresaste fuera de horario, cuéntanos qué ocurrió. No cambia el horario autorizado; el pastor la revisará."
                else "Pide volver más tarde. Adjunta una foto que lo respalde; el pastor lo revisará antes de la hora autorizada.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ResiCampoTexto(s.motivo, vm::motivo, if (justificacion) "Justificación" else "Motivo", error = s.errores["motivo"], unaLinea = false, habilitado = !s.enviando)
            if (!justificacion) {
                CampoFechaHora("Nuevo horario de ingreso", s.retorno, s.errores["retornoNuevo"], vm::retorno)
            }
            s.foto?.let { VistaPrevia(it) }
            s.errores["evidenciaId"]?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                ResiBoton(
                    if (s.foto == null) (if (justificacion) "Adjuntar foto (opcional)" else "Adjuntar foto") else "Cambiar foto",
                    abrirGaleria, Modifier.weight(1f), variante = VarianteBoton.Secundario, habilitado = !s.enviando,
                )
                if (s.foto != null) ResiBoton("Quitar", vm::quitarFoto, variante = VarianteBoton.Terciario, habilitado = !s.enviando)
            }
            s.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
            ResiBoton(
                if (justificacion) "Enviar justificación" else "Enviar pedido",
                { if (justificacion) vm.justificar() else vm.ampliar(retornoAutorizado) },
                Modifier.fillMaxWidth(), cargando = s.enviando,
            )
        }
    }
}

// ---------------------------------------------------------------- pastor: revisar

data class RevisionUi(val trabajando: Boolean = false, val error: ErrorApi? = null, val mensaje: String? = null)

class RevisionTramitesViewModel(private val repo: TramitesRepository) : ViewModel() {
    private val _ampliaciones = MutableStateFlow<Carga<List<AmpliacionPendiente>>>(Carga.Cargando)
    val ampliaciones: StateFlow<Carga<List<AmpliacionPendiente>>> = _ampliaciones.asStateFlow()
    private val _justificaciones = MutableStateFlow<Carga<List<JustificacionPendiente>>>(Carga.Cargando)
    val justificaciones: StateFlow<Carga<List<JustificacionPendiente>>> = _justificaciones.asStateFlow()
    private val _accion = MutableStateFlow(RevisionUi())
    val accion: StateFlow<RevisionUi> = _accion.asStateFlow()

    /** Cantidad por revisar entre ambas listas (null mientras carga). */
    fun total(a: Carga<List<AmpliacionPendiente>>, j: Carga<List<JustificacionPendiente>>): Int? =
        if (a is Carga.Exito && j is Carga.Exito) a.datos.size + j.datos.size else null

    fun cargar() {
        viewModelScope.launch { _ampliaciones.value = repo.ampliacionesPendientes().aCarga() }
        viewModelScope.launch { _justificaciones.value = repo.justificacionesPendientes().aCarga() }
    }

    fun decidirAmpliacion(a: AmpliacionPendiente, aprobar: Boolean, comentario: String?) =
        decidir(if (aprobar) "Ampliación aprobada" else "Ampliación rechazada") { repo.decidirAmpliacion(a.id, aprobar, comentario) }

    fun decidirJustificacion(j: JustificacionPendiente, aprobar: Boolean, comentario: String?) =
        decidir(if (aprobar) "Justificación aceptada" else "Justificación rechazada") { repo.decidirJustificacion(j.id, aprobar, comentario) }

    private fun decidir(mensaje: String, llamada: suspend () -> Result<Unit>) {
        if (_accion.value.trabajando) return
        _accion.value = RevisionUi(trabajando = true)
        viewModelScope.launch {
            llamada()
                .onSuccess { _accion.value = RevisionUi(mensaje = mensaje); cargar() }
                .onFailure { _accion.value = RevisionUi(error = it.aErrorApi()) }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun RevisionTramitesScreen(onVolver: () -> Unit, vm: RevisionTramitesViewModel = koinViewModel()) {
    val ampliaciones by vm.ampliaciones.collectAsState()
    val justificaciones by vm.justificaciones.collectAsState()
    val accion by vm.accion.collectAsState()
    var rechazando by remember { mutableStateOf<(String) -> Unit>({}) }
    var pidiendoMotivo by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.cargar() }
    BackHandler(onBack = onVolver)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        TituloPantalla("Por revisar", subtitulo = "Ampliaciones de horario y justificaciones de tardanza")
        accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        accion.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }

        Text("Ampliaciones de horario", style = MaterialTheme.typography.titleMedium)
        ContenidoCarga(ampliaciones, vm::cargar, Modifier.fillMaxWidth()) { lista ->
            if (lista.isEmpty()) Text("No hay ampliaciones pendientes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            lista.forEach { a ->
                var verFoto by remember(a.id) { mutableStateOf(false) }
                ResiTarjeta {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        ElementoLista(a.residenteNombre, subtitulo = "Código ${a.residenteCodigo}", icono = ResiIconos.Permisos, acento = Acento.Naranja)
                        Text("Pide volver a las ${a.retornoNuevo.formatoLima()} (autorizado hasta ${a.retornoOriginal.formatoLima()}).", style = MaterialTheme.typography.bodyMedium)
                        Text("Motivo: ${a.motivo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (a.evidenciaId != null) {
                            ResiBoton(if (verFoto) "Ocultar foto" else "Ver foto", { verFoto = !verFoto }, variante = VarianteBoton.Terciario)
                            if (verFoto) EvidenciaImagen(a.evidenciaId)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            ResiBoton("Rechazar", { rechazando = { c -> vm.decidirAmpliacion(a, false, c) }; pidiendoMotivo = true }, Modifier.weight(1f), variante = VarianteBoton.Secundario, habilitado = !accion.trabajando)
                            ResiBoton("Aprobar", { vm.decidirAmpliacion(a, true, null) }, Modifier.weight(1f), habilitado = !accion.trabajando)
                        }
                    }
                }
            }
        }

        Text("Justificaciones de tardanza", Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.titleMedium)
        ContenidoCarga(justificaciones, vm::cargar, Modifier.fillMaxWidth()) { lista ->
            if (lista.isEmpty()) Text("No hay justificaciones pendientes.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            lista.forEach { j ->
                var verFoto by remember(j.id) { mutableStateOf(false) }
                ResiTarjeta {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        ElementoLista(j.residenteNombre, subtitulo = "Código ${j.residenteCodigo}", icono = ResiIconos.Permisos, acento = Acento.Violeta)
                        Text(j.motivo, style = MaterialTheme.typography.bodyMedium)
                        if (j.evidenciaId != null) {
                            ResiBoton(if (verFoto) "Ocultar foto" else "Ver foto", { verFoto = !verFoto }, variante = VarianteBoton.Terciario)
                            if (verFoto) EvidenciaImagen(j.evidenciaId)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            ResiBoton("Rechazar", { rechazando = { c -> vm.decidirJustificacion(j, false, c) }; pidiendoMotivo = true }, Modifier.weight(1f), variante = VarianteBoton.Secundario, habilitado = !accion.trabajando)
                            ResiBoton("Aceptar", { vm.decidirJustificacion(j, true, null) }, Modifier.weight(1f), habilitado = !accion.trabajando)
                        }
                    }
                }
            }
        }
        val a = ampliaciones; val j = justificaciones
        if (a is Carga.Exito && j is Carga.Exito && a.datos.isEmpty() && j.datos.isEmpty()) {
            EstadoVacio("Todo al día", "No hay trámites por revisar.")
        }
    }
    if (pidiendoMotivo) {
        DialogoMotivo(
            "Rechazar", "Escribe el motivo: lo verá el residente.", "Motivo", "Rechazar",
            { c -> pidiendoMotivo = false; rechazando(c) }, { pidiendoMotivo = false }, destructivo = true,
        )
    }
}
