package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto
import pe.upeu.resi.resiup.domain.model.ImagenElegida

@Serializable
data class EvidenciaSubidaDto(val id: Long)

@Serializable
data class TramiteAmpliacionDto(
    val id: Long, val solicitudId: Long, val motivo: String, val evidenciaId: Long? = null, val retornoOriginal: String, val retornoNuevo: String,
    val residenteNombre: String? = null, val residenteCodigo: String? = null,
)

@Serializable
data class TramiteJustificacionDto(
    val id: Long, val solicitudId: Long, val motivo: String, val evidenciaId: Long? = null,
    val residenteNombre: String? = null, val residenteCodigo: String? = null,
)

@Serializable
data class AmpliacionPedidoDto(val motivo: String, val retornoNuevo: String, val evidenciaId: Long? = null)

@Serializable
data class JustificacionPedidoDto(val motivo: String, val evidenciaId: Long? = null)

@Serializable
data class DecisionTramiteDto(val aprobar: Boolean, val comentario: String? = null)

class TramitesApi(private val client: HttpClient) {
    private val base = "api/v1/"

    suspend fun subirEvidencia(imagen: ImagenElegida): EvidenciaSubidaDto = client.submitFormWithBinaryData(
        url = "${base}evidencias",
        formData = formData {
            append("archivo", imagen.bytes, Headers.build {
                append(HttpHeaders.ContentType, imagen.tipo)
                append(HttpHeaders.ContentDisposition, "filename=\"${imagen.nombre}\"")
            })
        },
    ).body()

    suspend fun descargarEvidencia(id: Long): ByteArray = client.get("${base}evidencias/$id").body()

    suspend fun ampliar(solicitudId: Long, r: AmpliacionPedidoDto) {
        client.post("${base}permisos/solicitudes/$solicitudId/ampliaciones") { setBody(r) }
    }

    suspend fun justificar(solicitudId: Long, r: JustificacionPedidoDto) {
        client.post("${base}permisos/solicitudes/$solicitudId/justificaciones") { setBody(r) }
    }

    suspend fun ampliaciones(): PaginaDto<TramiteAmpliacionDto> = client.get("${base}permisos/ampliaciones/pendientes") { parameter("tamanio", 50) }.body()
    suspend fun justificaciones(): PaginaDto<TramiteJustificacionDto> = client.get("${base}permisos/justificaciones/pendientes") { parameter("tamanio", 50) }.body()

    suspend fun decidirAmpliacion(id: Long, d: DecisionTramiteDto) {
        client.post("${base}permisos/ampliaciones/$id/decision") { setBody(d) }
    }

    suspend fun decidirJustificacion(id: Long, d: DecisionTramiteDto) {
        client.post("${base}permisos/justificaciones/$id/decision") { setBody(d) }
    }
}
