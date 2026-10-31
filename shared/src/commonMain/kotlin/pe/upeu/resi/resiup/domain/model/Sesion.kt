package pe.upeu.resi.resiup.domain.model

enum class Rol(val etiqueta: String) {
    Residente("Residente"),
    Preceptor("Preceptor"),
    Garita("Garita"),
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
) {
    fun puede(permiso: String): Boolean = permiso in permisos

    fun haExpirado(ahoraMillis: Long): Boolean = ahoraMillis >= expiraEnMillis
}
