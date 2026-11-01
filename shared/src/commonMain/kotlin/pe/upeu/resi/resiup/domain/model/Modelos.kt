package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

data class Pagina<T>(val contenido: List<T>, val pagina: Int, val totalElementos: Long, val ultima: Boolean)

// ---- Residente ----
data class HabitacionResumen(val id: Long, val numero: String, val cama: String? = null) {
    /** «Cama 101-A» cuando la letra está asignada. */
    val etiquetaCama: String? get() = cama?.let { "Cama $numero-$it" }
}

data class Perfil(val id: Long, val codigo: String, val nombreCompleto: String, val telefono: String?, val habitacion: HabitacionResumen?)

/** Contenido del QR emitido por el servidor. Se muestra tal cual; la app no lo interpreta. */
/** [guardado] indica que se muestra la copia cifrada del teléfono porque no hubo conexión. */
data class QrPersonal(val contenido: String, val guardado: Boolean = false)

enum class TipoCulto(val etiqueta: String) { Matutino("Matutino"), Vespertino("Vespertino") }

data class AsistenciaCulto(val id: Long, val fecha: String, val tipo: TipoCulto, val registradoEn: Instant, val despuesDelCorte: Boolean)

enum class CriterioLimpieza(val etiqueta: String, val maximo: Int) {
    Ropero("Ropero", 5), Cama("Cama tendida", 3), Escritorio("Escritorio", 3),
    Basura("Basura", 3), Olores("Olores", 3), Piso("Piso", 3),
}

data class EvaluacionLimpieza(
    val id: Long,
    val fecha: String,
    val total: Int,
    val maximo: Int,
    val puntajes: Map<CriterioLimpieza, Int>,
    val observacion: String?,
)

data class ResumenLimpieza(val evaluaciones: Long, val promedio: Double?, val ultimoTotal: Int?, val ultimaFecha: String?)

// ---- Permisos ----
enum class TipoPermiso(val etiqueta: String) { Corto("Corto"), Largo("Largo"), Especial("Especial") }

enum class EstadoSolicitud(val etiqueta: String) {
    Borrador("Borrador"), PendienteApoderado("Pendiente del apoderado"), PendientePreceptor("Pendiente del preceptor"),
    Aprobado("Aprobado"), Rechazado("Rechazado"), Cancelado("Cancelado"), Vencido("Vencido"),
}

enum class EstadoRevision(val etiqueta: String) { Pendiente("Pendiente"), Aprobada("Aprobada"), Rechazada("Rechazada") }

data class Ampliacion(val id: Long, val motivo: String, val retornoNuevo: Instant, val estado: EstadoRevision)

data class Justificacion(val id: Long, val motivo: String, val estado: EstadoRevision, val comentario: String?)

data class SolicitudPermiso(
    val id: Long,
    val residenteCodigo: String,
    val residenteNombre: String,
    val tipo: TipoPermiso,
    val motivo: String,
    val destino: String,
    val salida: Instant,
    val retornoSolicitado: Instant,
    val retornoAutorizado: Instant,
    val estado: EstadoSolicitud,
    val requiereApoderado: Boolean,
    val comentarioDecision: String?,
    val creadoEn: Instant,
    val ampliaciones: List<Ampliacion>,
    val justificaciones: List<Justificacion>,
)

data class NuevaSolicitud(val tipo: TipoPermiso, val motivo: String, val destino: String, val salida: Instant, val retorno: Instant)

data class EnlaceApoderado(val token: String, val ruta: String, val expiraEn: Instant, val apoderado: String)

// ---- Preceptor ----
data class ResidenteResumen(val id: Long, val codigo: String, val nombreCompleto: String, val habitacion: String?, val activo: Boolean)

data class SesionCulto(val id: Long, val fecha: String, val tipo: TipoCulto, val cerrada: Boolean, val presentes: Long, val corte: Instant)

data class AsistenciaRegistrada(val codigo: String, val nombreCompleto: String, val despuesDelCorte: Boolean)

/** Residente de una habitación en la lista de limpieza; [cama] es la letra («A») y [evaluado] indica si ya tiene nota en la fecha. */
data class ResidenteLimpieza(val residenteId: Long, val codigo: String, val nombreCompleto: String, val cama: String?, val evaluado: Boolean)

data class HabitacionLimpieza(val id: Long, val numero: String, val capacidad: Int, val residentes: List<ResidenteLimpieza>, val evaluados: Int) {
    val pendientes get() = residentes.filterNot { it.evaluado }
    val completa get() = residentes.isNotEmpty() && pendientes.isEmpty()
}

/** Puntaje personal de un residente: basura y olores no van aquí, son comunes a la habitación. */
data class PuntajePersonal(val residenteId: Long, val ropero: Int, val cama: Int, val escritorio: Int, val piso: Int)

data class NuevaEvaluacionHabitacion(val habitacionId: Long, val fecha: String, val basura: Int, val olores: Int, val personas: List<PuntajePersonal>)

/** Criterios que puntúa cada residente por separado; basura y olores se puntúan una vez por habitación. */
val CRITERIOS_PERSONALES = listOf(CriterioLimpieza.Ropero, CriterioLimpieza.Cama, CriterioLimpieza.Escritorio, CriterioLimpieza.Piso)
val CRITERIOS_DE_HABITACION = listOf(CriterioLimpieza.Basura, CriterioLimpieza.Olores)

// ---- Garita ----
enum class TipoMovimiento(val etiqueta: String) { Salida("Salida"), Retorno("Retorno") }

enum class ResultadoGarita {
    AutorizadoSalida, AutorizadoRetorno, RetornoFueraDeHorario, QrInvalido, QrRevocado,
    ResidenteInactivo, SinPermiso, PermisoAunNoVigente, PermisoVencido, Desconocido,
}

data class VerificacionGarita(
    val resultado: ResultadoGarita,
    val mensaje: String,
    val nombreCompleto: String?,
    val codigo: String?,
    val habitacion: String?,
    val movimientoPermitido: TipoMovimiento?,
    val retornoAutorizado: Instant?,
    val minutosTarde: Int?,
)

data class MovimientoRegistrado(
    val id: Long,
    val codigo: String,
    val nombreCompleto: String,
    val tipo: TipoMovimiento,
    val registradoEn: Instant,
    val minutosTarde: Int?,
    val anulado: Boolean,
)
