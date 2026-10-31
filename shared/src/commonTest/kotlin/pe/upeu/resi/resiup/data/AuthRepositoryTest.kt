package pe.upeu.resi.resiup.data

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.Entorno
import pe.upeu.resi.resiup.RESPUESTA_LOGIN
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.json

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val credenciales = Credenciales("ana", "clave-segura")

    private fun errorDe(r: Result<*>): ErrorApi = (r.exceptionOrNull() as ErrorApiException).error

    @Test
    fun `login correcto devuelve la sesion, la guarda cifrada y calcula el vencimiento`() = runTest {
        val e = Entorno { json(RESPUESTA_LOGIN) }
        val sesion = e.repositorio.iniciarSesion(credenciales).getOrThrow()

        assertEquals("ana", sesion.usuario)
        assertEquals(Rol.Residente, sesion.rol)
        assertTrue(sesion.puede("QR_VER"))
        assertEquals(1_000_000L + 3_600_000L, sesion.expiraEnMillis)
        assertNotNull(e.almacen.valor, "la sesión debe quedar persistida en el almacén seguro")
        assertEquals("jwt-de-prueba", e.store.token())
        assertNull(e.peticiones.single().headers[HttpHeaders.Authorization], "el login no envía token")
    }

    @Test
    fun `credenciales incorrectas se distinguen de una sesion vencida`() = runTest {
        val e = Entorno { json("""{"status":401,"error":"Unauthorized","message":"Credenciales inválidas"}""", HttpStatusCode.Unauthorized) }
        val r = e.repositorio.iniciarSesion(credenciales)
        assertEquals(ErrorApi.CredencialesInvalidas, errorDe(r))
        assertNull(e.almacen.valor)
    }

    @Test
    fun `error 400 conserva los mensajes por campo`() = runTest {
        val e = Entorno { json("""{"status":400,"error":"Bad Request","message":"Datos inválidos","validationErrors":{"username":"obligatorio"}}""", HttpStatusCode.BadRequest) }
        val error = errorDe(e.repositorio.iniciarSesion(credenciales)) as ErrorApi.Validacion
        assertEquals("obligatorio", error.porCampo["username"])
    }

    @Test
    fun `un rol desconocido no inicia sesion y no guarda nada`() = runTest {
        val e = Entorno { json(RESPUESTA_LOGIN.replace("RESIDENTE", "VISITANTE")) }
        assertEquals(ErrorApi.RolNoSoportado, errorDe(e.repositorio.iniciarSesion(credenciales)))
        assertNull(e.almacen.valor)
    }

    @Test
    fun `fallas de red, tiempo agotado y 5xx se traducen a errores tipados`() = runTest {
        val sinRed = Entorno { throw Exception("sin red") }
        assertEquals(ErrorApi.SinConexion, errorDe(sinRed.repositorio.iniciarSesion(credenciales)))

        val lento = Entorno { request -> throw HttpRequestTimeoutException(request.url.toString(), 1) }
        assertEquals(ErrorApi.TiempoAgotado, errorDe(lento.repositorio.iniciarSesion(credenciales)))

        val caido = Entorno { json("{}", HttpStatusCode.InternalServerError) }
        assertEquals(ErrorApi.Servidor, errorDe(caido.repositorio.iniciarSesion(credenciales)))

        val raro = Entorno { json("esto no es json") }
        assertEquals(ErrorApi.Servidor, errorDe(raro.repositorio.iniciarSesion(credenciales)))
    }

    @Test
    fun `403 mantiene la sesion y 401 fuera del login avisa que expiro`() = runTest {
        var estado = HttpStatusCode.Forbidden
        val e = Entorno { json("""{"status":${estado.value},"error":"x","message":"x"}""", estado) }
        var avisos = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { e.eventos.expirada.collect { avisos++ } }

        assertEquals(ErrorApi.SinPermiso, errorDe(ejecutarLlamada { e.cliente.get("api/v1/usuarios") }))
        assertEquals(0, avisos, "un 403 no cierra la sesión")

        estado = HttpStatusCode.Unauthorized
        assertEquals(ErrorApi.SesionExpirada, errorDe(ejecutarLlamada { e.cliente.get("api/v1/usuarios") }))
        assertEquals(1, avisos)

        estado = HttpStatusCode.Conflict
        val conflicto = errorDe(ejecutarLlamada { e.cliente.get("api/v1/x") }) as ErrorApi.Conflicto
        assertEquals("x", conflicto.mensaje)
        estado = HttpStatusCode.NotFound
        assertEquals(ErrorApi.NoEncontrado, errorDe(ejecutarLlamada { e.cliente.get("api/v1/x") }))
    }

    @Test
    fun `un 401 del login no se confunde con sesion expirada`() = runTest {
        val e = Entorno { json("{}", HttpStatusCode.Unauthorized) }
        var avisos = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { e.eventos.expirada.collect { avisos++ } }
        e.repositorio.iniciarSesion(credenciales)
        assertEquals(0, avisos)
    }

    @Test
    fun `las peticiones autenticadas envian el token Bearer`() = runTest {
        val e = Entorno { request -> if (request.url.encodedPath.endsWith("/auth/login")) json(RESPUESTA_LOGIN) else json("{}") }
        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        e.cliente.get("api/v1/auth/me")
        assertEquals("Bearer jwt-de-prueba", e.peticiones.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `la cancelacion de corrutinas nunca se oculta`() = runTest {
        assertFailsWith<CancellationException> { ejecutarLlamada<Unit> { throw CancellationException("cancelada") } }
    }

    @Test
    fun `una sesion vencida se descarta al restaurar y una vigente se conserva`() = runTest {
        val e = Entorno { json(RESPUESTA_LOGIN) }
        e.repositorio.iniciarSesion(credenciales).getOrThrow()

        e.ahora += 1_000L
        assertNotNull(e.repositorio.sesionGuardada())

        e.ahora += 3_600_000L // pasó la hora de vigencia
        assertNull(e.repositorio.sesionGuardada())
        assertNull(e.almacen.valor, "la sesión vencida se elimina del almacén")
    }

    @Test
    fun `un contenido guardado danado se trata como sin sesion`() = runTest {
        val e = Entorno(almacen = pe.upeu.resi.resiup.AlmacenEnMemoria("{no es json")) { json("{}") }
        assertNull(e.repositorio.sesionGuardada())
        assertNull(e.almacen.valor)
    }

    @Test
    fun `cerrar sesion borra el almacen y el token en memoria`() = runTest {
        val e = Entorno { json(RESPUESTA_LOGIN) }
        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        e.repositorio.cerrarSesion()
        assertNull(e.almacen.valor)
        assertNull(e.store.token())
        assertFalse(e.almacen.valor != null)
    }
}
