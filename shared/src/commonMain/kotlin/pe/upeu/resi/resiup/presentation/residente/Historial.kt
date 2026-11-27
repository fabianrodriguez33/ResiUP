package pe.upeu.resi.resiup.presentation.residente

import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.Reserva
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso

// La lista principal muestra lo generado en las últimas 72 horas y lo que sigue vigente; «Historial», todo.

private fun esReciente(creado: Instant, ahora: Instant) = ahora - creado <= 72.hours

/** Pendiente, o aprobado sin terminar: sin retorno en garita y todavía en plazo (o fuera de la residencia). */
fun SolicitudPermiso.vigente(ahora: Instant): Boolean = when (estado) {
    EstadoSolicitud.PendienteApoderado, EstadoSolicitud.PendientePreceptor -> true
    EstadoSolicitud.Aprobado -> garitaEntrada == null && (retornoAutorizado > ahora || garitaSalida != null)
    else -> false
}

fun List<SolicitudPermiso>.todosLosPermisos() = sortedByDescending { it.creadoEn }

fun List<SolicitudPermiso>.permisosPrincipales(ahora: Instant) =
    filter { it.vigente(ahora) || esReciente(it.creadoEn, ahora) }.todosLosPermisos()

fun List<Reserva>.todasLasReservas() = sortedByDescending { it.creadaEn }

/** Las reservas de [hoy] (fecha de Lima) en adelante nunca salen de la lista. */
fun List<Reserva>.reservasPrincipales(ahora: Instant, hoy: LocalDate) =
    filter { it.fecha >= hoy.toString() || esReciente(it.creadaEn, ahora) }.todasLasReservas()
