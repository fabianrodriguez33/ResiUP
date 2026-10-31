package pe.upeu.resi.resiup.presentation.residente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.QrPersonal
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ListaViewModel
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.ahora

class AsistenciasViewModel(repo: ResidenteRepository) : ListaViewModel<AsistenciaCulto>({ repo.asistencias(it) })

class MovimientosViewModel(repo: ResidenteRepository) : ListaViewModel<MovimientoRegistrado>({ repo.movimientos(it) })

class MisSolicitudesViewModel(repo: PermisoRepository) : ListaViewModel<SolicitudPermiso>({ repo.mias(it) })

// ---------------------------------------------------------------- perfil + QR

/** El perfil es obligatorio; el QR puede fallar sin impedir ver el perfil. */
data class PerfilUi(val perfil: Perfil, val qr: QrPersonal?, val errorQr: ErrorApi?)

class PerfilViewModel(private val repo: ResidenteRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<PerfilUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<PerfilUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch {
            val (perfil, qr) = coroutineScope { async { repo.perfil() } to async { repo.qr() } }
            val p = perfil.await()
            val q = qr.await()
            _estado.value = p.fold(
                { Carga.Exito(PerfilUi(it, q.getOrNull(), q.exceptionOrNull()?.aErrorApi())) },
                { Carga.Fallo(it.aErrorApi()) },
            )
        }
    }
}

// ---------------------------------------------------------------- inicio

data class InicioResidenteUi(
    val perfil: Perfil,
    val limpieza: ResumenLimpieza?,
    val ultimaAsistencia: AsistenciaCulto?,
    val permisoReciente: SolicitudPermiso?,
)

class InicioResidenteViewModel(private val residente: ResidenteRepository, private val permisos: PermisoRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<InicioResidenteUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<InicioResidenteUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch {
            coroutineScope {
                val perfil = async { residente.perfil() }
                val limpieza = async { residente.resumenLimpieza() }
                val asistencias = async { residente.asistencias(0) }
                val solicitudes = async { permisos.mias(0) }
                _estado.value = perfil.await().fold(
                    {
                        Carga.Exito(
                            InicioResidenteUi(
                                it, limpieza.await().getOrNull(), asistencias.await().getOrNull()?.contenido?.firstOrNull(),
                                solicitudes.await().getOrNull()?.contenido?.firstOrNull(),
                            ),
                        )
                    },
                    { Carga.Fallo(it.aErrorApi()) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- limpieza

data class LimpiezaUi(val resumen: ResumenLimpieza, val evaluaciones: List<EvaluacionLimpieza>)

class LimpiezaResidenteViewModel(private val repo: ResidenteRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<LimpiezaUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<LimpiezaUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch {
            coroutineScope {
                val resumen = async { repo.resumenLimpieza() }
                val lista = async { repo.limpieza(0) }
                val r = resumen.await()
                val l = lista.await()
                _estado.value = when {
                    r.isFailure -> Carga.Fallo(r.exceptionOrNull()!!.aErrorApi())
                    l.isFailure -> Carga.Fallo(l.exceptionOrNull()!!.aErrorApi())
                    else -> Carga.Exito(LimpiezaUi(r.getOrThrow(), l.getOrThrow().contenido))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- permisos: formulario y detalle

data class SolicitudFormState(
    val tipo: TipoPermiso = TipoPermiso.Corto,
    val motivo: String = "",
    val destino: String = "",
    val salida: kotlin.time.Instant? = null,
    val retorno: kotlin.time.Instant? = null,
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val creada: SolicitudPermiso? = null,
)

class SolicitudFormViewModel(private val repo: PermisoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(SolicitudFormState())
    val estado: StateFlow<SolicitudFormState> = _estado.asStateFlow()

    fun tipo(v: TipoPermiso) = _estado.update { it.copy(tipo = v) }
    fun motivo(v: String) = _estado.update { it.copy(motivo = v.take(300), errores = it.errores - "motivo", error = null) }
    fun destino(v: String) = _estado.update { it.copy(destino = v.take(200), errores = it.errores - "destino", error = null) }
    fun salida(v: kotlin.time.Instant) = _estado.update { it.copy(salida = v, errores = it.errores - "salida", error = null) }
    fun retorno(v: kotlin.time.Instant) = _estado.update { it.copy(retorno = v, errores = it.errores - "retorno", error = null) }

    /** Valida lo básico en el cliente; las reglas reales (duración, anticipación, superposición) las aplica el servidor. */
    fun enviar() {
        val s = _estado.value
        if (s.enviando) return
        val errores = buildMap {
            if (s.motivo.isBlank()) put("motivo", "Indica el motivo")
            if (s.destino.isBlank()) put("destino", "Indica el destino")
            if (s.salida == null) put("salida", "Elige fecha y hora de salida")
            else if (s.salida < ahora().minus(kotlin.time.Duration.parse("5m"))) put("salida", "La salida no puede estar en el pasado")
            if (s.retorno == null) put("retorno", "Elige fecha y hora de retorno")
            else if (s.salida != null && s.retorno <= s.salida) put("retorno", "El retorno debe ser posterior a la salida")
        }
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        _estado.update { it.copy(enviando = true, error = null, errores = emptyMap()) }
        viewModelScope.launch {
            repo.crear(NuevaSolicitud(s.tipo, s.motivo.trim(), s.destino.trim(), s.salida!!, s.retorno!!))
                .onSuccess { c -> _estado.update { it.copy(enviando = false, creada = c) } }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _estado.update {
                        if (err is ErrorApi.Validacion) it.copy(enviando = false, errores = err.porCampo)
                        else it.copy(enviando = false, error = err)
                    }
                }
        }
    }
}

data class DetalleAccionState(val trabajando: Boolean = false, val error: ErrorApi? = null, val mensaje: String? = null)

class SolicitudDetalleViewModel(private val id: Long, private val repo: PermisoRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<SolicitudPermiso>>(Carga.Cargando)
    val estado: StateFlow<Carga<SolicitudPermiso>> = _estado.asStateFlow()
    private val _accion = MutableStateFlow(DetalleAccionState())
    val accion: StateFlow<DetalleAccionState> = _accion.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch { _estado.value = repo.detalle(id).aCarga() }
    }

    /** Al volver a la pantalla se descarta lo anterior y se pide el estado vigente. */
    fun recargar() {
        _accion.value = DetalleAccionState()
        cargar()
    }

    fun cancelar() = ejecutar("Solicitud cancelada") { repo.cancelar(id).map { } }

    fun justificar(motivo: String) {
        if (motivo.isBlank()) {
            _accion.value = DetalleAccionState(error = ErrorApi.Validacion(mapOf("motivo" to "Cuéntanos qué ocurrió")))
            return
        }
        ejecutar("Justificación enviada para revisión") { repo.justificar(id, motivo.trim()) }
    }

    private fun ejecutar(exito: String, bloque: suspend () -> Result<Unit>) {
        if (_accion.value.trabajando) return
        _accion.value = DetalleAccionState(trabajando = true)
        viewModelScope.launch {
            bloque()
                .onSuccess { _accion.value = DetalleAccionState(mensaje = exito); cargar() }
                .onFailure { _accion.value = DetalleAccionState(error = it.aErrorApi()) }
        }
    }
}
