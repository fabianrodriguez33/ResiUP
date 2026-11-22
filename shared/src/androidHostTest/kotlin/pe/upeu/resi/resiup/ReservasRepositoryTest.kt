package pe.upeu.resi.resiup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.data.remote.ReservasApi
import pe.upeu.resi.resiup.data.repository.ReservasRepositoryImpl
import pe.upeu.resi.resiup.db.ResiDb
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.EstadoReserva
import pe.upeu.resi.resiup.domain.model.NuevaReserva
import pe.upeu.resi.resiup.domain.model.TipoReserva

private const val OFERTA = """{"comidas":[
    {"tipo":"ALMUERZO","fecha":"2026-11-23","puedeReservar":true},
    {"tipo":"DESAYUNO_FRIO","fecha":"2026-11-23","puedeReservar":false,"motivoNoDisponible":"Cerrado"}]}"""

private const val MIAS = """{"contenido":[{"id":5,"tipo":"ALMUERZO","fecha":"2026-11-23","motivo":"","estado":"RESERVADA","creadaEn":"2026-11-22T10:00:00Z"}]}"""

/** Base SQLite en memoria: el respaldo local de reservas se guarda aquí, como en el teléfono. */
private fun baseEnMemoria(): DatosLocales {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ResiDb.Schema.create(driver)
    return DatosLocales(ResiDb(driver))
}

class ReservasRepositoryTest {

    private fun errorDe(r: Result<*>): ErrorApi = (r.exceptionOrNull() as ErrorApiException).error

    @Test
    fun `la oferta se mapea del servidor al dominio`() = runTest {
        val e = Entorno { json(OFERTA) }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())

        val oferta = repo.oferta().getOrThrow()

        assertEquals(listOf(TipoReserva.Almuerzo, TipoReserva.Desayuno), oferta.map { it.tipo })
        assertEquals("Cerrado", oferta[1].motivoNoDisponible)
    }

    @Test
    fun `sin conexion la oferta sale de la ultima copia guardada`() = runTest {
        var enLinea = true
        val e = Entorno { if (enLinea) json(OFERTA) else throw Exception("sin red") }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())
        val enLineaAntes = repo.oferta().getOrThrow()

        enLinea = false
        val sinRed = repo.oferta().getOrThrow()

        assertEquals(enLineaAntes.map { it.tipo }, sinRed.map { it.tipo })
        assertEquals(enLineaAntes.map { it.fecha }, sinRed.map { it.fecha })
    }

    @Test
    fun `sin conexion y sin copia la oferta informa SinConexion`() = runTest {
        val e = Entorno { throw Exception("sin red") }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())

        assertEquals(ErrorApi.SinConexion, errorDe(repo.oferta()))
    }

    @Test
    fun `un error del servidor no se reemplaza por la copia`() = runTest {
        var enLinea = true
        val e = Entorno { if (enLinea) json(OFERTA) else json("{}", HttpStatusCode.InternalServerError) }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())
        repo.oferta().getOrThrow()

        enLinea = false
        val r = repo.oferta()

        assertEquals(ErrorApi.Servidor, errorDe(r))
    }

    @Test
    fun `mis reservas sin conexion usa la copia guardada`() = runTest {
        var enLinea = true
        val e = Entorno { if (enLinea) json(MIAS) else throw Exception("sin red") }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())
        repo.mias().getOrThrow()

        enLinea = false
        val copia = repo.mias().getOrThrow()

        assertEquals(5L, copia.single().id)
        assertEquals(EstadoReserva.Reservado, copia.single().estado)
    }

    @Test
    fun `reservar envia el tipo en formato de la API y mapea el estado`() = runTest {
        val e = Entorno {
            json("""{"id":9,"tipo":"CENA_TAPER","fecha":"2026-11-23","motivo":"Clase nocturna","estado":"PENDIENTE","creadaEn":"2026-11-22T10:00:00Z"}""")
        }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())

        val reserva = repo.reservar(NuevaReserva(TipoReserva.CenaTaper, "2026-11-23", "Clase nocturna", null)).getOrThrow()

        assertEquals(9L, reserva.id)
        assertEquals(TipoReserva.CenaTaper, reserva.tipo)
        assertEquals(EstadoReserva.Pendiente, reserva.estado)
        assertTrue(e.peticiones.single().url.encodedPath.endsWith("api/v1/reservas"))
    }

    @Test
    fun `cancelar una reserva inexistente informa NoEncontrado`() = runTest {
        val e = Entorno { json("""{"status":404,"error":"Not Found"}""", HttpStatusCode.NotFound) }
        val repo = ReservasRepositoryImpl(ReservasApi(e.cliente), baseEnMemoria())

        val r = repo.cancelar(404L)

        assertEquals(ErrorApi.NoEncontrado, errorDe(r))
    }
}
