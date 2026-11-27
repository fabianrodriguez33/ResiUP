package pe.upeu.resi.resiup.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.model.EstadoReserva
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.Reserva
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.model.TipoReserva
import pe.upeu.resi.resiup.presentation.residente.permisosPrincipales
import pe.upeu.resi.resiup.presentation.residente.reservasPrincipales
import pe.upeu.resi.resiup.presentation.residente.todosLosPermisos

private val AHORA = Instant.parse("2026-11-27T12:00:00Z")

class HistorialTest {

    private fun permiso(
        id: Long, horas: Int, estado: EstadoSolicitud = EstadoSolicitud.Rechazado, retorno: Instant = AHORA - (horas - 2).hours,
        salida: Instant? = null, entrada: Instant? = null,
    ) = SolicitudPermiso(
        id = id, residenteCodigo = "A001", residenteNombre = "Ana", tipo = TipoPermiso.Corto, motivo = "Compras", destino = "Centro",
        salida = AHORA - horas.hours, retornoSolicitado = retorno, retornoAutorizado = retorno, estado = estado, requiereApoderado = false,
        comentarioDecision = null, creadoEn = AHORA - horas.hours, ampliaciones = emptyList(), justificaciones = emptyList(),
        garitaSalida = salida, garitaEntrada = entrada,
    )

    private fun reserva(id: Long, horas: Int, fecha: String) =
        Reserva(id, TipoReserva.Almuerzo, fecha, "Clases", null, null, EstadoReserva.Reservado, AHORA - horas.hours, null)

    @Test
    fun `un permiso de 71 horas se ve y uno de 73 solo en el historial`() {
        val lista = listOf(permiso(73, 73), permiso(71, 71))

        assertEquals(listOf(71L), lista.permisosPrincipales(AHORA).map { it.id })
        assertEquals(listOf(71L, 73L), lista.todosLosPermisos().map { it.id })
    }

    @Test
    fun `un permiso vigente antiguo sigue en la lista y uno terminado no`() {
        val futuro = AHORA + 24.hours
        val lista = listOf(
            permiso(1, 200, EstadoSolicitud.PendientePreceptor),
            permiso(2, 201, EstadoSolicitud.PendienteApoderado),
            permiso(3, 202, EstadoSolicitud.Aprobado, retorno = futuro),
            permiso(4, 203, EstadoSolicitud.Aprobado, salida = AHORA - 190.hours), // sigue fuera
            permiso(5, 204, EstadoSolicitud.Aprobado), // venció sin usarse
            permiso(6, 205, EstadoSolicitud.Aprobado, retorno = futuro, salida = AHORA - 5.hours, entrada = AHORA - 4.hours),
            permiso(7, 206, EstadoSolicitud.Cancelado, retorno = futuro),
        )

        assertEquals(listOf(1L, 2L, 3L, 4L), lista.permisosPrincipales(AHORA).map { it.id })
    }

    @Test
    fun `una reserva antigua de hoy en adelante sigue en la lista`() {
        val lista = listOf(
            reserva(73, 73, "2026-11-24"), reserva(71, 71, "2026-11-24"),
            reserva(1, 300, "2026-11-27"), reserva(2, 301, "2026-11-28"),
        )

        assertEquals(listOf(71L, 1L, 2L), lista.reservasPrincipales(AHORA, LocalDate(2026, 11, 27)).map { it.id })
    }
}
