package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.AsistenciaRegistrada
import pe.upeu.resi.resiup.domain.model.EnlaceApoderado
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import pe.upeu.resi.resiup.domain.model.NuevaEvaluacionHabitacion
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.QrPersonal
import pe.upeu.resi.resiup.domain.model.ResidenteResumen
import pe.upeu.resi.resiup.domain.model.Hijo
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.ResumenResidente
import pe.upeu.resi.resiup.domain.model.SesionCulto
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.domain.model.VerificacionGarita

interface ResidenteRepository {
    suspend fun perfil(): Result<Perfil>
    suspend fun qr(): Result<QrPersonal>
    suspend fun asistencias(pagina: Int): Result<Pagina<AsistenciaCulto>>
    suspend fun limpieza(pagina: Int): Result<Pagina<EvaluacionLimpieza>>
    suspend fun resumenLimpieza(): Result<ResumenLimpieza>
    suspend fun movimientos(pagina: Int): Result<Pagina<MovimientoRegistrado>>
    suspend fun listarResidentes(): Result<List<ResidenteResumen>>
}

interface PermisoRepository {
    suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso>
    suspend fun mias(pagina: Int): Result<Pagina<SolicitudPermiso>>
    suspend fun detalle(id: Long): Result<SolicitudPermiso>
    suspend fun cancelar(id: Long): Result<SolicitudPermiso>
    suspend fun justificar(id: Long, motivo: String): Result<Unit>
    suspend fun listar(estado: EstadoSolicitud?, pagina: Int): Result<Pagina<SolicitudPermiso>>
    suspend fun decidir(id: Long, aprobar: Boolean, comentario: String?): Result<SolicitudPermiso>
    suspend fun enlaceApoderado(id: Long): Result<EnlaceApoderado>
    suspend fun resumenDelResidente(id: Long): Result<ResumenResidente>
}

/** Perfil de padres de familia: sus hijos y los permisos de ellos. */
interface FamiliaRepository {
    suspend fun hijos(): Result<List<Hijo>>
    suspend fun solicitudes(estado: EstadoSolicitud?, pagina: Int): Result<Pagina<SolicitudPermiso>>
    suspend fun detalle(id: Long): Result<SolicitudPermiso>
    suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso>
    suspend fun decidir(id: Long, aprobar: Boolean): Result<SolicitudPermiso>
    suspend fun cancelar(id: Long): Result<SolicitudPermiso>
}

interface CultoRepository {
    suspend fun abrirSesion(tipo: TipoCulto): Result<SesionCulto>
    suspend fun sesionesDeHoy(): Result<List<SesionCulto>>
    suspend fun registrarAsistencia(sesionId: Long, qr: String): Result<AsistenciaRegistrada>
    suspend fun cerrarSesion(sesionId: Long): Result<SesionCulto>
}

interface LimpiezaRepository {
    suspend fun habitaciones(fecha: String): Result<List<HabitacionLimpieza>>
    suspend fun registrarHabitacion(evaluacion: NuevaEvaluacionHabitacion): Result<List<EvaluacionLimpieza>>
}

interface GaritaRepository {
    suspend fun verificar(qr: String): Result<VerificacionGarita>
    suspend fun registrar(qr: String, tipo: TipoMovimiento): Result<MovimientoRegistrado>
    suspend fun historial(pagina: Int): Result<Pagina<MovimientoRegistrado>>
}
