package pe.upeu.resi.resiup.presentation.residente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
import pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.Consulta
import pe.upeu.resi.resiup.presentation.util.ListaViewModel
import pe.upeu.resi.resiup.presentation.util.Sincronizador
import pe.upeu.resi.resiup.presentation.util.comoConsulta
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.ahora

class MovimientosViewModel(repo: ResidenteRepository) : ListaViewModel<MovimientoRegistrado>({ repo.movimientos(it) })

// Las pantallas de consulta leen la copia local (SQLDelight) y se actualizan solas al sincronizar.
private fun <T> ViewModel.compartir(flujo: Flow<Consulta<T>>): StateFlow<Consulta<T>> =
    flujo.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Consulta.Cargando)

class AsistenciasViewModel(private val consultas: ConsultasResidenteRepository, private val sync: Sincronizador) : ViewModel() {
    val estado = compartir(consultas.asistencias().comoConsulta(sync.estado, consultas.ultimaSincronizacion()))
    init { sync.refrescar() }
    fun reintentar() = sync.refrescar(forzar = true)
}

class MisSolicitudesViewModel(private val consultas: ConsultasResidenteRepository, private val sync: Sincronizador) : ViewModel() {
    val estado = compartir(consultas.solicitudes().comoConsulta(sync.estado, consultas.ultimaSincronizacion()))
    init { sync.refrescar() }
    fun reintentar() = sync.refrescar(forzar = true)
}

// ---------------------------------------------------------------- perfil + QR

/** El QR se pide al servidor y, sin red, se usa la copia cifrada del teléfono (el servidor valida y revoca en garita). */
class PerfilViewModel(
    private val consultas: ConsultasResidenteRepository,
    private val residente: ResidenteRepository,
    private val sync: Sincronizador,
) : ViewModel() {
    val estado = compartir(consultas.perfil().comoConsulta(sync.estado, consultas.ultimaSincronizacion()))

    private val _qr = MutableStateFlow<Carga<QrPersonal>>(Carga.Cargando)
    val qr: StateFlow<Carga<QrPersonal>> = _qr.asStateFlow()

    init {
        sync.refrescar()
        cargarQr()
    }

    fun cargarQr() {
        _qr.value = Carga.Cargando
        viewModelScope.launch { _qr.value = residente.qr().aCarga() }
    }

    fun reintentar() {
        sync.refrescar(forzar = true)
        cargarQr()
    }
}

// ---------------------------------------------------------------- inicio

data class InicioResidenteUi(
    val perfil: Perfil,
    val limpieza: ResumenLimpieza?,
    val ultimaAsistencia: AsistenciaCulto?,
    val permisoReciente: SolicitudPermiso?,
)

class InicioResidenteViewModel(private val consultas: ConsultasResidenteRepository, private val sync: Sincronizador) : ViewModel() {
    val estado = compartir(
        combine(consultas.perfil(), consultas.resumenLimpieza(), consultas.asistencias(), consultas.solicitudes()) { perfil, resumen, asistencias, solicitudes ->
            perfil?.let { InicioResidenteUi(it, resumen, asistencias.firstOrNull(), solicitudes.firstOrNull()) }
        }.comoConsulta(sync.estado, consultas.ultimaSincronizacion()),
    )
    init { sync.refrescar() }
    fun reintentar() = sync.refrescar(forzar = true)
}

// ---------------------------------------------------------------- limpieza

data class LimpiezaUi(val resumen: ResumenLimpieza, val evaluaciones: List<EvaluacionLimpieza>)

class LimpiezaResidenteViewModel(private val consultas: ConsultasResidenteRepository, private val sync: Sincronizador) : ViewModel() {
    val estado = compartir(
        combine(consultas.resumenLimpieza(), consultas.evaluaciones()) { resumen, lista ->
            LimpiezaUi(resumen ?: ResumenLimpieza(0, null, null, null), lista)
        }.comoConsulta(sync.estado, consultas.ultimaSincronizacion()),
    )
    init { sync.refrescar() }
    fun reintentar() = sync.refrescar(forzar = true)
}

// ---------------------------------------------------------------- permisos: formulario y detalle

data class DetalleAccionState(val trabajando: Boolean = false, val error: ErrorApi? = null, val mensaje: String? = null)

class SolicitudDetalleViewModel(private val id: Long, private val repo: PermisoRepository, private val alCambiar: () -> Unit = {}) : ViewModel() {
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
                .onSuccess { _accion.value = DetalleAccionState(mensaje = exito); alCambiar(); cargar() }
                .onFailure { _accion.value = DetalleAccionState(error = it.aErrorApi()) }
        }
    }
}
