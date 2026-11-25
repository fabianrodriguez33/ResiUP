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
import pe.upeu.resi.resiup.domain.model.ResumenResidente
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

/** Permisos para el pastor: solo consulta. Todo permiso lo revisa el pastor: lo aprueba o lo rechaza. */
class PermisosAprobadosViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(EstadoSolicitud.Aprobado, it) })

class PermisosEnEsperaViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(EstadoSolicitud.PendienteApoderado, it) })

class PermisosEnRevisionViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(EstadoSolicitud.PendientePreceptor, it) })

class PermisosGaritaViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listarGarita(it) })

class PermisosTodosViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.listar(null, it) })

class HistorialGaritaViewModel(repo: GaritaRepository) : ListaViewModel<MovimientoRegistrado>({ repo.historial(it) })

// ---------------------------------------------------------------- inicio del preceptor

data class InicioPreceptorUi(val aprobados: Long, val enEspera: Long, val sesiones: List<SesionCulto>, val presentes: Long = 0)

class InicioPreceptorViewModel(private val permisos: PermisoRepository, private val cultos: CultoRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<InicioPreceptorUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<InicioPreceptorUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        // Al volver al inicio se actualiza sin borrar lo que ya se ve (el culto abierto cambia el acceso «Registrar»).
        if (_estado.value !is Carga.Exito) _estado.value = Carga.Cargando
        viewModelScope.launch {
            coroutineScope {
                val a = async { permisos.aprobadosHoy() }
                val b = async { permisos.listar(EstadoSolicitud.PendientePreceptor, 0) }
                val c = async { cultos.sesionesDeHoy() }
                val d = async { cultos.presentesHoy() }
                val ra = a.await()
                _estado.value = ra.fold(
                    { Carga.Exito(InicioPreceptorUi(it, b.await().getOrNull()?.totalElementos ?: 0, c.await().getOrDefault(emptyList()), d.await().getOrDefault(0))) },
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
    private val _resumen = MutableStateFlow<Carga<ResumenResidente>>(Carga.Cargando)

    /** Asistencia a cultos y limpieza de los últimos 30 días: lo que el pastor revisa antes de aprobar. */
    val resumen: StateFlow<Carga<ResumenResidente>> = _resumen.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        _resumen.value = Carga.Cargando
        viewModelScope.launch { _estado.value = repo.detalle(id).aCarga() }
        viewModelScope.launch { _resumen.value = repo.resumenDelResidente(id).aCarga() }
    }

    fun recargar() {
        _accion.value = RevisionAccion()
        cargar()
    }

    /** Anula un permiso aprobado que el residente todavía no usó; garita deja de autorizarlo. */
    fun anular(motivo: String) {
        if (_accion.value.trabajando) return
        _accion.value = RevisionAccion(trabajando = true)
        viewModelScope.launch {
            repo.anular(id, motivo)
                .onSuccess { _accion.value = RevisionAccion(mensaje = "Permiso anulado"); pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Permiso anulado exitosamente"); cargar() }
                .onFailure { _accion.value = RevisionAccion(error = it.aErrorApi()) }
        }
    }

    /** Aprueba o rechaza un permiso en revisión; rechazar exige el motivo. */
    fun decidir(aprobar: Boolean, comentario: String?) {
        if (_accion.value.trabajando) return
        _accion.value = RevisionAccion(trabajando = true)
        viewModelScope.launch {
            repo.decidir(id, aprobar, comentario)
                .onSuccess { _accion.value = RevisionAccion(mensaje = if (aprobar) "Permiso aprobado" else "Permiso rechazado"); pe.upeu.resi.resiup.presentation.components.Mensajes.exito(if (aprobar) "Permiso aprobado exitosamente" else "Permiso rechazado exitosamente"); cargar() }
                .onFailure { _accion.value = RevisionAccion(error = it.aErrorApi()) }
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

    /** Código manual: el pastor escribe el código universitario cuando el QR no se puede leer. */
    fun codigoManual(codigo: String) {
        val sesion = _estado.value.sesion ?: return
        if (_estado.value.procesando) return
        _estado.update { it.copy(procesando = true) }
        viewModelScope.launch {
            cultos.registrarPorCodigo(sesion.id, codigo.trim())
                .onSuccess { a ->
                    _estado.update { it.copy(procesando = false, presentes = it.presentes + 1, registros = (listOf(ResultadoEscaneo("${a.nombreCompleto} (${a.codigo})", true, null)) + it.registros).take(8)) }
                }
                .onFailure { err ->
                    val msg = when (val x = err.aErrorApi()) { is ErrorApi.Conflicto -> x.mensaje; is ErrorApi.Validacion -> x.porCampo.values.firstOrNull() ?: "Código no válido"; else -> x.aMensaje() }
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

/** Orden de los criterios en el menú: cama, ropero y escritorio son de cada residente; basura, olores y piso, de la habitación. */
val CRITERIOS_EN_MENU = listOf(CriterioLimpieza.Cama, CriterioLimpieza.Ropero, CriterioLimpieza.Escritorio, CriterioLimpieza.Basura, CriterioLimpieza.Olores, CriterioLimpieza.Piso)
val CRITERIOS_POR_RESIDENTE = listOf(CriterioLimpieza.Cama, CriterioLimpieza.Ropero, CriterioLimpieza.Escritorio)

data class LimpiezaFormState(
    val fecha: kotlinx.datetime.LocalDate = hoyLima(),
    val habitaciones: Carga<List<HabitacionLimpieza>> = Carga.Cargando,
    val habitacion: HabitacionLimpieza? = null,
    /** Criterio que se está puntuando (el menú «Criterio»). */
    val criterio: CriterioLimpieza? = null,
    /** Nota de la habitación (basura, olores, piso); null = todavía sin poner. */
    val comunes: Map<CriterioLimpieza, Int?> = emptyMap(),
    /** Nota de cada residente que falta evaluar (cama, ropero, escritorio); null = todavía sin poner. */
    val personales: Map<Long, Map<CriterioLimpieza, Int?>> = emptyMap(),
    /** Criterios ya registrados de esta habitación; al registrar los seis se guarda la evaluación. */
    val registrados: Set<CriterioLimpieza> = emptySet(),
    /** Se está modificando una evaluación ya registrada (se guarda reemplazándola). */
    val edicion: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val mensaje: String? = null,
) {
    fun comun(c: CriterioLimpieza): Int? = comunes[c]

    /** El criterio elegido se puede registrar cuando todas sus filas tienen nota. */
    val criterioCompleto: Boolean get() = when (criterio) {
        null -> false
        in CRITERIOS_POR_RESIDENTE -> personales.isNotEmpty() && personales.values.all { it[criterio] != null }
        else -> comunes[criterio] != null
    }
}

class LimpiezaPreceptorViewModel(private val limpieza: LimpiezaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(LimpiezaFormState())
    val estado: StateFlow<LimpiezaFormState> = _estado.asStateFlow()

    /** Avance de cada habitación (por fecha): si el pastor sale por error y vuelve a entrar, no pierde lo que ya puso. Se borra al guardar la habitación. */
    private val avances = mutableMapOf<Pair<kotlinx.datetime.LocalDate, Long>, LimpiezaFormState>()

    private fun respaldar() {
        val s = _estado.value
        val h = s.habitacion ?: return
        if (s.edicion) return
        avances[s.fecha to h.id] = s
    }

    init { cargar() }

    fun cargar() {
        val f = _estado.value.fecha
        _estado.update { it.copy(habitaciones = Carga.Cargando) }
        viewModelScope.launch { _estado.update { it.copy(habitaciones = limpieza.habitaciones(f.toString()).aCarga()) } }
    }

    fun cambiarFecha(f: kotlinx.datetime.LocalDate) { _estado.update { it.copy(fecha = f, mensaje = null) }; cargar() }

    fun abrir(h: HabitacionLimpieza) = _estado.update {
        val previo = avances[it.fecha to h.id]
        val vacioPersonal: Map<CriterioLimpieza, Int?> = CRITERIOS_POR_RESIDENTE.associateWith { null }
        it.copy(
            habitacion = h, criterio = previo?.criterio, registrados = previo?.registrados ?: emptySet(), mensaje = null, error = null,
            comunes = previo?.comunes ?: listOf(CriterioLimpieza.Basura, CriterioLimpieza.Olores, CriterioLimpieza.Piso).associateWith { null },
            personales = h.pendientes.associate { r -> r.residenteId to (previo?.personales?.get(r.residenteId) ?: vacioPersonal) },
        )
    }

    fun cerrar() { respaldar(); _estado.update { it.copy(habitacion = null, criterio = null, error = null, edicion = false) } }

    /** Abre una habitación ya evaluada con sus puntajes para modificarlos; «Guardar cambios» reemplaza la evaluación de ese día. */
    fun editar(h: HabitacionLimpieza) {
        viewModelScope.launch {
            limpieza.detalleHabitacion(h.id, _estado.value.fecha.toString())
                .onSuccess { d ->
                    val todos = h.copy(residentes = h.residentes.map { it.copy(evaluado = false) })
                    _estado.update {
                        it.copy(
                            habitacion = todos, criterio = null, registrados = emptySet(), mensaje = null, error = null, edicion = true,
                            comunes = mapOf(CriterioLimpieza.Basura to d.basura, CriterioLimpieza.Olores to d.olores, CriterioLimpieza.Piso to d.personas.firstOrNull()?.piso),
                            personales = d.personas.associate { p -> p.residenteId to mapOf<CriterioLimpieza, Int?>(CriterioLimpieza.Ropero to p.ropero, CriterioLimpieza.Cama to p.cama, CriterioLimpieza.Escritorio to p.escritorio) },
                        )
                    }
                }
                .onFailure { e -> _estado.update { it.copy(error = e.aErrorApi()) } }
        }
    }

    fun guardarCambios() = guardar()

    /** Registra la habitación completa (o guarda los cambios) cuando ya no falta ninguna nota. */
    fun registrarTodo() = guardar()

    fun elegirCriterio(c: CriterioLimpieza) { _estado.update { it.copy(criterio = c, error = null) }; respaldar() }

    private fun paso(actual: Int?, delta: Int, maximo: Int) = if (actual == null) (if (delta > 0) 1 else 0).coerceAtMost(maximo) else (actual + delta).coerceIn(0, maximo)

    fun ajustarComun(c: CriterioLimpieza, delta: Int) {
        _estado.update { it.copy(comunes = it.comunes + (c to paso(it.comunes[c], delta, c.maximo)), error = null) }
        respaldar()
    }

    fun ajustar(residenteId: Long, c: CriterioLimpieza, delta: Int) {
        _estado.update {
            val actual = it.personales[residenteId] ?: return@update it
            it.copy(personales = it.personales + (residenteId to (actual + (c to paso(actual[c], delta, c.maximo)))), error = null)
        }
        respaldar()
    }

    /** Registra el criterio elegido; al completar los seis criterios se guarda la evaluación de la habitación. */
    fun registrarCriterio() {
        val s = _estado.value
        val c = s.criterio ?: return
        if (!s.criterioCompleto || s.enviando) return
        val registrados = s.registrados + c
        _estado.update { it.copy(registrados = registrados, criterio = null, error = null) }
        respaldar()
        if (registrados.containsAll(CRITERIOS_EN_MENU) || s.personales.isEmpty() && registrados.containsAll(listOf(CriterioLimpieza.Basura, CriterioLimpieza.Olores, CriterioLimpieza.Piso))) guardar()
    }

    private fun guardar() {
        val s = _estado.value
        val h = s.habitacion ?: return
        if (s.enviando || s.personales.isEmpty()) return
        _estado.update { it.copy(enviando = true, error = null, mensaje = null) }
        val piso = s.comunes.getValue(CriterioLimpieza.Piso) ?: 0
        val personas = s.personales.map { (id, p) ->
            PuntajePersonal(id, p.getValue(CriterioLimpieza.Ropero) ?: 0, p.getValue(CriterioLimpieza.Cama) ?: 0, p.getValue(CriterioLimpieza.Escritorio) ?: 0, piso)
        }
        viewModelScope.launch {
            (if (s.edicion) limpieza::reemplazarHabitacion else limpieza::registrarHabitacion)(NuevaEvaluacionHabitacion(h.id, s.fecha.toString(), s.comunes.getValue(CriterioLimpieza.Basura) ?: 0, s.comunes.getValue(CriterioLimpieza.Olores) ?: 0, personas))
                .onSuccess { lista ->
                    avances.remove(s.fecha to h.id)
                    pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Habitación ${h.numero} evaluada exitosamente")
                    _estado.update { it.copy(enviando = false, habitacion = null, criterio = null, mensaje = "Habitación ${h.numero} evaluada (${lista.size} ${if (lista.size == 1) "residente" else "residentes"})") }
                    cargar()
                }
                .onFailure { err ->
                    val x = err.aErrorApi()
                    _estado.update { it.copy(enviando = false, registrados = it.registrados - (it.registrados.lastOrNull() ?: CriterioLimpieza.Piso), error = x) }
                }
        }
    }
}

// ---------------------------------------------------------------- garita

sealed interface GaritaEstado {
    data object Esperando : GaritaEstado
    data object Verificando : GaritaEstado
    /** QR leido: el residente y sus permisos aprobados; garita elige con cual sale o vuelve. */
    data class ElegirPermiso(val qr: String, val datos: pe.upeu.resi.resiup.domain.model.PermisosDeResidente) : GaritaEstado
    data class Resultado(val qr: String, val verificacion: VerificacionGarita, val registrando: Boolean = false, val error: ErrorApi? = null, val solicitudId: Long? = null) : GaritaEstado
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
            repo.permisosDe(qr)
                .onSuccess { d ->
                    if (!d.valido || d.permisos.isEmpty()) {
                        // QR no valido o sin permisos pendientes: se muestra el motivo con la verificacion de siempre.
                        repo.verificar(qr)
                            .onSuccess { _estado.value = GaritaEstado.Resultado(qr, it) }
                            .onFailure { _estado.value = GaritaEstado.Fallo(it.aErrorApi()) }
                    } else _estado.value = GaritaEstado.ElegirPermiso(qr, d)
                }
                .onFailure { _estado.value = GaritaEstado.Fallo(it.aErrorApi()) }
        }
    }

    /** Garita eligio un permiso de la lista: se verifica con ese permiso y se muestra el boton de registrar. */
    fun elegir(qr: String, solicitudId: Long) {
        _estado.value = GaritaEstado.Verificando
        viewModelScope.launch {
            repo.verificar(qr, solicitudId)
                .onSuccess { _estado.value = GaritaEstado.Resultado(qr, it, solicitudId = solicitudId) }
                .onFailure { _estado.value = GaritaEstado.Fallo(it.aErrorApi()) }
        }
    }

    /** El servidor vuelve a validar todo al registrar; este boton nunca autoriza por si mismo. */
    fun registrar(tipo: TipoMovimiento) {
        val actual = _estado.value as? GaritaEstado.Resultado ?: return
        if (actual.registrando) return
        _estado.value = actual.copy(registrando = true, error = null)
        viewModelScope.launch {
            repo.registrar(actual.qr, tipo, actual.solicitudId)
                .onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("${tipo.etiqueta} registrada exitosamente"); _estado.value = GaritaEstado.Registrado(it) }
                .onFailure { _estado.value = actual.copy(registrando = false, error = it.aErrorApi()) }
        }
    }

    fun siguiente() {
        _estado.value = GaritaEstado.Esperando
    }
}
