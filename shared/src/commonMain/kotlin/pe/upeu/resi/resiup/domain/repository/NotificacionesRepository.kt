package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.Notificacion

/** Bandeja de avisos del usuario. Son operaciones en línea. */
interface NotificacionesRepository {
    suspend fun recientes(): Result<List<Notificacion>>
    suspend fun cantidadNoLeidas(): Result<Int>
    suspend fun marcarLeida(id: Long): Result<Unit>
    suspend fun marcarTodasLeidas(): Result<Unit>
}
