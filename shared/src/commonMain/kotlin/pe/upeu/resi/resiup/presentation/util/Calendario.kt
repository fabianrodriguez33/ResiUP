package pe.upeu.resi.resiup.presentation.util

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto

val NOMBRES_MES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

data class DiaCalendario(
    val fecha: LocalDate,
    val delMes: Boolean,
    val asistencias: List<AsistenciaCulto>,
    val esHoy: Boolean,
    /** Los cultos son de lunes a viernes. */
    val habil: Boolean,
    /** Día hábil ya pasado, dentro del periodo descargado, sin ningún registro. No implica falta: puede haber sido feriado o permiso. */
    val sinRegistro: Boolean,
)

/**
 * Semanas (lunes a domingo) de un mes. [desde] es la fecha más antigua descargada: antes de ella no se afirma nada,
 * porque la copia local solo guarda los registros más recientes.
 */
fun armarMes(anio: Int, mes: Int, asistencias: List<AsistenciaCulto>, hoy: LocalDate, desde: LocalDate?): List<List<DiaCalendario>> {
    val primero = LocalDate(anio, mes, 1)
    val ultimo = primero.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    val porFecha = asistencias.groupBy { it.fecha }
    var dia = primero.minus(primero.dayOfWeek.ordinal, DateTimeUnit.DAY)
    val semanas = mutableListOf<List<DiaCalendario>>()
    while (dia <= ultimo) {
        semanas += (0 until 7).map {
            val d = dia.plus(it, DateTimeUnit.DAY)
            val registros = porFecha[d.toString()].orEmpty()
            val habil = d.dayOfWeek.ordinal < 5
            DiaCalendario(
                fecha = d,
                delMes = d.month == primero.month && d.year == primero.year,
                asistencias = registros,
                esHoy = d == hoy,
                habil = habil,
                sinRegistro = habil && registros.isEmpty() && d < hoy && desde != null && d >= desde,
            )
        }
        dia = dia.plus(7, DateTimeUnit.DAY)
    }
    return semanas
}

/** Mes anterior o siguiente como (año, mes). */
fun mesDesplazado(anio: Int, mes: Int, delta: Int): Pair<Int, Int> {
    val total = anio * 12 + (mes - 1) + delta
    return total / 12 to total % 12 + 1
}

/** Fecha más antigua con registro, o null si no hay ninguno. */
fun List<AsistenciaCulto>.fechaMasAntigua(): LocalDate? = minOfOrNull { it.fecha }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
