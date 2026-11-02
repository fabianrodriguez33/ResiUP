package pe.upeu.resi.resiup.domain.model

enum class Rol(val etiqueta: String) {
    Residente("Residente"),
    Preceptor("Pastor"),
    Garita("Garita"),
    Apoderado("Padre/madre"),
    Administrador("Administrador"),
}

data class Credenciales(val usuario: String, val clave: String)

/** Sesión autenticada. El backend sigue siendo la autoridad: estos datos sólo orientan la interfaz. */
data class Sesion(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val rol: Rol,
    val permisos: Set<String>,
    val expiraEnMillis: Long,
    /** Entró con una contraseña provisional: la app le exige elegir una nueva antes de seguir. */
    val debeCambiarClave: Boolean = false,
) {
    fun puede(permiso: String): Boolean = permiso in permisos

    fun haExpirado(ahoraMillis: Long): Boolean = ahoraMillis >= expiraEnMillis
}
