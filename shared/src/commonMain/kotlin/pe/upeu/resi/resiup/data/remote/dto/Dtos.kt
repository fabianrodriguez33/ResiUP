package pe.upeu.resi.resiup.data.remote.dto

import kotlinx.serialization.Serializable

/** DTOs del contrato REST de ResiAPI. Las fechas viajan como texto ISO‑8601 y se convierten en los mappers. */
@Serializable
data class PaginaDto<T>(
    val contenido: List<T> = emptyList(),
    val pagina: Int = 0,
    val tamanio: Int = 0,
    val totalElementos: Long = 0,
    val totalPaginas: Int = 0,
    val ultima: Boolean = true,
)

@Serializable
data class HabitacionResumenDto(val id: Long, val numero: String, val cama: String? = null)

@Serializable
data class ResidenteDto(
    val id: Long,
    val codigo: String,
    val nombreCompleto: String,
    val telefono: String? = null,
    val activo: Boolean = true,
    val habitacion: HabitacionResumenDto? = null,
    val puedeSolicitarSolo: Boolean = false,
    val email: String? = null,
    val carrera: String? = null,
    val ciclo: Int? = null,
    val dni: String? = null,
    val fechaNacimiento: String? = null,
    val emailVerificado: Boolean = false,
)

@Serializable
data class PerfilRequestDto(val email: String? = null, val telefono: String? = null, val fechaNacimiento: String? = null, val carrera: String? = null, val ciclo: Int? = null)

@Serializable
data class PerfilCuentaDto(
    val username: String,
    val nombreCompleto: String,
    val dni: String? = null,
    val fechaNacimiento: String? = null,
    val email: String? = null,
    val emailVerificado: Boolean = false,
    val telefono: String? = null,
)

@Serializable
data class ContactoRequestDto(val email: String? = null, val telefono: String? = null, val fechaNacimiento: String? = null)

@Serializable
data class CodigoCorreoRequestDto(val codigo: String)

@Serializable
data class DiaSinCultoDto(val fecha: String, val motivo: String)

@Serializable
data class AnulacionRequestDto(val motivo: String)

@Serializable
data class QrDto(val contenido: String)

@Serializable
data class AsistenciaDto(
    val id: Long,
    val fecha: String,
    val tipo: String,
    val codigo: String = "",
    val nombreCompleto: String = "",
    val registradoEn: String,
    val despuesDelCorte: Boolean = false,
)

@Serializable
data class EvaluacionDto(
    val id: Long,
    val fecha: String,
    val total: Int,
    val maximo: Int = 20,
    val puntajes: Map<String, Int> = emptyMap(),
    val observacion: String? = null,
)

@Serializable
data class ResumenLimpiezaDto(val evaluaciones: Long = 0, val promedio: Double? = null, val ultimoTotal: Int? = null, val ultimaFecha: String? = null)

@Serializable
data class AmpliacionDto(val id: Long, val motivo: String, val retornoNuevo: String, val estado: String)

@Serializable
data class JustificacionDto(val id: Long, val motivo: String, val estado: String, val comentario: String? = null)

@Serializable
data class SolicitudDto(
    val id: Long,
    val residenteCodigo: String = "",
    val residenteNombre: String = "",
    val tipo: String,
    val motivo: String,
    val destino: String,
    val salidaSolicitada: String,
    val retornoSolicitado: String,
    val retornoAutorizado: String,
    val estado: String,
    val requiereApoderado: Boolean = false,
    val comentarioDecision: String? = null,
    val creadoEn: String,
    val ampliaciones: List<AmpliacionDto>? = null,
    val justificaciones: List<JustificacionDto>? = null,
    val origen: String? = null,
    val responsableNombre: String? = null,
    val responsableCelular: String? = null,
    val acompanante: String? = null,
)

@Serializable
data class SolicitudRequestDto(
    val tipo: String, val motivo: String, val destino: String, val salida: String, val retorno: String,
    val residenteId: Long? = null, val responsableNombre: String? = null, val responsableCelular: String? = null, val acompanante: String? = null,
    val claveCliente: String? = null, val creadaEn: String? = null,
)

@Serializable
data class DecisionApoderadoRequestDto(val aprobar: Boolean)

@Serializable
data class HijoDto(
    val residenteId: Long, val codigo: String, val nombreCompleto: String, val habitacion: String? = null, val cama: String? = null,
    val puedeSolicitarSolo: Boolean = false, val pendientes: Long = 0,
)

@Serializable
data class ResumenResidenteDto(
    val dias: Int = 30, val cultosRealizados: Long = 0, val asistencias: Long = 0, val despuesDelCorte: Long = 0,
    val promedioLimpieza: Double? = null, val evaluaciones: Long = 0,
)

@Serializable
data class JustificacionRequestDto(val motivo: String, val evidenciaId: Long? = null)

@Serializable
data class EnlaceDto(val token: String, val ruta: String, val expiraEn: String, val apoderado: String = "")

@Serializable
data class SesionCultoDto(val id: Long, val fecha: String, val tipo: String, val corte: String, val estado: String, val presentes: Long = 0)

@Serializable
data class AbrirSesionRequestDto(val tipo: String)

@Serializable
data class QrRequestDto(val qr: String)

@Serializable
data class PuntajePersonalDto(val residenteId: Long, val ropero: Int, val cama: Int, val escritorio: Int, val piso: Int)

@Serializable
data class LimpiezaHabitacionRequestDto(val fecha: String, val basura: Int, val olores: Int, val residentes: List<PuntajePersonalDto>)

@Serializable
data class ResidenteLimpiezaDto(val residenteId: Long, val codigo: String, val nombreCompleto: String, val cama: String? = null, val evaluado: Boolean = false)

@Serializable
data class HabitacionLimpiezaDto(val habitacionId: Long, val numero: String, val capacidad: Int = 0, val residentes: List<ResidenteLimpiezaDto> = emptyList(), val evaluados: Long = 0)

@Serializable
data class ResidenteGaritaDto(val id: Long, val codigo: String, val nombreCompleto: String, val habitacion: String? = null)

@Serializable
data class VerificacionDto(
    val resultado: String,
    val mensaje: String = "",
    val residente: ResidenteGaritaDto? = null,
    val movimientoPermitido: String? = null,
    val solicitudId: Long? = null,
    val retornoAutorizado: String? = null,
    val minutosTarde: Int? = null,
)

@Serializable
data class MovimientoRequestDto(val qr: String, val tipo: String)

@Serializable
data class MovimientoDto(
    val id: Long,
    val codigo: String = "",
    val nombreCompleto: String = "",
    val tipo: String,
    val registradoEn: String,
    val minutosTarde: Int? = null,
    val anulado: Boolean = false,
)
