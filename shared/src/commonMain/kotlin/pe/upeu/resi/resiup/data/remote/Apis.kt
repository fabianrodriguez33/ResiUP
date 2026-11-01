package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pe.upeu.resi.resiup.data.remote.dto.AbrirSesionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.AnulacionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.AsistenciaDto
import pe.upeu.resi.resiup.data.remote.dto.PerfilRequestDto
import pe.upeu.resi.resiup.data.remote.dto.EnlaceDto
import pe.upeu.resi.resiup.data.remote.dto.EvaluacionDto
import pe.upeu.resi.resiup.data.remote.dto.JustificacionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.DecisionApoderadoRequestDto
import pe.upeu.resi.resiup.data.remote.dto.HabitacionLimpiezaDto
import pe.upeu.resi.resiup.data.remote.dto.HijoDto
import pe.upeu.resi.resiup.data.remote.dto.ResumenResidenteDto
import pe.upeu.resi.resiup.data.remote.dto.LimpiezaHabitacionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.MovimientoDto
import pe.upeu.resi.resiup.data.remote.dto.MovimientoRequestDto
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto
import pe.upeu.resi.resiup.data.remote.dto.QrDto
import pe.upeu.resi.resiup.data.remote.dto.QrRequestDto
import pe.upeu.resi.resiup.data.remote.dto.ResidenteDto
import pe.upeu.resi.resiup.data.remote.dto.ResumenLimpiezaDto
import pe.upeu.resi.resiup.data.remote.dto.SesionCultoDto
import pe.upeu.resi.resiup.data.remote.dto.SolicitudDto
import pe.upeu.resi.resiup.data.remote.dto.SolicitudRequestDto
import pe.upeu.resi.resiup.data.remote.dto.VerificacionDto

private const val BASE = "api/v1/"

class ResidenteApi(private val client: HttpClient) {
    suspend fun perfil(): ResidenteDto = client.get("${BASE}residentes/yo").body()
    suspend fun qr(): QrDto = client.get("${BASE}residentes/yo/qr").body()
    suspend fun actualizarPerfil(r: PerfilRequestDto): ResidenteDto = client.patch("${BASE}residentes/yo") { setBody(r) }.body()
    suspend fun asistencias(pagina: Int, tamanio: Int): PaginaDto<AsistenciaDto> =
        client.get("${BASE}residentes/yo/asistencias") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
    suspend fun limpieza(pagina: Int, tamanio: Int): PaginaDto<EvaluacionDto> =
        client.get("${BASE}residentes/yo/limpieza") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
    suspend fun resumenLimpieza(): ResumenLimpiezaDto = client.get("${BASE}residentes/yo/limpieza/resumen").body()
    suspend fun movimientos(pagina: Int, tamanio: Int): PaginaDto<MovimientoDto> =
        client.get("${BASE}residentes/yo/movimientos") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
    suspend fun listar(pagina: Int, tamanio: Int): PaginaDto<ResidenteDto> =
        client.get("${BASE}residentes") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
}

class PermisoApi(private val client: HttpClient) {
    suspend fun crear(r: SolicitudRequestDto): SolicitudDto = client.post("${BASE}permisos/solicitudes") { setBody(r) }.body()
    suspend fun mias(pagina: Int, tamanio: Int): PaginaDto<SolicitudDto> =
        client.get("${BASE}permisos/solicitudes/mias") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
    suspend fun detalle(id: Long): SolicitudDto = client.get("${BASE}permisos/solicitudes/$id").body()
    suspend fun cancelar(id: Long): SolicitudDto = client.post("${BASE}permisos/solicitudes/$id/cancelar").body()
    suspend fun justificar(id: Long, r: JustificacionRequestDto) {
        client.post("${BASE}permisos/solicitudes/$id/justificaciones") { setBody(r) }
    }
    suspend fun listar(estado: String?, pagina: Int, tamanio: Int): PaginaDto<SolicitudDto> =
        client.get("${BASE}permisos/solicitudes") {
            estado?.let { parameter("estado", it) }
            parameter("pagina", pagina); parameter("tamanio", tamanio)
        }.body()
    suspend fun enlaceApoderado(id: Long): EnlaceDto = client.post("${BASE}permisos/solicitudes/$id/enlace-apoderado").body()
    suspend fun resumen(id: Long): ResumenResidenteDto = client.get("${BASE}permisos/solicitudes/$id/resumen-residente").body()
    suspend fun anular(id: Long, motivo: String): SolicitudDto =
        client.post("${BASE}permisos/solicitudes/$id/anular") { setBody(AnulacionRequestDto(motivo)) }.body()
}

class FamiliaApi(private val client: HttpClient) {
    suspend fun hijos(): List<HijoDto> = client.get("${BASE}familia/hijos").body()
    suspend fun solicitudes(estado: String?, pagina: Int, tamanio: Int): PaginaDto<SolicitudDto> =
        client.get("${BASE}familia/solicitudes") {
            estado?.let { parameter("estado", it) }
            parameter("pagina", pagina); parameter("tamanio", tamanio)
        }.body()
    suspend fun detalle(id: Long): SolicitudDto = client.get("${BASE}familia/solicitudes/$id").body()
    suspend fun crear(r: SolicitudRequestDto): SolicitudDto = client.post("${BASE}familia/solicitudes") { setBody(r) }.body()
    suspend fun decidir(id: Long, aprobar: Boolean): SolicitudDto =
        client.post("${BASE}familia/solicitudes/$id/decision") { setBody(DecisionApoderadoRequestDto(aprobar)) }.body()
    suspend fun cancelar(id: Long): SolicitudDto = client.post("${BASE}familia/solicitudes/$id/cancelar").body()
}

class CultoApi(private val client: HttpClient) {
    suspend fun abrir(tipo: String): SesionCultoDto = client.post("${BASE}cultos/sesiones") { setBody(AbrirSesionRequestDto(tipo)) }.body()
    suspend fun hoy(): List<SesionCultoDto> = client.get("${BASE}cultos/sesiones/hoy").body()
    suspend fun registrar(sesionId: Long, qr: String): AsistenciaDto =
        client.post("${BASE}cultos/sesiones/$sesionId/asistencias") { setBody(QrRequestDto(qr)) }.body()
    suspend fun cerrar(sesionId: Long): SesionCultoDto = client.post("${BASE}cultos/sesiones/$sesionId/cerrar").body()
}

class LimpiezaApi(private val client: HttpClient) {
    suspend fun habitaciones(fecha: String): List<HabitacionLimpiezaDto> = client.get("${BASE}limpieza/habitaciones") { parameter("fecha", fecha) }.body()
    suspend fun registrarHabitacion(id: Long, r: LimpiezaHabitacionRequestDto): List<EvaluacionDto> =
        client.post("${BASE}limpieza/habitaciones/$id/evaluaciones") { setBody(r) }.body()
}

class GaritaApi(private val client: HttpClient) {
    suspend fun verificar(qr: String): VerificacionDto = client.post("${BASE}garita/verificaciones") { setBody(QrRequestDto(qr)) }.body()
    suspend fun registrar(qr: String, tipo: String): MovimientoDto =
        client.post("${BASE}garita/movimientos") { setBody(MovimientoRequestDto(qr, tipo)) }.body()
    suspend fun historial(pagina: Int, tamanio: Int): PaginaDto<MovimientoDto> =
        client.get("${BASE}garita/movimientos") { parameter("pagina", pagina); parameter("tamanio", tamanio) }.body()
}
