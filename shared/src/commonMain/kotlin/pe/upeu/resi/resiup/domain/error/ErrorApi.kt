package pe.upeu.resi.resiup.domain.error

/** Errores tipados que la capa de datos entrega al dominio y a la UI. */
sealed interface ErrorApi {
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi
    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi

    /** Login con usuario o contraseña incorrectos (401 en /auth/login). */
    data object CredencialesInvalidas : ErrorApi

    /** 401 en cualquier otro endpoint: el token venció o fue invalidado. Se debe cerrar la sesión local. */
    data object SesionExpirada : ErrorApi

    /** 403: la sesión es válida pero el rol no tiene permiso. La sesión se mantiene. */
    data object SinPermiso : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
    data object RolNoSoportado : ErrorApi
}

class ErrorApiException(val error: ErrorApi) : Exception(error.toString())
