package pe.upeu.resi.resiup.presentation.util

import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

/**
 * Horario en que se pueden ENVIAR solicitudes de permiso (hora de Perú; no es la hora de salida). Desde las 5:30 a. m.;
 * domingo a jueves hasta las 10:00 p. m.; viernes hasta las 2:00 p. m.; sábado de 6:30 p. m. a 10:00 p. m.
 * El servidor decide siempre; esto solo evita pedir (o dejar en cola) algo que se va a rechazar.
 */
object HorarioEnvio {
    const val MENSAJE = "Las solicitudes se reciben de domingo a jueves de 5:30 a. m. a 10:00 p. m., los viernes de 5:30 a. m. a 2:00 p. m. " +
        "y los sábados de 6:30 p. m. a 10:00 p. m."

    private val apertura = LocalTime(5, 30)

    /** Reloj con el que se evalúa el horario; las pruebas lo fijan para no depender de la hora real. */
    var reloj: () -> Instant = { ahora() }

    fun abierto(momento: Instant): Boolean {
        val f = momento.toLocalDateTime(ZonaResidencia)
        val hora = LocalTime(f.hour, f.minute)
        return when (f.dayOfWeek) {
            DayOfWeek.SATURDAY -> hora >= LocalTime(18, 30) && hora <= LocalTime(22, 0)
            DayOfWeek.FRIDAY -> hora >= apertura && hora <= LocalTime(14, 0)
            else -> hora >= apertura && hora <= LocalTime(22, 0)
        }
    }
}
