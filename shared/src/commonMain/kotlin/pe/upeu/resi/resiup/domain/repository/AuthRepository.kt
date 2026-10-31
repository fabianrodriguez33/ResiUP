package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Sesion

interface AuthRepository {
    suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion>

    /** Sesión guardada y todavía vigente; null si no hay o ya venció (en ese caso se elimina). */
    suspend fun sesionGuardada(): Sesion?

    suspend fun cerrarSesion()
}
