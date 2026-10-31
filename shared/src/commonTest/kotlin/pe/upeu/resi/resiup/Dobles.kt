package pe.upeu.resi.resiup

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import pe.upeu.resi.resiup.data.local.AlmacenSeguro
import pe.upeu.resi.resiup.data.local.SesionStore
import pe.upeu.resi.resiup.data.remote.AuthApi
import pe.upeu.resi.resiup.data.remote.ConfigEntorno
import pe.upeu.resi.resiup.data.remote.SesionEventos
import pe.upeu.resi.resiup.data.remote.crearHttpClient
import pe.upeu.resi.resiup.data.repository.AuthRepositoryImpl

class AlmacenEnMemoria(var valor: String? = null) : AlmacenSeguro {
    override fun guardar(valor: String) { this.valor = valor }
    override fun leer(): String? = valor
    override fun borrar() { valor = null }
}

const val RESPUESTA_LOGIN = """{"token":"jwt-de-prueba","tipo":"Bearer","expiraEnSegundos":3600,"username":"ana","nombreCompleto":"Ana Pérez","roles":["RESIDENTE"],"permisos":["PERFIL_VER","QR_VER"],"extra":"se ignora"}"""

fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

/** Armado completo de la capa de datos sobre un MockEngine. */
class Entorno(
    val almacen: AlmacenEnMemoria = AlmacenEnMemoria(),
    var ahora: Long = 1_000_000L,
    manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    val peticiones = mutableListOf<HttpRequestData>()
    val eventos = SesionEventos()
    val store = SesionStore(almacen)
    val cliente: HttpClient = crearHttpClient(
        MockEngine { request -> peticiones += request; manejador(request) },
        ConfigEntorno("http://api.test/", depuracion = false), store, eventos,
    )
    val repositorio = AuthRepositoryImpl(AuthApi(cliente), store) { ahora }
}
