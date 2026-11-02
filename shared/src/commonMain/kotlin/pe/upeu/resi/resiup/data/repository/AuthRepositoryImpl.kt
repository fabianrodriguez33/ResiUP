package pe.upeu.resi.resiup.data.repository

import pe.upeu.resi.resiup.data.local.SesionStore
import pe.upeu.resi.resiup.data.remote.AuthApi
import pe.upeu.resi.resiup.data.remote.dto.LoginRequestDto
import pe.upeu.resi.resiup.data.remote.dto.aSesion
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val store: SesionStore,
    private val ahoraMillis: () -> Long,
) : AuthRepository {

    override suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion> =
        ejecutarLlamada(esLogin = true) {
            val sesion = api.login(LoginRequestDto(credenciales.usuario, credenciales.clave)).aSesion(ahoraMillis())
            store.guardar(sesion)
            sesion
        }

    override suspend fun sesionGuardada(): Sesion? {
        // Aunque el token ya haya vencido se conserva: sin red el residente sigue viendo sus datos y su QR.
        // En cuanto el servidor responda 401 se cierra la sesión y se pide iniciar de nuevo.
        return store.leer()
    }

    override suspend fun cerrarSesion() = store.borrar()

    override suspend fun olvideClave(usuario: String) = ejecutarLlamada(esLogin = true) { api.olvideClave(usuario.trim()) }

    override suspend fun cambiarClave(actual: String, nueva: String): Result<Unit> = ejecutarLlamada {
        api.cambiarClave(actual, nueva)
        store.claveCambiada()
    }
}
