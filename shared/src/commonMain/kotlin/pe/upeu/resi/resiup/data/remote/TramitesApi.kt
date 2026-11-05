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

@Serializable
data class HabilitacionDto(val habilitado: Boolean = true, val hasta: String = "", val motivo: String? = null, val faltasNoJustificadas: Int = 0, val promedioLimpieza: Double? = null)

@Serializable
data class FaltaPendienteDto(val residenteId: Long, val codigo: String, val nombre: String, val fecha: String, val plazo: String)

@Serializable
data class JustificarFaltaDto(val residenteId: Long, val fecha: String, val motivo: String)

@Serializable
data class AnularPedidoDto(val motivo: String)

@Serializable
data class PermisoSuspendidoDto(val id: Long, val residenteNombre: String, val residenteCodigo: String, val tipo: String, val destino: String, val salidaSolicitada: String)

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

    suspend fun habilitacion(): HabilitacionDto = client.get("${base}residentes/yo/habilitacion").body()
    suspend fun faltasPorJustificar(): List<FaltaPendienteDto> = client.get("${base}habilitacion/faltas-por-justificar").body()

    suspend fun justificarFalta(r: JustificarFaltaDto) {
        client.post("${base}habilitacion/faltas/justificar") { setBody(r) }
    }

    suspend fun permisosDeSuspendidos(): List<PermisoSuspendidoDto> = client.get("${base}permisos/solicitudes/de-suspendidos").body()

    suspend fun anularPermiso(id: Long, r: AnularPedidoDto) {
        client.post("${base}permisos/solicitudes/$id/anular") { setBody(r) }
    }

    suspend fun decidirJustificacion(id: Long, d: DecisionTramiteDto) {
        client.post("${base}permisos/justificaciones/$id/decision") { setBody(d) }
    }
}
