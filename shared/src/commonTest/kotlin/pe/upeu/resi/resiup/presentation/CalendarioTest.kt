package pe.upeu.resi.resiup.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.presentation.util.armarMes
import pe.upeu.resi.resiup.presentation.util.fechaMasAntigua
import pe.upeu.resi.resiup.presentation.util.mesDesplazado

class CalendarioTest {
    private fun culto(fecha: String, tipo: TipoCulto = TipoCulto.Matutino, tarde: Boolean = false) =
        AsistenciaCulto(1, fecha, tipo, Instant.fromEpochMilliseconds(0), tarde)

    private val hoy = LocalDate(2026, 10, 30) // viernes

    @Test
    fun `octubre de 2026 empieza en jueves y las semanas van de lunes a domingo`() {
        val semanas = armarMes(2026, 10, emptyList(), hoy, null)
        assertEquals(5, semanas.size)
        assertTrue(semanas.all { it.size == 7 })
        assertEquals(LocalDate(2026, 9, 28), semanas.first().first().fecha, "la primera semana empieza el lunes 28 de septiembre")
        assertFalse(semanas.first().first().delMes)
        assertTrue(semanas.first()[3].delMes, "el jueves 1 es del mes")
        assertEquals(LocalDate(2026, 11, 1), semanas.last().last().fecha)
    }

    @Test
    fun `los dias con asistencia se agrupan y los de fin de semana no son habiles`() {
        val lista = listOf(culto("2026-10-05"), culto("2026-10-05", TipoCulto.Vespertino, tarde = true), culto("2026-10-06"))
        val dias = armarMes(2026, 10, lista, hoy, lista.fechaMasAntigua()).flatten()
        assertEquals(2, dias.first { it.fecha == LocalDate(2026, 10, 5) }.asistencias.size)
        assertEquals(1, dias.first { it.fecha == LocalDate(2026, 10, 6) }.asistencias.size)
        assertFalse(dias.first { it.fecha == LocalDate(2026, 10, 3) }.habil)
        assertTrue(dias.first { it.fecha == hoy }.esHoy)
    }

    @Test
    fun `solo se marca sin registro un dia habil pasado dentro del periodo descargado`() {
        val lista = listOf(culto("2026-10-05"))
        val dias = armarMes(2026, 10, lista, hoy, lista.fechaMasAntigua()).flatten().associateBy { it.fecha }
        assertFalse(dias.getValue(LocalDate(2026, 10, 2)).sinRegistro, "anterior al periodo descargado: no se afirma nada")
        assertFalse(dias.getValue(LocalDate(2026, 10, 5)).sinRegistro, "tiene asistencia")
        assertTrue(dias.getValue(LocalDate(2026, 10, 7)).sinRegistro)
        assertFalse(dias.getValue(LocalDate(2026, 10, 10)).sinRegistro, "sábado")
        assertFalse(dias.getValue(hoy).sinRegistro, "hoy todavía puede registrarse")
        val sinDatos = armarMes(2026, 10, emptyList(), hoy, null).flatten()
        assertTrue(sinDatos.none { it.sinRegistro }, "sin ningún dato no se marca ninguna falta")
    }

    @Test
    fun `desplazar el mes cruza los limites del anio`() {
        assertEquals(2026 to 12, mesDesplazado(2027, 1, -1))
        assertEquals(2027 to 1, mesDesplazado(2026, 12, 1))
        assertEquals(2026 to 10, mesDesplazado(2026, 10, 0))
        assertNull(emptyList<AsistenciaCulto>().fechaMasAntigua())
    }
}
