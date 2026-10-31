package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.header
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.upeu.resi.resiup.data.local.SesionStore

/** Configuración por entorno. [apiUrl] termina en «/» (p. ej. http://10.0.2.2:8080/ en el emulador). */
data class ConfigEntorno(val apiUrl: String, val depuracion: Boolean)

private val EnviarToken = createClientPlugin("ResiEnviarToken", ::ConfigToken) {
    val proveedor = pluginConfig.proveedor
    onRequest { request, _ ->
        // El login es el único endpoint que no lleva token.
        if (!request.url.build().encodedPath.endsWith("/auth/login")) {
            proveedor()?.let { request.header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}

private class ConfigToken {
    var proveedor: () -> String? = { null }
}

fun crearHttpClient(
    engine: HttpClientEngine,
    config: ConfigEntorno,
    store: SesionStore,
    eventos: SesionEventos,
): HttpClient = HttpClient(engine) {
    expectSuccess = true // 4xx y 5xx se convierten en excepciones que traduce ejecutarLlamada

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }
    install(EnviarToken) { proveedor = { store.token() } }

    // Un 401 fuera del login significa que el token ya no sirve: se avisa para cerrar la sesión local.
    install(HttpCallValidator) {
        handleResponseExceptionWithRequest { e, request ->
            if (e is ClientRequestException && e.response.status.value == 401 && !request.url.encodedPath.endsWith("/auth/login")) {
                eventos.notificarExpirada()
            }
        }
    }
    if (config.depuracion) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.HEADERS // sin cuerpos: el login incluye la contraseña
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
    }
    defaultRequest {
        url(config.apiUrl)
        contentType(ContentType.Application.Json)
    }
}
