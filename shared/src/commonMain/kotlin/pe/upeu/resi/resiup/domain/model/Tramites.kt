package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

/** Foto elegida por el residente (ya reducida) para respaldar una ampliación o una justificación. */
class ImagenElegida(val bytes: ByteArray, val nombre: String, val tipo: String)

/** Pedido de volver más tarde, con la foto que lo respalda. El pastor lo revisa. */
data class AmpliacionPendiente(
    val id: Long, val solicitudId: Long, val residenteNombre: String, val residenteCodigo: String, val motivo: String,
    val retornoOriginal: Instant, val retornoNuevo: Instant, val evidenciaId: Long?,
)

/** Estado de habilitación semanal del residente para solicitar permisos (domingo a sábado). */
data class Habilitacion(val habilitado: Boolean, val hasta: String, val motivo: String?, val faltasNoJustificadas: Int, val promedioLimpieza: Double?)

/** Día de falta a cultos que el pastor todavía puede justificar (hasta [plazo], el lunes). */
data class FaltaPendiente(val residenteId: Long, val codigo: String, val nombre: String, val fecha: String, val plazo: String)

/** Permiso aprobado de un residente inhabilitado esta semana: el pastor decide si lo anula. */
data class PermisoDeSuspendido(val id: Long, val residenteNombre: String, val residenteCodigo: String, val tipo: String, val destino: String, val salida: Instant)

/** Explicación de un residente que volvió después de la hora autorizada. */
data class JustificacionPendiente(
    val id: Long, val solicitudId: Long, val residenteNombre: String, val residenteCodigo: String, val motivo: String, val evidenciaId: Long?,
)
