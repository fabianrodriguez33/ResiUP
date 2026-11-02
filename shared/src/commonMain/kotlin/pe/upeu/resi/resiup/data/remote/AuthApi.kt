package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pe.upeu.resi.resiup.data.remote.dto.LoginRequestDto
import pe.upeu.resi.resiup.data.remote.dto.LoginResponseDto

class AuthApi(private val client: HttpClient) {
    suspend fun login(request: LoginRequestDto): LoginResponseDto =
        client.post("api/v1/auth/login") { setBody(request) }.body()

    suspend fun olvideClave(usuario: String) {
        client.post("api/v1/auth/olvide-clave") { setBody(pe.upeu.resi.resiup.data.remote.dto.OlvideClaveRequestDto(usuario)) }
    }

    suspend fun cambiarClave(actual: String, nueva: String) {
        client.post("api/v1/auth/cambiar-clave") { setBody(pe.upeu.resi.resiup.data.remote.dto.CambiarClaveRequestDto(actual, nueva)) }
    }
}
