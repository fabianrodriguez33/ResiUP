package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto
import pe.upeu.resi.resiup.domain.model.ComidaOfertada
import pe.upeu.resi.resiup.domain.model.EstadoReserva
import pe.upeu.resi.resiup.domain.model.Reserva
import pe.upeu.resi.resiup.domain.model.TipoReserva
import kotlin.time.Instant

@Serializable
data class ReservaDto(
    val id: Long, val tipo: String, val fecha: String, val motivo: String = "", val curso: String? = null, val observacion: String? = null,
    val estado: String, val creadaEn: String, val motivoCancelacion: String? = null,
)

@Serializable
data class OfertaItemDto(val tipo: String, val fecha: String, val puedeReservar: Boolean = false, val motivoNoDisponible: String? = null, val reservaActiva: ReservaDto? = null)

@Serializable
data class OfertaDto(val comidas: List<OfertaItemDto> = emptyList())

@Serializable
data class ReservaRequestDto(val tipo: String, val fecha: String, val motivo: String, val curso: String? = null)

@Serializable
data class ReservaPastorDto(
    val id: Long, val residenteNombre: String = "", val codigo: String = "", val tipo: String, val fecha: String,
    val motivo: String = "", val curso: String? = null, val estado: String, val comentarioDecision: String? = null,
)

@Serializable
data class DecisionReservaDto(val aprobar: Boolean, val comentario: String? = null)

private const val BASE = "api/v1/reservas"

class ReservasApi(private val client: HttpClient) {
    suspend fun oferta(): OfertaDto = client.get("$BASE/oferta").body()
    suspend fun reservar(r: ReservaRequestDto): ReservaDto = client.post(BASE) { setBody(r) }.body()
    suspend fun cancelar(id: Long): ReservaDto = client.post("$BASE/$id/cancelar").body()
    suspend fun delDia(fecha: String, tipo: String?): List<ReservaPastorDto> =
        client.get("$BASE/del-dia") { parameter("fecha", fecha); if (tipo != null) parameter("tipo", tipo) }.body()
    suspend fun decidir(id: Long, aprobar: Boolean, comentario: String?): ReservaDto = client.post("$BASE/$id/decision") { setBody(DecisionReservaDto(aprobar, comentario)) }.body()
    suspend fun mias(pagina: Int): PaginaDto<ReservaDto> = client.get("$BASE/mias") { parameter("pagina", pagina); parameter("tamanio", 20) }.body()
}

internal fun String.aTipoReserva() = when (this) {
    "ALMUERZO" -> TipoReserva.Almuerzo
    "DESAYUNO_FRIO" -> TipoReserva.Desayuno
    "CENA_TAPER" -> TipoReserva.CenaTaper
    else -> TipoReserva.CenaSegundoTurno
}

internal fun TipoReserva.aApi() = when (this) {
    TipoReserva.Almuerzo -> "ALMUERZO"
    TipoReserva.Desayuno -> "DESAYUNO_FRIO"
    TipoReserva.CenaTaper -> "CENA_TAPER"
    TipoReserva.CenaSegundoTurno -> "CENA_SEGUNDO_TURNO"
}

internal fun String.aEstadoReserva() = when (this) {
    "PENDIENTE" -> EstadoReserva.Pendiente
    "RESERVADA" -> EstadoReserva.Reservado
    "RECHAZADA" -> EstadoReserva.Rechazada
    "CANCELADA_PASTOR" -> EstadoReserva.CanceladaPastor
    "CANCELADA_CIERRE" -> EstadoReserva.CanceladaCierre
    else -> EstadoReserva.CanceladaResidente
}

internal fun ReservaDto.aDominio() = Reserva(id, tipo.aTipoReserva(), fecha, motivo, curso, observacion, estado.aEstadoReserva(), Instant.parse(creadaEn), motivoCancelacion)

internal fun OfertaItemDto.aDominio() = ComidaOfertada(tipo.aTipoReserva(), fecha, puedeReservar, motivoNoDisponible, reservaActiva?.aDominio())
