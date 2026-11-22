package pe.upeu.resi.resiup.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto
import pe.upeu.resi.resiup.data.remote.aApi
import pe.upeu.resi.resiup.data.remote.aDominio
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.*
import pe.upeu.resi.resiup.domain.repository.AccionesPastorRepository
import pe.upeu.resi.resiup.domain.repository.AdministracionRepository

private const val B = "api/v1/"

@Serializable private data class RevocacionDto(val motivo: String)
@Serializable private data class EliminarLimpiezaDto(val fecha: String, val motivo: String)
@Serializable private data class MotivoDto(val motivo: String)

class AdministracionRepositoryImpl(private val c: HttpClient) : AdministracionRepository {
    override suspend fun residentes() = ejecutarLlamada { c.get("${B}residentes") { parameter("pagina", 0); parameter("tamanio", 100) }.body<PaginaDto<ResidenteAdmin>>().contenido }
    override suspend fun habitaciones() = ejecutarLlamada { c.get("${B}habitaciones").body<List<HabAdmin>>() }
    override suspend fun ocupantes(habitacionId: Long) = ejecutarLlamada { c.get("${B}habitaciones/$habitacionId/ocupantes").body<List<OcupanteAdmin>>() }
    override suspend fun semestres() = ejecutarLlamada { c.get("${B}semestres").body<List<SemestreAdmin>>() }
    override suspend fun semestreActual() = ejecutarLlamada { c.get("${B}semestres/actual").body<SemestreAdmin>() }
    override suspend fun terminarSemestre() = ejecutarLlamada { c.post("${B}semestres/terminar"); Unit }
    override suspend fun crearSemestre(s: NuevoSemestreAdmin) = ejecutarLlamada { c.post("${B}semestres") { setBody(s) }.body<SemestreAdmin>() }
    override suspend fun liberar(id: Long) = ejecutarLlamada { c.post("${B}residentes/$id/liberar-habitacion"); Unit }
    override suspend fun crear(r: NuevoResidenteAdmin) = ejecutarLlamada { c.post("${B}residentes") { setBody(r) }.body<ResidenteAdmin>() }
    override suspend fun asignar(id: Long, a: AsignacionAdmin) = ejecutarLlamada { c.post("${B}residentes/$id/asignacion") { setBody(a) }; Unit }
    override suspend fun autorizaciones(id: Long) = ejecutarLlamada { c.get("${B}residentes/$id/autorizaciones").body<List<AutorizacionAdmin>>() }
    override suspend fun autorizar(id: Long, a: NuevaAutorizacionAdmin) = ejecutarLlamada { c.post("${B}residentes/$id/autorizaciones") { setBody(a) }; Unit }
    override suspend fun revocar(id: Long, motivo: String) = ejecutarLlamada { c.post("${B}autorizaciones/$id/revocar") { setBody(RevocacionDto(motivo)) }; Unit }
}

class AccionesPastorRepositoryImpl(private val c: HttpClient) : AccionesPastorRepository {
    override suspend fun eliminarLimpieza(habitacionId: Long, fecha: String, motivo: String) = ejecutarLlamada { c.post("${B}limpieza/habitaciones/$habitacionId/eliminar") { setBody(EliminarLimpiezaDto(fecha, motivo)) }; Unit }
    override suspend fun eliminarLimpiezaDelDia(fecha: String, motivo: String) = ejecutarLlamada { c.post("${B}limpieza/eliminar-dia") { setBody(EliminarLimpiezaDto(fecha, motivo)) }; Unit }
    override suspend fun eliminarCulto(sesionId: Long) = ejecutarLlamada { c.delete("${B}cultos/sesiones/$sesionId"); Unit }
    override suspend fun cancelarReserva(id: Long, motivo: String) = ejecutarLlamada { c.post("${B}reservas/$id/cancelar-pastor") { setBody(MotivoDto(motivo)) }; Unit }
}

class ReservasRepositoryImpl(private val api: pe.upeu.resi.resiup.data.remote.ReservasApi) : pe.upeu.resi.resiup.domain.repository.ReservasRepository {
    override suspend fun oferta() = ejecutarLlamada { api.oferta().comidas.map { it.aDominio() } }
    override suspend fun mias() = ejecutarLlamada { api.mias(0).contenido.map { it.aDominio() } }
    override suspend fun reservar(n: NuevaReserva) = ejecutarLlamada { api.reservar(pe.upeu.resi.resiup.data.remote.ReservaRequestDto(n.tipo.aApi(), n.fecha, n.motivo, n.curso, n.evidenciaId)).aDominio() }
    override suspend fun cancelar(id: Long) = ejecutarLlamada { api.cancelar(id).aDominio() }
}
