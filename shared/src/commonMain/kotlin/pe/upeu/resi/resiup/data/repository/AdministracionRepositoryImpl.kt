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
    /** El servidor entrega como máximo 100 por página: se piden todas hasta la última para no cortar la lista. */
    private suspend inline fun <reified T> todasLasPaginas(ruta: String): List<T> {
        val todo = mutableListOf<T>()
        var pagina = 0
        do {
            val r = c.get("$B$ruta") { parameter("pagina", pagina); parameter("tamanio", 100) }.body<PaginaDto<T>>()
            todo += r.contenido
            pagina++
        } while (!r.ultima && r.contenido.isNotEmpty())
        return todo
    }

    override suspend fun residentes() = ejecutarLlamada { todasLasPaginas<ResidenteAdmin>("residentes") }
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
    override suspend fun usuarios() = ejecutarLlamada { todasLasPaginas<UsuarioAdmin>("usuarios") }
    override suspend fun crearUsuario(u: NuevoUsuarioAdmin) = ejecutarLlamada { c.post("${B}usuarios") { setBody(u) }.body<UsuarioAdmin>() }
}

class AccionesPastorRepositoryImpl(private val c: HttpClient) : AccionesPastorRepository {
    override suspend fun eliminarLimpieza(habitacionId: Long, fecha: String, motivo: String) = ejecutarLlamada { c.post("${B}limpieza/habitaciones/$habitacionId/eliminar") { setBody(EliminarLimpiezaDto(fecha, motivo)) }; Unit }
    override suspend fun eliminarLimpiezaDelDia(fecha: String, motivo: String) = ejecutarLlamada { c.post("${B}limpieza/eliminar-dia") { setBody(EliminarLimpiezaDto(fecha, motivo)) }; Unit }
    override suspend fun eliminarCulto(sesionId: Long) = ejecutarLlamada { c.delete("${B}cultos/sesiones/$sesionId"); Unit }
    override suspend fun cancelarReserva(id: Long, motivo: String) = ejecutarLlamada { c.post("${B}reservas/$id/cancelar-pastor") { setBody(MotivoDto(motivo)) }; Unit }
}

class ReservasRepositoryImpl(private val api: pe.upeu.resi.resiup.data.remote.ReservasApi, private val locales: pe.upeu.resi.resiup.data.local.DatosLocales) : pe.upeu.resi.resiup.domain.repository.ReservasRepository {
    override suspend fun oferta() = conRespaldoLocal(locales, "reservas:oferta", pe.upeu.resi.resiup.data.remote.OfertaDto.serializer()) { api.oferta() }.map { o -> o.comidas.map { it.aDominio() } }
    override suspend fun mias() = conRespaldoLocal(locales, "reservas:mias", kotlinx.serialization.builtins.ListSerializer(pe.upeu.resi.resiup.data.remote.ReservaDto.serializer())) { api.todasMias() }.map { l -> l.map { it.aDominio() } }
    override suspend fun reservar(n: NuevaReserva) = ejecutarLlamada { api.reservar(pe.upeu.resi.resiup.data.remote.ReservaRequestDto(n.tipo.aApi(), n.fecha, n.motivo, n.curso, n.evidenciaId)).aDominio() }
    override suspend fun cancelar(id: Long) = ejecutarLlamada { api.cancelar(id).aDominio() }
}
