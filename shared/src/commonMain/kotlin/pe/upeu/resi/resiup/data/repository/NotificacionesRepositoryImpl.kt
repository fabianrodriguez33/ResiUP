package pe.upeu.resi.resiup.data.repository

import kotlin.time.Instant
import pe.upeu.resi.resiup.data.remote.NotificacionesApi
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.Notificacion
import pe.upeu.resi.resiup.domain.repository.NotificacionesRepository

class NotificacionesRepositoryImpl(private val api: NotificacionesApi) : NotificacionesRepository {
    override suspend fun recientes() = ejecutarLlamada {
        api.recientes().contenido.map { Notificacion(it.id, it.titulo, it.mensaje, it.leida, Instant.parse(it.creadaEn)) }
    }

    override suspend fun cantidadNoLeidas() = ejecutarLlamada { api.noLeidas().cantidad }
    override suspend fun marcarLeida(id: Long) = ejecutarLlamada { api.marcarLeida(id) }
    override suspend fun marcarTodasLeidas() = ejecutarLlamada { api.marcarTodas() }
}
