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
import pe.upeu.resi.resiup.domain.model.PerfilEditable
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
    suspend fun ranking(): Result<pe.upeu.resi.resiup.domain.model.Ranking> = Result.failure(UnsupportedOperationException())
    /** Ranking y notas de un rango de fechas (una semana, por ejemplo). */
    suspend fun rankingDe(desde: String, hasta: String): Result<pe.upeu.resi.resiup.domain.model.Ranking> = Result.failure(UnsupportedOperationException())
    /** Id de la evidencia que es su foto de perfil (null si no tiene). */
    suspend fun fotoPerfil(): Result<Long?> = Result.failure(UnsupportedOperationException())
    suspend fun cambiarFoto(evidenciaId: Long): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun asistencias(pagina: Int): Result<Pagina<AsistenciaCulto>>
    /** Fechas (aaaa-mm-dd) en las que hubo culto en el rango; el residente las usa para distinguir «sin registro» de «no hubo culto». */
    suspend fun diasConCulto(desde: String, hasta: String): Result<List<String>> = Result.failure(UnsupportedOperationException())
    suspend fun limpieza(pagina: Int): Result<Pagina<EvaluacionLimpieza>>
    suspend fun resumenLimpieza(): Result<ResumenLimpieza>
    suspend fun movimientos(pagina: Int): Result<Pagina<MovimientoRegistrado>>
    suspend fun listarResidentes(): Result<List<ResidenteResumen>>
    suspend fun actualizarPerfil(cambios: PerfilEditable): Result<Perfil>
}

interface PermisoRepository {
    suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso>
    suspend fun mias(pagina: Int): Result<Pagina<SolicitudPermiso>>
    /** Garita: permisos aprobados. */
    suspend fun listarGarita(pagina: Int): Result<Pagina<SolicitudPermiso>> = Result.failure(UnsupportedOperationException())
    suspend fun detalle(id: Long): Result<SolicitudPermiso>
    suspend fun cancelar(id: Long): Result<SolicitudPermiso>
    suspend fun justificar(id: Long, motivo: String): Result<Unit>
    suspend fun listar(estado: EstadoSolicitud?, pagina: Int): Result<Pagina<SolicitudPermiso>>
    /** Permisos aprobados hoy (vuelve a empezar cada día). */
    suspend fun aprobadosHoy(): Result<Long> = Result.failure(UnsupportedOperationException())
    suspend fun enlaceApoderado(id: Long): Result<EnlaceApoderado>
    suspend fun resumenDelResidente(id: Long): Result<ResumenResidente>

    /** Solo el pastor: anula un permiso aprobado que el residente todavía no usó. */
    suspend fun anular(id: Long, motivo: String): Result<SolicitudPermiso>

    /** Solo el pastor: aprueba o rechaza un permiso en revisión; rechazar exige el motivo. */
    suspend fun decidir(id: Long, aprobar: Boolean, comentario: String?): Result<SolicitudPermiso> = Result.failure(UnsupportedOperationException())
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
    /** Residentes distintos que asistieron hoy a uno o a los dos cultos. */
    suspend fun presentesHoy(): Result<Long> = Result.failure(UnsupportedOperationException())
    suspend fun registrarAsistencia(sesionId: Long, qr: String): Result<AsistenciaRegistrada>
    suspend fun cerrarSesion(sesionId: Long): Result<SesionCulto>
    suspend fun sesionesDeFecha(fecha: String): Result<List<SesionCulto>> = Result.failure(UnsupportedOperationException())
    suspend fun presentes(sesionId: Long): Result<List<AsistenciaPresente>> = Result.failure(UnsupportedOperationException())
    suspend fun reabrirSesion(sesionId: Long): Result<SesionCulto> = Result.failure(UnsupportedOperationException())
    suspend fun registrarPorCodigo(sesionId: Long, codigo: String): Result<AsistenciaRegistrada> = Result.failure(UnsupportedOperationException())
    suspend fun exportarPresentes(sesionId: Long): Result<String> = Result.failure(UnsupportedOperationException())
}

data class AsistenciaPresente(val codigo: String, val nombreCompleto: String, val despuesDelCorte: Boolean)

interface LimpiezaRepository {
    suspend fun habitaciones(fecha: String): Result<List<HabitacionLimpieza>>
    suspend fun registrarHabitacion(evaluacion: NuevaEvaluacionHabitacion): Result<List<EvaluacionLimpieza>>
    /** Lo ya evaluado en una habitación y fecha (para modificarlo). */
    suspend fun detalleHabitacion(habitacionId: Long, fecha: String): Result<NuevaEvaluacionHabitacion>
    /** Modifica la evaluación de una habitación: reemplaza los puntajes de ese día. */
    suspend fun reemplazarHabitacion(evaluacion: NuevaEvaluacionHabitacion): Result<List<EvaluacionLimpieza>>
}

interface GaritaRepository {
    suspend fun verificar(qr: String, solicitudId: Long? = null): Result<VerificacionGarita>
    suspend fun permisosDe(qr: String): Result<pe.upeu.resi.resiup.domain.model.PermisosDeResidente>
    suspend fun registrar(qr: String, tipo: TipoMovimiento, solicitudId: Long? = null): Result<MovimientoRegistrado>
    suspend fun historial(pagina: Int): Result<Pagina<MovimientoRegistrado>>
}

/** Reservas de alimentación del residente. Solo con conexión: el servidor valida horarios, días cerrados y duplicados. */
interface ReservasRepository {
    suspend fun oferta(): Result<List<pe.upeu.resi.resiup.domain.model.ComidaOfertada>>
    suspend fun mias(): Result<List<pe.upeu.resi.resiup.domain.model.Reserva>>
    suspend fun reservar(n: pe.upeu.resi.resiup.domain.model.NuevaReserva): Result<pe.upeu.resi.resiup.domain.model.Reserva>
    suspend fun cancelar(id: Long): Result<pe.upeu.resi.resiup.domain.model.Reserva>
}
