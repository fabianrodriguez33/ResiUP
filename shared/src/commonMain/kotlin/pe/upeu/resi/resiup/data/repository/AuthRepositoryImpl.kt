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
        val sesion = store.leer() ?: return null
        if (sesion.haExpirado(ahoraMillis())) {
            store.borrar()
            return null
        }
        return sesion
    }

    override suspend fun cerrarSesion() = store.borrar()
}
