package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.ContactoEditable
import pe.upeu.resi.resiup.domain.model.DiaSinCulto
import pe.upeu.resi.resiup.domain.model.PerfilCuenta

/** Datos personales y verificación del correo de cualquier rol. Son operaciones en línea. */
interface PerfilCuentaRepository {
    suspend fun perfil(): Result<PerfilCuenta>
    suspend fun actualizar(cambios: ContactoEditable): Result<PerfilCuenta>

    /** Envía un código de 6 dígitos al correo guardado. */
    suspend fun enviarCodigo(): Result<Unit>
    suspend fun verificarCorreo(codigo: String): Result<PerfilCuenta>
}

interface CalendarioRepository {
    suspend fun diasSinCulto(): Result<List<DiaSinCulto>>
    suspend fun marcar(fecha: String, motivo: String): Result<DiaSinCulto>
    suspend fun quitar(fecha: String): Result<Unit>
}
