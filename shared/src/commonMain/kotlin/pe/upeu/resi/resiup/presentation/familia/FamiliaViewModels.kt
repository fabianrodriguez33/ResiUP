package pe.upeu.resi.resiup.presentation.familia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.Hijo
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.FamiliaRepository
import pe.upeu.resi.resiup.presentation.residente.PermisoFormViewModel
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ListaViewModel
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi

// ---------------------------------------------------------------- inicio: hijos y permisos por aceptar

data class FamiliaInicioState(
    val hijos: Carga<List<Hijo>> = Carga.Cargando,
    val porAceptar: Carga<List<SolicitudPermiso>> = Carga.Cargando,
    val trabajandoId: Long? = null,
    val mensaje: String? = null,
    val error: ErrorApi? = null,
)

class FamiliaInicioViewModel(private val repo: FamiliaRepository) : ViewModel() {
    private val _estado = MutableStateFlow(FamiliaInicioState())
    val estado: StateFlow<FamiliaInicioState> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.update { it.copy(hijos = Carga.Cargando, porAceptar = Carga.Cargando) }
        viewModelScope.launch {
            val hijos = repo.hijos().aCarga()
            val pendientes = repo.solicitudes(EstadoSolicitud.PendienteApoderado, 0).map { it.contenido }.aCarga()
            _estado.update { it.copy(hijos = hijos, porAceptar = pendientes) }
        }
    }

    /** El padre/madre acepta o rechaza el permiso que pidió su hijo; si acepta, pasa al pastor. */
    fun decidir(id: Long, aprobar: Boolean) {
        if (_estado.value.trabajandoId != null) return
        _estado.update { it.copy(trabajandoId = id, mensaje = null, error = null) }
        viewModelScope.launch {
            repo.decidir(id, aprobar)
                .onSuccess {
                    _estado.update { it.copy(trabajandoId = null, mensaje = if (aprobar) "Permiso aceptado: ya pasó al pastor para su aprobación" else "Permiso rechazado") }
                    cargar()
                }
                .onFailure { e ->
                    _estado.update { it.copy(trabajandoId = null, error = e.aErrorApi()) }
                    cargar()
                }
        }
    }
}

// ---------------------------------------------------------------- historial

class FamiliaSolicitudesViewModel(repo: FamiliaRepository) : ListaViewModel<SolicitudPermiso>({ repo.solicitudes(null, it) })

data class FamiliaAccionState(val trabajando: Boolean = false, val error: ErrorApi? = null, val mensaje: String? = null)

class FamiliaDetalleViewModel(private val id: Long, private val repo: FamiliaRepository, private val alCambiar: () -> Unit = {}) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<SolicitudPermiso>>(Carga.Cargando)
    val estado: StateFlow<Carga<SolicitudPermiso>> = _estado.asStateFlow()
    private val _accion = MutableStateFlow(FamiliaAccionState())
    val accion: StateFlow<FamiliaAccionState> = _accion.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = Carga.Cargando
        viewModelScope.launch { _estado.value = repo.detalle(id).aCarga() }
    }

    fun recargar() {
        _accion.value = FamiliaAccionState()
        cargar()
    }

    fun decidir(aprobar: Boolean) = ejecutar(if (aprobar) "Permiso aceptado: ya pasó al pastor" else "Permiso rechazado") { repo.decidir(id, aprobar) }

    fun cancelar() = ejecutar("Solicitud cancelada") { repo.cancelar(id) }

    private fun ejecutar(exito: String, llamada: suspend () -> Result<SolicitudPermiso>) {
        if (_accion.value.trabajando) return
        _accion.value = FamiliaAccionState(trabajando = true)
        viewModelScope.launch {
            llamada()
                .onSuccess { _accion.value = FamiliaAccionState(mensaje = exito); alCambiar(); cargar() }
                .onFailure { _accion.value = FamiliaAccionState(error = it.aErrorApi()); cargar() }
        }
    }
}

// ---------------------------------------------------------------- nuevo permiso (corto, largo o pernoctar fuera)

class FamiliaNuevoViewModel(private val repo: FamiliaRepository) :
    PermisoFormViewModel(TipoPermiso.entries, exigeHijo = true, enviarSolicitud = { repo.crear(it) }) {
    private val _hijos = MutableStateFlow<Carga<List<Hijo>>>(Carga.Cargando)
    val hijos: StateFlow<Carga<List<Hijo>>> = _hijos.asStateFlow()

    init { cargarHijos() }

    fun cargarHijos() {
        _hijos.value = Carga.Cargando
        viewModelScope.launch {
            val r = repo.hijos()
            _hijos.value = r.aCarga()
            // Con un solo hijo no hace falta preguntar para quién es.
            r.getOrNull()?.singleOrNull()?.let { hijo(it.id) }
        }
    }
}
