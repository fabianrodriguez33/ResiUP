package pe.upeu.resi.resiup.data.repository

import pe.upeu.resi.resiup.data.remote.CalendarioApi
import pe.upeu.resi.resiup.data.remote.PerfilCuentaApi
import pe.upeu.resi.resiup.data.remote.dto.ContactoRequestDto
import pe.upeu.resi.resiup.data.remote.dto.PerfilCuentaDto
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.ContactoEditable
import pe.upeu.resi.resiup.domain.model.DiaSinCulto
import pe.upeu.resi.resiup.domain.model.PerfilCuenta
import pe.upeu.resi.resiup.domain.repository.CalendarioRepository
import pe.upeu.resi.resiup.domain.repository.PerfilCuentaRepository

class PerfilCuentaRepositoryImpl(private val api: PerfilCuentaApi) : PerfilCuentaRepository {
    override suspend fun perfil() = ejecutarLlamada { api.perfil().aDominio() }
    override suspend fun actualizar(cambios: ContactoEditable) = ejecutarLlamada {
        api.actualizar(ContactoRequestDto(cambios.email.trim(), cambios.telefono.trim(), cambios.fechaNacimiento)).aDominio()
    }
    override suspend fun enviarCodigo() = ejecutarLlamada { api.enviarCodigo() }
    override suspend fun verificarCorreo(codigo: String) = ejecutarLlamada { api.verificar(codigo.trim()).aDominio() }
}

private fun PerfilCuentaDto.aDominio() = PerfilCuenta(username, nombreCompleto, dni, fechaNacimiento, email, emailVerificado, telefono)

class CalendarioRepositoryImpl(private val api: CalendarioApi) : CalendarioRepository {
    override suspend fun diasSinCulto() = ejecutarLlamada { api.diasSinCulto().map { DiaSinCulto(it.fecha, it.motivo) } }
    override suspend fun marcar(fecha: String, motivo: String) = ejecutarLlamada { api.marcar(fecha, motivo.trim()).let { DiaSinCulto(it.fecha, it.motivo) } }
    override suspend fun quitar(fecha: String) = ejecutarLlamada { api.quitar(fecha) }
}
