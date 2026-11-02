package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Sesion

interface AuthRepository {
    suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion>

    /** Sesión guardada y todavía vigente; null si no hay o ya venció (en ese caso se elimina). */
    suspend fun sesionGuardada(): Sesion?

    suspend fun cerrarSesion()

    /** Pide una contraseña provisional por correo. Siempre responde igual: no revela si el usuario existe. */
    suspend fun olvideClave(usuario: String): Result<Unit>

    /** Cambia la contraseña de la sesión (sirve también con la provisional) y quita la exigencia de cambiarla. */
    suspend fun cambiarClave(actual: String, nueva: String): Result<Unit>
}
