package pe.upeu.resi.resiup.data.remote

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Canal por el que la capa de red avisa a la app que el servidor rechazó el token (401). */
class SesionEventos {
    private val _expirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val expirada: SharedFlow<Unit> = _expirada.asSharedFlow()

    fun notificarExpirada() {
        _expirada.tryEmit(Unit)
    }
}
