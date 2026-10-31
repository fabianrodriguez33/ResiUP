package pe.upeu.resi.resiup.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso

/**
 * Datos de CONSULTA del residente con la base local como fuente única que observa la UI (Offline-First de lectura).
 * [sincronizar] descarga del servidor y reemplaza la copia local. Las operaciones que cambian estado
 * (solicitar, cancelar, justificar) y toda autorización de garita NO pasan por aquí: requieren conexión.
 */
interface ConsultasResidenteRepository {
    fun perfil(): Flow<Perfil?>
    fun asistencias(): Flow<List<AsistenciaCulto>>
    fun evaluaciones(): Flow<List<EvaluacionLimpieza>>
    fun resumenLimpieza(): Flow<ResumenLimpieza?>
    fun solicitudes(): Flow<List<SolicitudPermiso>>

    /** Milisegundos de la última sincronización completa; null si todavía no hay copia válida. */
    fun ultimaSincronizacion(): Flow<Long?>

    /** Falla con el primer error; lo que sí se pudo descargar queda guardado. */
    suspend fun sincronizar(): Result<Unit>

    /** Borra la copia local (cierre de sesión o cambio de usuario). */
    fun borrarTodo()

    fun asegurarPropietario(usuario: String)
}
