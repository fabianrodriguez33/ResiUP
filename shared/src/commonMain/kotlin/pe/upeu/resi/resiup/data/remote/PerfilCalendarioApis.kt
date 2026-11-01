package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import pe.upeu.resi.resiup.data.remote.dto.CodigoCorreoRequestDto
import pe.upeu.resi.resiup.data.remote.dto.ContactoRequestDto
import pe.upeu.resi.resiup.data.remote.dto.DiaSinCultoDto
import pe.upeu.resi.resiup.data.remote.dto.PerfilCuentaDto

private const val BASE_API = "api/v1/"

class PerfilCuentaApi(private val client: HttpClient) {
    suspend fun perfil(): PerfilCuentaDto = client.get("${BASE_API}perfil").body()
    suspend fun actualizar(r: ContactoRequestDto): PerfilCuentaDto = client.patch("${BASE_API}perfil") { setBody(r) }.body()
    suspend fun enviarCodigo() {
        client.post("${BASE_API}perfil/correo/codigo")
    }
    suspend fun verificar(codigo: String): PerfilCuentaDto = client.post("${BASE_API}perfil/correo/verificar") { setBody(CodigoCorreoRequestDto(codigo)) }.body()
}

class CalendarioApi(private val client: HttpClient) {
    suspend fun diasSinCulto(): List<DiaSinCultoDto> = client.get("${BASE_API}calendario/dias-sin-culto").body()
    suspend fun marcar(fecha: String, motivo: String): DiaSinCultoDto = client.post("${BASE_API}calendario/dias-sin-culto") { setBody(DiaSinCultoDto(fecha, motivo)) }.body()
    suspend fun quitar(fecha: String) {
        client.delete("${BASE_API}calendario/dias-sin-culto/$fecha")
    }
}
