package pe.upeu.resi.resiup.data

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.Entorno
import pe.upeu.resi.resiup.data.remote.PermisoApi
import pe.upeu.resi.resiup.data.repository.PermisoRepositoryImpl
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.json
import kotlin.time.Instant

private const val SOLICITUD = """{"id":7,"residenteCodigo":"A001","residenteNombre":"Ana","tipo":"CORTO","motivo":"Compras",
    "destino":"Centro","salidaSolicitada":"2026-11-23T10:00:00Z","retornoSolicitado":"2026-11-23T12:00:00Z",
    "retornoAutorizado":"2026-11-23T12:00:00Z","estado":"PENDIENTE_PRECEPTOR","creadoEn":"2026-11-22T10:00:00Z"}"""

class PermisoRepositoryTest {

    private fun errorDe(r: Result<*>): ErrorApi = (r.exceptionOrNull() as ErrorApiException).error

    private fun cuerpoDe(e: Entorno): String = (e.peticiones.single().body as TextContent).text

    private fun nueva(motivo: String = "Compras") = NuevaSolicitud(
        TipoPermiso.Corto, motivo, "Centro",
        Instant.parse("2026-11-23T10:00:00Z"), Instant.parse("2026-11-23T12:00:00Z"),
    )

    @Test
    fun `crear envia la solicitud y mapea la respuesta`() = runTest {
        val e = Entorno { json(SOLICITUD) }

        val creada = PermisoRepositoryImpl(PermisoApi(e.cliente)).crear(nueva()).getOrThrow()

        assertEquals(7L, creada.id)
        assertEquals(TipoPermiso.Corto, creada.tipo)
        assertEquals(EstadoSolicitud.PendientePreceptor, creada.estado)
        val peticion = e.peticiones.single()
        assertEquals(HttpMethod.Post, peticion.method)
        assertTrue(peticion.url.encodedPath.endsWith("api/v1/permisos/solicitudes"))
        assertTrue(cuerpoDe(e).contains("\"motivo\":\"Compras\""))
    }

    @Test
    fun `mias pide la pagina indicada y devuelve la pagina del servidor`() = runTest {
        val e = Entorno {
            json("""{"contenido":[$SOLICITUD],"pagina":2,"tamanio":20,"totalElementos":21,"totalPaginas":2,"ultima":true}""")
        }

        val pagina = PermisoRepositoryImpl(PermisoApi(e.cliente)).mias(2).getOrThrow()

        assertEquals(7L, pagina.contenido.single().id)
        assertEquals(2, e.peticiones.single().url.parameters["pagina"]?.toInt())
        assertTrue(pagina.ultima)
    }

    @Test
    fun `justificar envia el motivo a la solicitud`() = runTest {
        val e = Entorno { json("", HttpStatusCode.OK) }

        PermisoRepositoryImpl(PermisoApi(e.cliente)).justificar(7L, "Cita médica").getOrThrow()

        val peticion = e.peticiones.single()
        assertTrue(peticion.url.encodedPath.endsWith("api/v1/permisos/solicitudes/7/justificaciones"))
        assertTrue(cuerpoDe(e).contains("\"motivo\":\"Cita médica\""))
    }

    @Test
    fun `anular quita los espacios del motivo antes de enviarlo`() = runTest {
        val e = Entorno { json(SOLICITUD) }

        PermisoRepositoryImpl(PermisoApi(e.cliente)).anular(7L, "  duplicado  ").getOrThrow()

        assertTrue(cuerpoDe(e).contains("\"motivo\":\"duplicado\""), cuerpoDe(e))
    }

    @Test
    fun `decidir con comentario en blanco no envia comentario`() = runTest {
        val e = Entorno { json(SOLICITUD) }

        PermisoRepositoryImpl(PermisoApi(e.cliente)).decidir(7L, true, "   ").getOrThrow()

        val cuerpo = cuerpoDe(e)
        assertTrue(cuerpo.contains("\"aprobar\":true"), cuerpo)
        assertFalse(cuerpo.contains("   "), cuerpo)
    }

    @Test
    fun `aprobadosHoy devuelve el conteo del servidor`() = runTest {
        val e = Entorno { json("""{"aprobados":4}""") }

        assertEquals(4L, PermisoRepositoryImpl(PermisoApi(e.cliente)).aprobadosHoy().getOrThrow())
    }

    @Test
    fun `listar sin estado no manda el filtro`() = runTest {
        val e = Entorno { json("""{"contenido":[],"pagina":0,"tamanio":20,"ultima":true}""") }

        PermisoRepositoryImpl(PermisoApi(e.cliente)).listar(null, 0).getOrThrow()

        assertEquals(null, e.peticiones.single().url.parameters["estado"])
    }

    @Test
    fun `detalle de una solicitud inexistente informa NoEncontrado`() = runTest {
        val e = Entorno { json("""{"status":404,"error":"Not Found"}""", HttpStatusCode.NotFound) }

        val r = PermisoRepositoryImpl(PermisoApi(e.cliente)).detalle(999L)

        assertEquals(ErrorApi.NoEncontrado, errorDe(r))
    }
}
