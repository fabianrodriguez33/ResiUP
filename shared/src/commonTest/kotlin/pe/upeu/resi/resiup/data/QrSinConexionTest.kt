package pe.upeu.resi.resiup.data

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.Entorno
import pe.upeu.resi.resiup.RESPUESTA_LOGIN
import pe.upeu.resi.resiup.data.remote.ResidenteApi
import pe.upeu.resi.resiup.data.repository.ResidenteRepositoryImpl
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.json

class QrSinConexionTest {
    private val credenciales = Credenciales("ana", "clave-segura")
    private var hayRed = true
    private var estado = HttpStatusCode.OK

    private fun armar(): Pair<Entorno, ResidenteRepositoryImpl> {
        val e = Entorno {
            when {
                it.url.encodedPath.endsWith("auth/login") -> json(RESPUESTA_LOGIN)
                !hayRed -> throw Exception("sin red")
                else -> json("""{"contenido":"RESIUP1.abc.firma"}""", estado)
            }
        }
        return e to ResidenteRepositoryImpl(ResidenteApi(e.cliente), e.store)
    }

    @Test
    fun `con conexion se guarda el QR y sin conexion se muestra la copia`() = runTest {
        val (e, repo) = armar()
        e.repositorio.iniciarSesion(credenciales).getOrThrow()

        val enLinea = repo.qr().getOrThrow()
        assertEquals("RESIUP1.abc.firma", enLinea.contenido)
        assertFalse(enLinea.guardado)

        hayRed = false
        val sinRed = repo.qr().getOrThrow()
        assertEquals("RESIUP1.abc.firma", sinRed.contenido)
        assertTrue(sinRed.guardado, "debe avisar que es la copia del teléfono")
    }

    @Test
    fun `sin conexion y sin copia previa se informa el error`() = runTest {
        val (e, repo) = armar()
        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        hayRed = false
        val error = (repo.qr().exceptionOrNull() as ErrorApiException).error
        assertEquals(ErrorApi.SinConexion, error)
    }

    @Test
    fun `un error del servidor no se disfraza con la copia guardada`() = runTest {
        val (e, repo) = armar()
        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        repo.qr().getOrThrow()
        estado = HttpStatusCode.Forbidden
        val error = (repo.qr().exceptionOrNull() as ErrorApiException).error
        assertEquals(ErrorApi.SinPermiso, error)
    }

    @Test
    fun `cerrar sesion o iniciar otra borra el QR guardado`() = runTest {
        val (e, repo) = armar()
        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        repo.qr().getOrThrow()
        assertEquals("RESIUP1.abc.firma", e.store.qr())

        e.repositorio.cerrarSesion()
        assertNull(e.store.qr())

        e.repositorio.iniciarSesion(credenciales).getOrThrow()
        assertNull(e.store.qr(), "una sesión nueva no hereda el QR anterior")
    }
}
