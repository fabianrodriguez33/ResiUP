package pe.upeu.resi.resiup.presentation.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.AsistenciaRegistrada
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EnlaceApoderado
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.NuevaEvaluacion
import pe.upeu.resi.resiup.domain.model.ResidenteResumen
import pe.upeu.resi.resiup.domain.model.SesionCulto
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.domain.model.VerificacionGarita
import pe.upeu.resi.resiup.domain.repository.CultoRepository
import pe.upeu.resi.resiup.domain.repository.GaritaRepository
import pe.upeu.resi.resiup.domain.repository.LimpiezaRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ListaViewModel
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.ahora
import pe.upeu.resi.resiup.presentation.util.hoyLima

class SolicitudesPorRevisarViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(EstadoSolicitud.PendientePreceptor, it) })

class SolicitudesConApoderadoViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(EstadoSolicitud.PendienteApoderado, it) })

class HistorialGaritaViewModel(repo: GaritaRepository) : ListaViewModel<MovimientoRegistrado>({ repo.historial(it) })

// ---------------------------------------------------------------- inicio del preceptor

data class InicioPreceptorUi(val porRevisar: Long, val conApoderado: Long, val sesiones: List<SesionCulto>)

class InicioPreceptorViewModel(private val permisos: PermisoRepository, private val cultos: CultoRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<InicioPreceptorUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<InicioPreceptorUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch {
            coroutineScope {
                val a = async { permisos.listar(EstadoSolicitud.PendientePreceptor, 0) }
                val b = async { permisos.listar(EstadoSolicitud.PendienteApoderado, 0) }
                val c = async { cultos.sesionesDeHoy() }
                val ra = a.await()
                _estado.value = ra.fold(
                    { Carga.Exito(InicioPreceptorUi(it.totalElementos, b.await().getOrNull()?.totalElementos ?: 0, c.await().getOrDefault(emptyList()))) },
                    { Carga.Fallo(it.aErrorApi()) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- revisión de una solicitud

data class RevisionAccion(val trabajando: Boolean = false, val error: ErrorApi? = null, val mensaje: String? = null, val enlace: EnlaceApoderado? = null)

class SolicitudRevisionViewModel(private val id: Long, private val repo: PermisoRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<SolicitudPermiso>>(Carga.Cargando)
    val estado: StateFlow<Carga<SolicitudPermiso>> = _estado.asStateFlow()
    private val _accion = MutableStateFlow(RevisionAccion())
    val accion: StateFlow<RevisionAccion> = _accion.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch { _estado.value = repo.detalle(id).aCarga() }
    }

    fun recargar() {
        _accion.value = RevisionAccion()
        cargar()
    }

    fun decidir(aprobar: Boolean, comentario: String) {
        if (_accion.value.trabajando) return
        _accion.value = RevisionAccion(trabajando = true)
        viewModelScope.launch {
            repo.decidir(id, aprobar, comentario)
                .onSuccess { _accion.value = RevisionAccion(mensaje = if (aprobar) "Solicitud aprobada" else "Solicitud rechazada"); cargar() }
                .onFailure { _accion.value = RevisionAccion(error = it.aErrorApi()); cargar() }
        }
    }

    fun generarEnlace() {
        if (_accion.value.trabajando) return
        _accion.value = RevisionAccion(trabajando = true)
        viewModelScope.launch {
            repo.enlaceApoderado(id)
                .onSuccess { _accion.value = RevisionAccion(enlace = it) }
                .onFailure { _accion.value = RevisionAccion(error = it.aErrorApi()) }
        }
    }
}

// ---------------------------------------------------------------- asistencia a cultos

data class ResultadoEscaneo(val texto: String, val exito: Boolean, val detalle: String? = null)

data class AsistenciaUiState(
    val sesion: SesionCulto? = null,
    val cargandoSesion: Boolean = true,
    val procesando: Boolean = false,
    val errorSesion: ErrorApi? = null,
    val registros: List<ResultadoEscaneo> = emptyList(),
    val presentes: Long = 0,
)

class AsistenciaCultoViewModel(private val cultos: CultoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AsistenciaUiState())
    val estado: StateFlow<AsistenciaUiState> = _estado.asStateFlow()
    private var ultimoCodigo: String? = null
    private var ultimoInstante: Instant = Instant.fromEpochMilliseconds(0)

    init { recuperarSesionAbierta() }

    /** Si ya hay una sesión abierta hoy (p. ej. tras reabrir la app) se retoma en lugar de crear otra. */
    fun recuperarSesionAbierta() {
        _estado.update { it.copy(cargandoSesion = true, errorSesion = null) }
        viewModelScope.launch {
            cultos.sesionesDeHoy()
                .onSuccess { lista -> _estado.update { it.copy(cargandoSesion = false, sesion = lista.firstOrNull { s -> !s.cerrada }, presentes = lista.firstOrNull { s -> !s.cerrada }?.presentes ?: 0) } }
                .onFailure { e -> _estado.update { it.copy(cargandoSesion = false, errorSesion = e.aErrorApi()) } }
        }
    }

    fun abrir(tipo: TipoCulto) {
        _estado.update { it.copy(cargandoSesion = true, errorSesion = null) }
        viewModelScope.launch {
            cultos.abrirSesion(tipo)
                .onSuccess { s ->
                    if (s.cerrada) _estado.update { it.copy(cargandoSesion = false, errorSesion = ErrorApi.Conflicto("El culto ${tipo.etiqueta.lowercase()} de hoy ya fue cerrado")) }
                    else _estado.update { it.copy(cargandoSesion = false, sesion = s, presentes = s.presentes) }
                }
                .onFailure { e -> _estado.update { it.copy(cargandoSesion = false, errorSesion = e.aErrorApi()) } }
        }
    }

    /** El mismo código leído varias veces seguidas se ignora unos segundos (la cámara lo detecta en cada cuadro). */
    fun codigoLeido(qr: String) {
        val e = _estado.value
        val sesion = e.sesion ?: return
        val t = ahora()
        if (e.procesando || (qr == ultimoCodigo && (t - ultimoInstante).inWholeSeconds < 4)) return
        ultimoCodigo = qr
        ultimoInstante = t
        _estado.update { it.copy(procesando = true) }
        viewModelScope.launch {
            cultos.registrarAsistencia(sesion.id, qr)
                .onSuccess { a: AsistenciaRegistrada ->
                    _estado.update {
                        it.copy(procesando = false, presentes = it.presentes + 1,
                            registros = (listOf(ResultadoEscaneo("${a.nombreCompleto} (${a.codigo})", true, if (a.despuesDelCorte) "Registrado después del corte" else "Registrado")) + it.registros).take(8))
                    }
                }
                .onFailure { err ->
                    val msg = when (val x = err.aErrorApi()) {
                        is ErrorApi.Conflicto -> x.mensaje
                        is ErrorApi.Validacion -> "El código QR no es válido"
                        else -> x.aMensaje()
                    }
                    _estado.update { it.copy(procesando = false, registros = (listOf(ResultadoEscaneo("No registrado", false, msg)) + it.registros).take(8)) }
                }
        }
    }

    fun cerrarSesion() {
        val sesion = _estado.value.sesion ?: return
        viewModelScope.launch {
            cultos.cerrarSesion(sesion.id)
                .onSuccess { _estado.value = AsistenciaUiState(cargandoSesion = false) }
                .onFailure { e -> _estado.update { it.copy(errorSesion = e.aErrorApi()) } }
        }
    }
}

// ---------------------------------------------------------------- evaluación de limpieza

data class LimpiezaFormState(
    val residentes: Carga<List<ResidenteResumen>> = Carga.Cargando,
    val seleccionado: ResidenteResumen? = null,
    val puntajes: Map<CriterioLimpieza, Int> = CriterioLimpieza.entries.associateWith { 0 },
    val observacion: String = "",
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
) {
    val total get() = puntajes.values.sum()
}

class LimpiezaPreceptorViewModel(private val residentes: ResidenteRepository, private val limpieza: LimpiezaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(LimpiezaFormState())
    val estado: StateFlow<LimpiezaFormState> = _estado.asStateFlow()

    init { cargarResidentes() }

    fun cargarResidentes() {
        _estado.update { it.copy(residentes = Carga.Cargando) }
        viewModelScope.launch { _estado.update { it.copy(residentes = residentes.listarResidentes().aCarga()) } }
    }

    fun seleccionar(r: ResidenteResumen?) = _estado.update { it.copy(seleccionado = r, mensaje = null, error = null, errores = emptyMap()) }

    fun ajustar(c: CriterioLimpieza, delta: Int) =
        _estado.update { it.copy(puntajes = it.puntajes + (c to ((it.puntajes.getValue(c) + delta).coerceIn(0, c.maximo))), mensaje = null, errores = emptyMap()) }

    fun observacion(v: String) = _estado.update { it.copy(observacion = v.take(500)) }

    fun guardar() {
        val s = _estado.value
        val r = s.seleccionado ?: return
        if (s.enviando) return
        _estado.update { it.copy(enviando = true, error = null, errores = emptyMap(), mensaje = null) }
        viewModelScope.launch {
            limpieza.registrar(NuevaEvaluacion(r.id, hoyLima().toString(), s.puntajes, s.observacion))
                .onSuccess { e ->
                    _estado.update {
                        it.copy(enviando = false, seleccionado = null, observacion = "", puntajes = CriterioLimpieza.entries.associateWith { 0 },
                            mensaje = "Evaluación guardada: ${r.nombreCompleto} obtuvo ${e.total}/${e.maximo}")
                    }
                }
                .onFailure { err ->
                    val x = err.aErrorApi()
                    _estado.update { if (x is ErrorApi.Validacion) it.copy(enviando = false, errores = x.porCampo) else it.copy(enviando = false, error = x) }
                }
        }
    }
}

// ---------------------------------------------------------------- garita

sealed interface GaritaEstado {
    data object Esperando : GaritaEstado
    data object Verificando : GaritaEstado
    data class Resultado(val qr: String, val verificacion: VerificacionGarita, val registrando: Boolean = false, val error: ErrorApi? = null) : GaritaEstado
    data class Registrado(val movimiento: MovimientoRegistrado) : GaritaEstado
    data class Fallo(val error: ErrorApi) : GaritaEstado
}

class GaritaEscanerViewModel(private val repo: GaritaRepository) : ViewModel() {
    private val _estado = MutableStateFlow<GaritaEstado>(GaritaEstado.Esperando)
    val estado: StateFlow<GaritaEstado> = _estado.asStateFlow()

    /** Sólo se acepta un código si no hay otro resultado en pantalla; garita decide cuándo pasar al siguiente. */
    fun codigoLeido(qr: String) {
        if (_estado.value != GaritaEstado.Esperando) return
        _estado.value = GaritaEstado.Verificando
        viewModelScope.launch {
            repo.verificar(qr)
                .onSuccess { _estado.value = GaritaEstado.Resultado(qr, it) }
                .onFailure { _estado.value = GaritaEstado.Fallo(it.aErrorApi()) }
        }
    }

    /** El servidor vuelve a validar todo al registrar; este botón nunca autoriza por sí mismo. */
    fun registrar(tipo: TipoMovimiento) {
        val actual = _estado.value as? GaritaEstado.Resultado ?: return
        if (actual.registrando) return
        _estado.value = actual.copy(registrando = true, error = null)
        viewModelScope.launch {
            repo.registrar(actual.qr, tipo)
                .onSuccess { _estado.value = GaritaEstado.Registrado(it) }
                .onFailure { _estado.value = actual.copy(registrando = false, error = it.aErrorApi()) }
        }
    }

    fun siguiente() {
        _estado.value = GaritaEstado.Esperando
    }
}
