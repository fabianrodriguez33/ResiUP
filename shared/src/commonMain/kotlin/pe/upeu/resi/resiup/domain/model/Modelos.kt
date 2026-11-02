package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

data class Pagina<T>(val contenido: List<T>, val pagina: Int, val totalElementos: Long, val ultima: Boolean)

// ---- Residente ----
data class HabitacionResumen(val id: Long, val numero: String, val cama: String? = null) {
    /** «Cama 101-A» cuando la letra está asignada. */
    val etiquetaCama: String? get() = cama?.let { "Cama $numero-$it" }
}

data class Perfil(
    val id: Long, val codigo: String, val nombreCompleto: String, val telefono: String?, val habitacion: HabitacionResumen?,
    val puedeSolicitarSolo: Boolean = false, val email: String? = null, val carrera: String? = null, val ciclo: Int? = null,
    val dni: String? = null, val fechaNacimiento: String? = null, val emailVerificado: Boolean = false,
)

/**
 * Lo que el residente puede editar de su perfil. Una cadena vacía borra el dato; el ciclo va de 1 a 14;
 * [fechaNacimiento] es «AAAA-MM-DD» o null si no cambia. El código universitario y el DNI los cambia solo la administración.
 */
data class PerfilEditable(val email: String, val telefono: String, val fechaNacimiento: String?, val carrera: String, val ciclo: Int?)

/** Datos personales de la cuenta de cualquier rol (pastor, garita, padre/madre…). */
data class PerfilCuenta(
    val usuario: String, val nombreCompleto: String, val dni: String?, val fechaNacimiento: String?,
    val email: String?, val emailVerificado: Boolean, val telefono: String?,
)

/** Cambios de contacto de la cuenta; mismas reglas que [PerfilEditable]. */
data class ContactoEditable(val email: String, val telefono: String, val fechaNacimiento: String?)

/** Día de lunes a viernes sin cultos (feriado, vacaciones). No corta la racha de asistencia. */
data class DiaSinCulto(val fecha: String, val motivo: String)

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
/** Corto: hasta 2 horas. Largo: más de 2 horas. Pernocte: duerme fuera (lo envía el padre/madre). */
enum class TipoPermiso(val etiqueta: String) { Corto("Corto"), Largo("Largo"), Pernocte("Pernoctar fuera") }

/** Quién envió la solicitud. */
enum class OrigenSolicitud { Residente, Apoderado }

/** Duración máxima de un permiso corto; si se pasa, debe pedirse uno largo. */
const val CORTO_MAX_MINUTOS = 120

enum class EstadoSolicitud(val etiqueta: String) {
    Borrador("Borrador"), PendienteApoderado("Esperando al padre/madre"), PendientePreceptor("Pendiente del pastor"),
    Aprobado("Aprobado"), Rechazado("Rechazado"), Cancelado("Cancelado"), Vencido("Vencido"), Anulado("Anulado"),
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
    val origen: OrigenSolicitud = OrigenSolicitud.Residente,
    /** Solo en permisos de pernoctar fuera. */
    val responsableNombre: String? = null,
    val responsableCelular: String? = null,
    val acompanante: String? = null,
)

/** [residenteId] solo lo envía el padre/madre (a qué hijo corresponde); los datos de la casa, solo en pernoctar. */
data class NuevaSolicitud(
    val tipo: TipoPermiso, val motivo: String, val destino: String, val salida: Instant, val retorno: Instant,
    val residenteId: Long? = null, val responsableNombre: String? = null, val responsableCelular: String? = null, val acompanante: String? = null,
    /** Clave que la app genera al pedir sin conexión: el servidor no duplica la solicitud si el envío se repite. */
    val claveCliente: String? = null,
)

/** Permiso pedido sin conexión que espera para enviarse. [error] es lo que respondió el servidor si lo rechazó. */
data class PermisoPendiente(
    val clave: String, val tipo: TipoPermiso, val motivo: String, val destino: String,
    val salida: Instant, val retorno: Instant, val creadaEn: Instant, val error: String? = null,
)

/** Hijo/a de un padre o madre con cuenta en la app. */
data class Hijo(val id: Long, val codigo: String, val nombre: String, val habitacion: String?, val cama: String?, val puedeSolicitarSolo: Boolean, val pendientes: Long)

/** Cultos y limpieza de los últimos [dias] días: lo que el pastor mira antes de aprobar un permiso. */
data class ResumenResidente(val dias: Int, val cultosRealizados: Long, val asistencias: Long, val despuesDelCorte: Long, val promedioLimpieza: Double?, val evaluaciones: Long)

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
