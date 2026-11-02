package pe.upeu.resi.resiup.domain.repository

import kotlin.time.Instant
import pe.upeu.resi.resiup.domain.model.AmpliacionPendiente
import pe.upeu.resi.resiup.domain.model.ImagenElegida
import pe.upeu.resi.resiup.domain.model.JustificacionPendiente

/** Ampliaciones de horario y justificaciones de tardanza: las pide el residente (con foto) y las revisa el pastor. En línea. */
interface TramitesRepository {
    /** Sube la foto y devuelve su identificador. */
    suspend fun subirEvidencia(imagen: ImagenElegida): Result<Long>
    suspend fun descargarEvidencia(id: Long): Result<ByteArray>

    suspend fun ampliar(solicitudId: Long, motivo: String, retornoNuevo: Instant, evidenciaId: Long?): Result<Unit>
    suspend fun justificar(solicitudId: Long, motivo: String, evidenciaId: Long?): Result<Unit>

    suspend fun ampliacionesPendientes(): Result<List<AmpliacionPendiente>>
    suspend fun justificacionesPendientes(): Result<List<JustificacionPendiente>>
    suspend fun decidirAmpliacion(id: Long, aprobar: Boolean, comentario: String?): Result<Unit>
    suspend fun decidirJustificacion(id: Long, aprobar: Boolean, comentario: String?): Result<Unit>
}
