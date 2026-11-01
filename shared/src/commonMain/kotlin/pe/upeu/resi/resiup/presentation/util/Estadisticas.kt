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
 * Los feriados y vacaciones que el pastor marca como «sin culto» no cortan la racha ni cuentan como días del mes.
 */
data class EstadisticasCultos(val racha: Int, val diasAsistidosMes: Int, val diasHabilesMes: Int, val delMesAnterior: Boolean = false)

private fun LocalDate.esFinDeSemana() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

/** Día con culto: de lunes a viernes y no marcado como sin culto (feriado, vacaciones). */
private fun LocalDate.esHabil(sinCulto: Set<LocalDate>) = !esFinDeSemana() && this !in sinCulto

private fun LocalDate.habilAnterior(sinCulto: Set<LocalDate>): LocalDate {
    var d = this.minus(1, DateTimeUnit.DAY)
    while (!d.esHabil(sinCulto)) d = d.minus(1, DateTimeUnit.DAY)
    return d
}

/** [diasSinCulto]: fechas sin cultos; se saltan igual que los fines de semana y no cuentan en el mes. */
fun calcularEstadisticas(asistencias: List<AsistenciaCulto>, hoy: LocalDate, diasSinCulto: Set<LocalDate> = emptySet()): EstadisticasCultos {
    val dias = asistencias.mapNotNull { runCatching { LocalDate.parse(it.fecha) }.getOrNull() }.toSet()

    var cursor = hoy
    while (!cursor.esHabil(diasSinCulto)) cursor = cursor.minus(1, DateTimeUnit.DAY)
    if (cursor == hoy && cursor !in dias) cursor = cursor.habilAnterior(diasSinCulto)
    var racha = 0
    while (cursor in dias) {
        racha++
        cursor = cursor.habilAnterior(diasSinCulto)
    }

    val (asistidos, habiles) = diasDelMes(dias, hoy, diasSinCulto)
    if (habiles > 0) return EstadisticasCultos(racha, asistidos, habiles)
    // Primeros días del mes sin ningún día hábil todavía (por ejemplo, un domingo 1): se muestra el mes que acaba de terminar.
    val finMesPasado = LocalDate(hoy.year, hoy.month, 1).minus(1, DateTimeUnit.DAY)
    val (asistidosPasado, habilesPasado) = diasDelMes(dias, finMesPasado, diasSinCulto)
    return EstadisticasCultos(racha, asistidosPasado, habilesPasado, delMesAnterior = true)
}

/** Días hábiles con asistencia y días hábiles transcurridos, desde el día 1 del mes de [hasta] hasta esa fecha. */
private fun diasDelMes(dias: Set<LocalDate>, hasta: LocalDate, sinCulto: Set<LocalDate>): Pair<Int, Int> {
    val primero = LocalDate(hasta.year, hasta.month, 1)
    var habiles = 0
    var d = primero
    while (d <= hasta) {
        if (d.esHabil(sinCulto)) habiles++
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return dias.count { it >= primero && it <= hasta && it.esHabil(sinCulto) } to habiles
}
