package pe.upeu.resi.resiup.presentation.sesion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.domain.usecase.CerrarSesionUseCase
import pe.upeu.resi.resiup.domain.usecase.ObtenerSesionGuardadaUseCase
import pe.upeu.resi.resiup.data.remote.SesionEventos

sealed interface EstadoSesion {
    /** Todavía se está leyendo la sesión guardada (mantiene el splash). */
    data object Arrancando : EstadoSesion
    data class SinSesion(val aviso: String? = null) : EstadoSesion
    data class Activa(val sesion: Sesion) : EstadoSesion
}

/** Fuente única del estado de sesión de la app. */
class GestorSesion(
    private val obtenerGuardada: ObtenerSesionGuardadaUseCase,
    private val cerrar: CerrarSesionUseCase,
    val eventos: SesionEventos,
) {
    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Arrancando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    suspend fun restaurar() {
        if (_estado.value != EstadoSesion.Arrancando) return
        val sesion = obtenerGuardada()
        _estado.value = if (sesion != null) EstadoSesion.Activa(sesion) else EstadoSesion.SinSesion()
    }

    fun sesionIniciada(sesion: Sesion) {
        _estado.value = EstadoSesion.Activa(sesion)
    }

    suspend fun cerrarSesion(aviso: String? = null) {
        cerrar()
        _estado.value = EstadoSesion.SinSesion(aviso)
    }

    /** El servidor devolvió 401: se elimina la sesión local y se vuelve al login. */
    suspend fun sesionExpirada() {
        if (_estado.value is EstadoSesion.Activa) cerrarSesion("Tu sesión expiró. Inicia sesión nuevamente.")
    }
}
