package pe.upeu.resi.resiup.data.repository

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException

private val json = Json { ignoreUnknownKeys = true }

/**
 * Consulta en línea con copia local: con conexión se pide al servidor y se renueva la copia; sin conexión (o con tiempo agotado)
 * se devuelve la última copia guardada, si la hay. Cualquier otro error (403, 404…) se informa tal cual.
 */
suspend fun <T> conRespaldoLocal(locales: DatosLocales?, clave: String, serializador: KSerializer<T>, llamada: suspend () -> T): Result<T> {
    val r = ejecutarLlamada { llamada() }
    r.onSuccess { runCatching { locales?.guardarCopia(clave, json.encodeToString(serializador, it)) } }
    val error = (r.exceptionOrNull() as? ErrorApiException)?.error
    if (error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado) {
        val copia = runCatching { locales?.leerCopia(clave)?.let { json.decodeFromString(serializador, it) } }.getOrNull()
        if (copia != null) return Result.success(copia)
    }
    return r
}
