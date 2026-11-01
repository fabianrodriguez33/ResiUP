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
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import pe.upeu.resi.resiup.domain.model.NuevaEvaluacionHabitacion
import pe.upeu.resi.resiup.domain.model.PuntajePersonal
import pe.upeu.resi.resiup.domain.model.CRITERIOS_PERSONALES
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

// ---------------------------------------------------------------- evaluación de limpieza (por habitación)

data class LimpiezaFormState(
    val habitaciones: Carga<List<HabitacionLimpieza>> = Carga.Cargando,
    val habitacion: HabitacionLimpieza? = null,
    /** Comunes a toda la habitación. */
    val basura: Int = 0,
    val olores: Int = 0,
    /** Ropero, cama, escritorio y piso de cada residente que todavía no tiene nota hoy. */
    val personales: Map<Long, Map<CriterioLimpieza, Int>> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val mensaje: String? = null,
) {
    fun comun(c: CriterioLimpieza) = if (c == CriterioLimpieza.Basura) basura else olores
    fun totalDe(residenteId: Long) = basura + olores + personales[residenteId].orEmpty().values.sum()
}

class LimpiezaPreceptorViewModel(private val limpieza: LimpiezaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(LimpiezaFormState())
    val estado: StateFlow<LimpiezaFormState> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.update { it.copy(habitaciones = Carga.Cargando) }
        viewModelScope.launch { _estado.update { it.copy(habitaciones = limpieza.habitaciones(hoyLima().toString()).aCarga()) } }
    }

    fun abrir(h: HabitacionLimpieza) = _estado.update {
        it.copy(
            habitacion = h, basura = 0, olores = 0, mensaje = null, error = null,
            personales = h.pendientes.associate { r -> r.residenteId to CRITERIOS_PERSONALES.associateWith { 0 } },
        )
    }

    fun cerrar() = _estado.update { it.copy(habitacion = null, error = null) }

    fun ajustarComun(c: CriterioLimpieza, delta: Int) = _estado.update {
        val v = (it.comun(c) + delta).coerceIn(0, c.maximo)
        (if (c == CriterioLimpieza.Basura) it.copy(basura = v) else it.copy(olores = v)).copy(mensaje = null, error = null)
    }

    fun ajustar(residenteId: Long, c: CriterioLimpieza, delta: Int) = _estado.update {
        val actual = it.personales[residenteId] ?: return@update it
        it.copy(personales = it.personales + (residenteId to (actual + (c to ((actual.getValue(c) + delta).coerceIn(0, c.maximo))))), mensaje = null, error = null)
    }

    fun guardar() {
        val s = _estado.value
        val h = s.habitacion ?: return
        if (s.enviando || s.personales.isEmpty()) return
        _estado.update { it.copy(enviando = true, error = null, mensaje = null) }
        val personas = s.personales.map { (id, p) ->
            PuntajePersonal(id, p.getValue(CriterioLimpieza.Ropero), p.getValue(CriterioLimpieza.Cama), p.getValue(CriterioLimpieza.Escritorio), p.getValue(CriterioLimpieza.Piso))
        }
        viewModelScope.launch {
            limpieza.registrarHabitacion(NuevaEvaluacionHabitacion(h.id, hoyLima().toString(), s.basura, s.olores, personas))
                .onSuccess { lista ->
                    _estado.update { it.copy(enviando = false, habitacion = null, mensaje = "Habitación ${h.numero} evaluada (${lista.size} ${if (lista.size == 1) "residente" else "residentes"})") }
                    cargar()
                }
                .onFailure { err ->
                    val x = err.aErrorApi()
                    _estado.update { it.copy(enviando = false, error = x) }
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
