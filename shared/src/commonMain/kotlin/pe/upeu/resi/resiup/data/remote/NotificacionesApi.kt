package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.serialization.Serializable
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto

@Serializable
data class NotificacionDto(val id: Long, val titulo: String, val mensaje: String, val leida: Boolean = false, val creadaEn: String)

@Serializable
data class NoLeidasDto(val cantidad: Int = 0)

class NotificacionesApi(private val client: HttpClient) {
    suspend fun recientes(): PaginaDto<NotificacionDto> = client.get("api/v1/notificaciones") {
        parameter("pagina", 0)
        parameter("tamanio", 30)
    }.body()

    suspend fun noLeidas(): NoLeidasDto = client.get("api/v1/notificaciones/no-leidas").body()

    suspend fun marcarLeida(id: Long) {
        client.post("api/v1/notificaciones/$id/leida")
    }

    suspend fun marcarTodas() {
        client.post("api/v1/notificaciones/leidas")
    }
}
