package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

enum class TipoReserva(val etiqueta: String) {
    Almuerzo("Almuerzo"), Desayuno("Desayuno"), CenaTaper("Cena - Táper"), CenaSegundoTurno("Cena - Turno");

    val esCena get() = this == CenaTaper || this == CenaSegundoTurno
}

/** Pendiente = espera al pastor; Reservado = aprobada. */
enum class EstadoReserva(val etiqueta: String) {
    Pendiente("Pendiente"), Reservado("Reservado"), Rechazada("Rechazada"),
    CanceladaResidente("Cancelada"), CanceladaPastor("Cancelada"), CanceladaCierre("Cancelada"),
    ;

    val activa get() = this == Pendiente || this == Reservado
}

data class Reserva(
    val id: Long, val tipo: TipoReserva, val fecha: String, val motivo: String, val curso: String?, val observacion: String?,
    val estado: EstadoReserva, val creadaEn: Instant, val comentario: String?, val evidenciaId: Long? = null,
)

/** Una comida que se puede pedir hoy; [puedeReservar] ya considera horario, día cerrado y reservas previas. */
data class ComidaOfertada(val tipo: TipoReserva, val fecha: String, val puedeReservar: Boolean, val motivoNoDisponible: String?, val reserva: Reserva?)

data class NuevaReserva(val tipo: TipoReserva, val fecha: String, val motivo: String, val curso: String?, val evidenciaId: Long? = null)
