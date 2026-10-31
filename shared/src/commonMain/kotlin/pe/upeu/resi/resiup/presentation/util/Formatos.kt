package pe.upeu.resi.resiup.presentation.util

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/** La residencia usa hora de Lima (UTC‑5, sin horario de verano). */
val ZonaResidencia: TimeZone = TimeZone.of("America/Lima")

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

private fun LocalDateTime.hora12(): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h:${minute.toString().padStart(2, '0')} ${if (hour < 12) "a. m." else "p. m."}"
}

fun LocalDate.legible(): String = "$day ${MESES[month.ordinal]} $year"

/** «vie 30 oct, 4:00 p. m.» en hora de Lima. */
fun Instant.formatoLima(): String {
    val f = toLocalDateTime(ZonaResidencia)
    return "${DIAS[f.date.dayOfWeek.ordinal]} ${f.day} ${MESES[f.month.ordinal]}, ${f.hora12()}"
}

fun Instant.soloHoraLima(): String = toLocalDateTime(ZonaResidencia).hora12()

/** Convierte «2026-10-30» (texto del servidor) a «30 oct 2026». */
fun String.fechaLegible(): String = runCatching { LocalDate.parse(this).legible() }.getOrDefault(this)

fun hoyLima(): LocalDate = Clock.System.todayIn(ZonaResidencia)

fun ahora(): Instant = Clock.System.now()

/** Combina fecha (milisegundos UTC a medianoche del selector) y hora local de Lima en un instante. */
fun combinarFechaHora(fechaMillisUtc: Long, hora: Int, minuto: Int): Instant {
    val fecha = Instant.fromEpochMilliseconds(fechaMillisUtc).toLocalDateTime(TimeZone.UTC).date
    return LocalDateTime(fecha, LocalTime(hora, minuto)).toInstant(ZonaResidencia)
}

/** Un decimal con coma («17,5»), sin depender de la configuración regional de la plataforma. */
fun Double.unDecimal(): String {
    val decimas = kotlin.math.round(this * 10).toInt()
    return "${decimas / 10},${decimas % 10}"
}
