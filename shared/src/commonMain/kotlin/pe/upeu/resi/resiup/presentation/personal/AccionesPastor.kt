package pe.upeu.resi.resiup.presentation.personal

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable private data class EliminarLimpiezaDto(val fecha: String, val motivo: String)
@Serializable private data class MotivoDto(val motivo: String)

/** Acciones de corrección del pastor: eliminar la evaluación de una habitación o un culto, y cancelar una reserva (todas con constancia en el servidor). */
class AccionesPastorApi(private val c: HttpClient) {
    suspend fun eliminarLimpieza(habitacionId: Long, fecha: String, motivo: String) {
        c.post("api/v1/limpieza/habitaciones/$habitacionId/eliminar") { setBody(EliminarLimpiezaDto(fecha, motivo)) }
    }
    suspend fun eliminarLimpiezaDelDia(fecha: String, motivo: String) { c.post("api/v1/limpieza/eliminar-dia") { setBody(EliminarLimpiezaDto(fecha, motivo)) } }
    suspend fun eliminarCulto(sesionId: Long) { c.delete("api/v1/cultos/sesiones/$sesionId") }
    suspend fun cancelarReserva(id: Long, motivo: String) { c.post("api/v1/reservas/$id/cancelar-pastor") { setBody(MotivoDto(motivo)) } }
}
