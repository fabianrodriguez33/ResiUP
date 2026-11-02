package pe.upeu.resi.resiup.data.remote

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.upeu.resi.resiup.data.remote.dto.ErrorResponseDto
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException

/**
 * Ejecuta una llamada de red y traduce cualquier fallo a [ErrorApi].
 * [esLogin] distingue un 401 por credenciales incorrectas de un 401 por sesión vencida.
 */
suspend fun <T> ejecutarLlamada(esLogin: Boolean = false, bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion // nunca se oculta: rompería la cancelación estructurada
} catch (e: ErrorApiException) {
    Result.failure(e)
} catch (e: ClientRequestException) {
    val cuerpo = runCatching { e.response.body<ErrorResponseDto>() }.getOrNull()
    val error = when (e.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        401 -> if (esLogin) ErrorApi.CredencialesInvalidas else ErrorApi.SesionExpirada
        403 -> ErrorApi.SinPermiso
        404 -> ErrorApi.NoEncontrado
        408 -> ErrorApi.TiempoAgotado
        429 -> ErrorApi.Conflicto(cuerpo?.message?.takeIf { it.isNotBlank() } ?: "Demasiados intentos. Espera un momento.")
        409 -> ErrorApi.Conflicto(cuerpo?.message?.takeIf { it.isNotBlank() } ?: "La operación entra en conflicto con el estado actual")
        else -> ErrorApi.Servidor
    }
    Result.failure(ErrorApiException(error))
} catch (e: ServerResponseException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor))
} catch (e: HttpRequestTimeoutException) {
    Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
} catch (e: ConnectTimeoutException) {
    Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
} catch (e: SocketTimeoutException) {
    Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
} catch (e: SerializationException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor)) // respuesta con formato inesperado
} catch (e: ContentConvertException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor))
} catch (e: NoTransformationFoundException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor))
} catch (e: Exception) {
    Result.failure(ErrorApiException(ErrorApi.SinConexion))
}
