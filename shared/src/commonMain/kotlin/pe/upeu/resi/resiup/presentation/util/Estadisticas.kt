package pe.upeu.resi.resiup.presentation.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto

/**
 * Estadísticas de asistencia a cultos (lunes a viernes) calculadas con la copia local de asistencias.
 * [racha]: días hábiles seguidos con asistencia hasta hoy; el día de hoy no corta la racha mientras todavía no se registra.
 * Los feriados y vacaciones sin culto cortan la racha porque la app no conoce el calendario institucional.
 */
data class EstadisticasCultos(val racha: Int, val diasAsistidosMes: Int, val diasHabilesMes: Int, val delMesAnterior: Boolean = false)

private fun LocalDate.esHabil() = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

private fun LocalDate.habilAnterior(): LocalDate {
    var d = this.minus(1, DateTimeUnit.DAY)
    while (!d.esHabil()) d = d.minus(1, DateTimeUnit.DAY)
    return d
}

fun calcularEstadisticas(asistencias: List<AsistenciaCulto>, hoy: LocalDate): EstadisticasCultos {
    val dias = asistencias.mapNotNull { runCatching { LocalDate.parse(it.fecha) }.getOrNull() }.toSet()

    var cursor = hoy
    while (!cursor.esHabil()) cursor = cursor.minus(1, DateTimeUnit.DAY)
    if (cursor == hoy && cursor !in dias) cursor = cursor.habilAnterior()
    var racha = 0
    while (cursor in dias) {
        racha++
        cursor = cursor.habilAnterior()
    }

    val (asistidos, habiles) = diasDelMes(dias, hoy)
    if (habiles > 0) return EstadisticasCultos(racha, asistidos, habiles)
    // Primeros días del mes sin ningún día hábil todavía (por ejemplo, un domingo 1): se muestra el mes que acaba de terminar.
    val finMesPasado = LocalDate(hoy.year, hoy.month, 1).minus(1, DateTimeUnit.DAY)
    val (asistidosPasado, habilesPasado) = diasDelMes(dias, finMesPasado)
    return EstadisticasCultos(racha, asistidosPasado, habilesPasado, delMesAnterior = true)
}

/** Días hábiles con asistencia y días hábiles transcurridos, desde el día 1 del mes de [hasta] hasta esa fecha. */
private fun diasDelMes(dias: Set<LocalDate>, hasta: LocalDate): Pair<Int, Int> {
    val primero = LocalDate(hasta.year, hasta.month, 1)
    var habiles = 0
    var d = primero
    while (d <= hasta) {
        if (d.esHabil()) habiles++
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return dias.count { it >= primero && it <= hasta && it.esHabil() } to habiles
}
