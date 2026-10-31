package pe.upeu.resi.resiup.data.remote.dto

import kotlinx.serialization.Serializable
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String> = emptyList(),
)

@Serializable
data class ErrorResponseDto(
    val status: Int = 0,
    val error: String = "",
    val message: String = "",
    val validationErrors: Map<String, String>? = null,
)

fun String.aRol(): Rol? = when (this) {
    "RESIDENTE" -> Rol.Residente
    "PRECEPTOR" -> Rol.Preceptor
    "GARITA" -> Rol.Garita
    "ADMINISTRADOR" -> Rol.Administrador
    else -> null
}

/** El vencimiento se calcula con el reloj local a partir de la duración que informa el servidor. */
fun LoginResponseDto.aSesion(ahoraMillis: Long): Sesion {
    val rol = roles.firstNotNullOfOrNull { it.aRol() } ?: throw ErrorApiException(ErrorApi.RolNoSoportado)
    return Sesion(
        token = token,
        usuario = username,
        nombreCompleto = nombreCompleto,
        rol = rol,
        permisos = permisos.toSet(),
        expiraEnMillis = ahoraMillis + expiraEnSegundos * 1000,
    )
}
