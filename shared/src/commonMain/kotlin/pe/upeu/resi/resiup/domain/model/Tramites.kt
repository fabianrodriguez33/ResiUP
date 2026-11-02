package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

/** Foto elegida por el residente (ya reducida) para respaldar una ampliación o una justificación. */
class ImagenElegida(val bytes: ByteArray, val nombre: String, val tipo: String)

/** Pedido de volver más tarde, con la foto que lo respalda. El pastor lo revisa. */
data class AmpliacionPendiente(
    val id: Long, val solicitudId: Long, val residenteNombre: String, val residenteCodigo: String, val motivo: String,
    val retornoOriginal: Instant, val retornoNuevo: Instant, val evidenciaId: Long?,
)

/** Explicación de un residente que volvió después de la hora autorizada. */
data class JustificacionPendiente(
    val id: Long, val solicitudId: Long, val residenteNombre: String, val residenteCodigo: String, val motivo: String, val evidenciaId: Long?,
)
