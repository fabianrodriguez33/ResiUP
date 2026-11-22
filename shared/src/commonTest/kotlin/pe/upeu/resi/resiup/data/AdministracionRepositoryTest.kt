package pe.upeu.resi.resiup.data

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.Entorno
import pe.upeu.resi.resiup.data.repository.AccionesPastorRepositoryImpl
import pe.upeu.resi.resiup.data.repository.AdministracionRepositoryImpl
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.NuevoSemestreAdmin
import pe.upeu.resi.resiup.json

class AdministracionRepositoryTest {

    private fun errorDe(r: Result<*>): ErrorApi = (r.exceptionOrNull() as ErrorApiException).error

    private fun cuerpoDe(e: Entorno): String = (e.peticiones.single().body as TextContent).text

    @Test
    fun `residentes pide una pagina de 100 y devuelve su contenido`() = runTest {
        val e = Entorno {
            json("""{"contenido":[{"id":1,"codigo":"A001","nombreCompleto":"Ana Pérez","habitacion":{"id":3,"numero":"101"}}],"pagina":0,"tamanio":100}""")
        }

        val lista = AdministracionRepositoryImpl(e.cliente).residentes().getOrThrow()

        assertEquals("A001", lista.single().codigo)
        assertEquals("101", lista.single().habitacion?.numero)
        val peticion = e.peticiones.single()
        assertEquals("/api/v1/residentes", peticion.url.encodedPath)
        assertEquals("0", peticion.url.parameters["pagina"])
        assertEquals("100", peticion.url.parameters["tamanio"])
    }

    @Test
    fun `semestreActual devuelve el semestre activo del servidor`() = runTest {
        val e = Entorno {
            json("""{"id":2,"nombre":"2026-2","inicio":"2026-08-01","fin":"2026-12-31","activo":true}""")
        }

        val semestre = AdministracionRepositoryImpl(e.cliente).semestreActual().getOrThrow()

        assertEquals("2026-2", semestre.nombre)
        assertTrue(semestre.activo)
        assertTrue(e.peticiones.single().url.encodedPath.endsWith("api/v1/semestres/actual"))
    }

    @Test
    fun `crearSemestre envia POST con el nombre y las fechas`() = runTest {
        val e = Entorno {
            json("""{"id":4,"nombre":"2027-1","inicio":"2027-03-01","fin":"2027-07-31","activo":false}""")
        }

        AdministracionRepositoryImpl(e.cliente)
            .crearSemestre(NuevoSemestreAdmin("2027-1", "2027-03-01", "2027-07-31"))
            .getOrThrow()

        assertEquals(HttpMethod.Post, e.peticiones.single().method)
        val cuerpo = cuerpoDe(e)
        assertTrue(cuerpo.contains("\"nombre\":\"2027-1\""), cuerpo)
        assertTrue(cuerpo.contains("\"inicio\":\"2027-03-01\""), cuerpo)
    }

    @Test
    fun `revocar envia el motivo en el cuerpo`() = runTest {
        val e = Entorno { json("", HttpStatusCode.OK) }

        AdministracionRepositoryImpl(e.cliente).revocar(3L, "Fin de estadia").getOrThrow()

        val peticion = e.peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertTrue(peticion.url.encodedPath.endsWith("api/v1/autorizaciones/3/revocar"))
        assertTrue(cuerpoDe(e).contains("\"motivo\":\"Fin de estadia\""))
    }

    @Test
    fun `terminarSemestre sin permiso informa SinPermiso y no lo oculta`() = runTest {
        val e = Entorno { json("""{"status":403,"error":"Forbidden"}""", HttpStatusCode.Forbidden) }

        val r = AdministracionRepositoryImpl(e.cliente).terminarSemestre()

        assertEquals(ErrorApi.SinPermiso, errorDe(r))
    }

    @Test
    fun `liberar un residente inexistente informa NoEncontrado`() = runTest {
        val e = Entorno { json("""{"status":404,"error":"Not Found"}""", HttpStatusCode.NotFound) }

        val r = AdministracionRepositoryImpl(e.cliente).liberar(77L)

        assertEquals(ErrorApi.NoEncontrado, errorDe(r))
    }

    @Test
    fun `crear un semestre repetido conserva el mensaje del servidor`() = runTest {
        val e = Entorno {
            json("""{"status":409,"error":"Conflict","message":"Ya existe un semestre 2027-1"}""", HttpStatusCode.Conflict)
        }

        val r = AdministracionRepositoryImpl(e.cliente).crearSemestre(NuevoSemestreAdmin("2027-1", "2027-03-01", "2027-07-31"))

        val error = assertIs<ErrorApi.Conflicto>(errorDe(r))
        assertEquals("Ya existe un semestre 2027-1", error.mensaje)
    }

    @Test
    fun `eliminarCulto usa DELETE sobre la sesion`() = runTest {
        val e = Entorno { json("", HttpStatusCode.OK) }

        AccionesPastorRepositoryImpl(e.cliente).eliminarCulto(12L).getOrThrow()

        val peticion = e.peticiones.single()
        assertEquals(HttpMethod.Delete, peticion.method)
        assertTrue(peticion.url.encodedPath.endsWith("api/v1/cultos/sesiones/12"))
    }

    @Test
    fun `cancelarReserva del pastor envia el motivo`() = runTest {
        val e = Entorno { json("", HttpStatusCode.OK) }

        AccionesPastorRepositoryImpl(e.cliente).cancelarReserva(5L, "Cierre del comedor").getOrThrow()

        val peticion = e.peticiones.single()
        assertTrue(peticion.url.encodedPath.endsWith("api/v1/reservas/5/cancelar-pastor"))
        assertTrue(cuerpoDe(e).contains("\"motivo\":\"Cierre del comedor\""))
    }
}
