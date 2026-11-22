package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.*

/** Administración del residencial. Todas las operaciones requieren conexión: el servidor valida cada dato. */
interface AdministracionRepository {
    suspend fun residentes(): Result<List<ResidenteAdmin>>
    suspend fun habitaciones(): Result<List<HabAdmin>>
    suspend fun ocupantes(habitacionId: Long): Result<List<OcupanteAdmin>>
    suspend fun semestres(): Result<List<SemestreAdmin>>
    suspend fun semestreActual(): Result<SemestreAdmin>
    suspend fun terminarSemestre(): Result<Unit>
    suspend fun crearSemestre(s: NuevoSemestreAdmin): Result<SemestreAdmin>
    suspend fun liberar(id: Long): Result<Unit>
    suspend fun crear(r: NuevoResidenteAdmin): Result<ResidenteAdmin>
    suspend fun asignar(id: Long, a: AsignacionAdmin): Result<Unit>
    suspend fun autorizaciones(id: Long): Result<List<AutorizacionAdmin>>
    suspend fun autorizar(id: Long, a: NuevaAutorizacionAdmin): Result<Unit>
    suspend fun revocar(id: Long, motivo: String): Result<Unit>
    suspend fun usuarios(): Result<List<UsuarioAdmin>>
    suspend fun crearUsuario(u: NuevoUsuarioAdmin): Result<UsuarioAdmin>
}

/** Correcciones del pastor, todas con constancia en el servidor. */
interface AccionesPastorRepository {
    suspend fun eliminarLimpieza(habitacionId: Long, fecha: String, motivo: String): Result<Unit>
    suspend fun eliminarLimpiezaDelDia(fecha: String, motivo: String): Result<Unit>
    suspend fun eliminarCulto(sesionId: Long): Result<Unit>
    suspend fun cancelarReserva(id: Long, motivo: String): Result<Unit>
}
