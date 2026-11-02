package pe.upeu.resi.resiup.data.repository

import kotlin.time.Instant
import pe.upeu.resi.resiup.data.remote.AmpliacionPedidoDto
import pe.upeu.resi.resiup.data.remote.DecisionTramiteDto
import pe.upeu.resi.resiup.data.remote.JustificacionPedidoDto
import pe.upeu.resi.resiup.data.remote.TramitesApi
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.AmpliacionPendiente
import pe.upeu.resi.resiup.domain.model.ImagenElegida
import pe.upeu.resi.resiup.domain.model.JustificacionPendiente
import pe.upeu.resi.resiup.domain.repository.TramitesRepository

class TramitesRepositoryImpl(private val api: TramitesApi) : TramitesRepository {
    override suspend fun subirEvidencia(imagen: ImagenElegida) = ejecutarLlamada { api.subirEvidencia(imagen).id }
    override suspend fun descargarEvidencia(id: Long) = ejecutarLlamada { api.descargarEvidencia(id) }

    override suspend fun ampliar(solicitudId: Long, motivo: String, retornoNuevo: Instant, evidenciaId: Long?) =
        ejecutarLlamada { api.ampliar(solicitudId, AmpliacionPedidoDto(motivo.trim(), retornoNuevo.toString(), evidenciaId)) }

    override suspend fun justificar(solicitudId: Long, motivo: String, evidenciaId: Long?) =
        ejecutarLlamada { api.justificar(solicitudId, JustificacionPedidoDto(motivo.trim(), evidenciaId)) }

    override suspend fun ampliacionesPendientes() = ejecutarLlamada {
        api.ampliaciones().contenido.map {
            AmpliacionPendiente(it.id, it.solicitudId, it.residenteNombre.orEmpty(), it.residenteCodigo.orEmpty(), it.motivo,
                Instant.parse(it.retornoOriginal), Instant.parse(it.retornoNuevo), it.evidenciaId)
        }
    }

    override suspend fun justificacionesPendientes() = ejecutarLlamada {
        api.justificaciones().contenido.map {
            JustificacionPendiente(it.id, it.solicitudId, it.residenteNombre.orEmpty(), it.residenteCodigo.orEmpty(), it.motivo, it.evidenciaId)
        }
    }

    override suspend fun decidirAmpliacion(id: Long, aprobar: Boolean, comentario: String?) =
        ejecutarLlamada { api.decidirAmpliacion(id, DecisionTramiteDto(aprobar, comentario?.takeIf { it.isNotBlank() })) }

    override suspend fun decidirJustificacion(id: Long, aprobar: Boolean, comentario: String?) =
        ejecutarLlamada { api.decidirJustificacion(id, DecisionTramiteDto(aprobar, comentario?.takeIf { it.isNotBlank() })) }
}
