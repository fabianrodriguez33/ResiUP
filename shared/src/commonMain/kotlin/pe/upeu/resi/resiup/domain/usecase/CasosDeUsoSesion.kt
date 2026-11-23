package pe.upeu.resi.resiup.domain.usecase

import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.domain.repository.AuthRepository

/** Valida el formato de los campos (el servidor valida de nuevo) y delega el login en el repositorio. */
/** Solo los caracteres que el servidor acepta en un usuario o código; al pegar desde un chat pueden colarse caracteres invisibles. */
fun String.comoUsuario(): String = filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '_' || it == '-' }

class IniciarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke(usuario: String, clave: String): Result<Sesion> {
        val limpio = usuario.comoUsuario()
        val errores = buildMap {
            if (limpio.isEmpty()) put("username", "Ingresa tu usuario o código")
            if (clave.isEmpty()) put("password", "Ingresa tu contraseña")
        }
        if (errores.isNotEmpty()) return Result.failure(ErrorApiException(ErrorApi.Validacion(errores)))
        return repositorio.iniciarSesion(Credenciales(limpio, clave))
    }
}

class ObtenerSesionGuardadaUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke(): Sesion? = repositorio.sesionGuardada()
}

class CerrarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke() = repositorio.cerrarSesion()
}
