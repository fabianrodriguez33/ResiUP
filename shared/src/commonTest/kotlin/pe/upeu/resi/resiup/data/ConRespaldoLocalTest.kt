package pe.upeu.resi.resiup.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import pe.upeu.resi.resiup.data.repository.conRespaldoLocal
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException

/** Casos sin base local: el respaldo sólo decide según el error, sin copia que devolver. */
class ConRespaldoLocalTest {

    private val serializador = ListSerializer(String.serializer())

    private fun errorDe(r: Result<*>): ErrorApi = (r.exceptionOrNull() as ErrorApiException).error

    @Test
    fun `con conexion devuelve la respuesta del servidor`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { listOf("en linea") }

        assertEquals(listOf("en linea"), r.getOrThrow())
    }

    @Test
    fun `sin conexion y sin base local se informa el error de conexion`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { throw ErrorApiException(ErrorApi.SinConexion) }

        assertEquals(ErrorApi.SinConexion, errorDe(r))
    }

    @Test
    fun `tiempo agotado sin base local se informa tal cual`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { throw ErrorApiException(ErrorApi.TiempoAgotado) }

        assertEquals(ErrorApi.TiempoAgotado, errorDe(r))
    }

    @Test
    fun `un error del servidor no se disfraza`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { throw ErrorApiException(ErrorApi.Servidor) }

        assertEquals(ErrorApi.Servidor, errorDe(r))
    }

    @Test
    fun `un 403 se informa aunque exista copia`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { throw ErrorApiException(ErrorApi.SinPermiso) }

        assertEquals(ErrorApi.SinPermiso, errorDe(r))
    }

    @Test
    fun `una excepcion de red cualquiera se traduce a SinConexion`() = runTest {
        val r = conRespaldoLocal(null, "clave", serializador) { throw IllegalStateException("cable desconectado") }

        assertEquals(ErrorApi.SinConexion, errorDe(r))
    }
}
