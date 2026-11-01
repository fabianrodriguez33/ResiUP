package pe.upeu.resi.resiup.data.repository

import pe.upeu.resi.resiup.data.remote.CultoApi
import pe.upeu.resi.resiup.data.remote.GaritaApi
import pe.upeu.resi.resiup.data.remote.FamiliaApi
import pe.upeu.resi.resiup.data.remote.LimpiezaApi
import pe.upeu.resi.resiup.data.remote.PermisoApi
import pe.upeu.resi.resiup.data.remote.ResidenteApi
import pe.upeu.resi.resiup.data.remote.dto.DecisionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.JustificacionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.LimpiezaHabitacionRequestDto
import pe.upeu.resi.resiup.data.remote.dto.PuntajePersonalDto
import pe.upeu.resi.resiup.domain.model.NuevaEvaluacionHabitacion
import pe.upeu.resi.resiup.data.remote.dto.SolicitudRequestDto
import pe.upeu.resi.resiup.data.remote.dto.aApi
import pe.upeu.resi.resiup.data.remote.dto.aDominio
import pe.upeu.resi.resiup.data.remote.dto.aPerfil
import pe.upeu.resi.resiup.data.remote.dto.aRegistrada
import pe.upeu.resi.resiup.data.remote.dto.aResumen
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.QrPersonal
import pe.upeu.resi.resiup.data.local.SesionStore
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.domain.repository.CultoRepository
import pe.upeu.resi.resiup.domain.repository.GaritaRepository
import pe.upeu.resi.resiup.domain.repository.FamiliaRepository
import pe.upeu.resi.resiup.domain.repository.LimpiezaRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository

const val TAMANIO_PAGINA = 20

class ResidenteRepositoryImpl(private val api: ResidenteApi, private val sesiones: SesionStore) : ResidenteRepository {
    override suspend fun perfil() = ejecutarLlamada { api.perfil().aPerfil() }
    /**
     * Con conexión se pide al servidor (y se renueva la copia cifrada). Sin red se muestra la última copia: el servidor
     * decide en garita/culto si sigue vigente, así que un QR revocado no sirve aunque se vea en el teléfono.
     */
    override suspend fun qr(): Result<QrPersonal> {
        val r = ejecutarLlamada { QrPersonal(api.qr().contenido) }
        r.onSuccess { sesiones.guardarQr(it.contenido) }
        val error = (r.exceptionOrNull() as? ErrorApiException)?.error
        if (error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado) {
            sesiones.qr()?.let { return Result.success(QrPersonal(it, guardado = true)) }
        }
        return r
    }
    override suspend fun asistencias(pagina: Int) = ejecutarLlamada { api.asistencias(pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun limpieza(pagina: Int) = ejecutarLlamada { api.limpieza(pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun resumenLimpieza() = ejecutarLlamada { api.resumenLimpieza().aDominio() }
    override suspend fun movimientos(pagina: Int) = ejecutarLlamada { api.movimientos(pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun listarResidentes() = ejecutarLlamada { api.listar(0, 100).contenido.filter { it.activo }.map { it.aResumen() } }
}

class PermisoRepositoryImpl(private val api: PermisoApi) : PermisoRepository {
    override suspend fun crear(solicitud: NuevaSolicitud) = ejecutarLlamada {
        api.crear(solicitud.aDto()).aDominio()
    }
    override suspend fun mias(pagina: Int) = ejecutarLlamada { api.mias(pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun detalle(id: Long) = ejecutarLlamada { api.detalle(id).aDominio() }
    override suspend fun cancelar(id: Long) = ejecutarLlamada { api.cancelar(id).aDominio() }
    override suspend fun justificar(id: Long, motivo: String) = ejecutarLlamada { api.justificar(id, JustificacionRequestDto(motivo)) }
    override suspend fun listar(estado: EstadoSolicitud?, pagina: Int) =
        ejecutarLlamada { api.listar(estado?.aApi(), pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun decidir(id: Long, aprobar: Boolean, comentario: String?) =
        ejecutarLlamada { api.decidir(id, DecisionRequestDto(aprobar, comentario?.takeIf { it.isNotBlank() })).aDominio() }
    override suspend fun enlaceApoderado(id: Long) = ejecutarLlamada { api.enlaceApoderado(id).aDominio() }
    override suspend fun resumenDelResidente(id: Long) = ejecutarLlamada { api.resumen(id).aDominio() }
}

private fun NuevaSolicitud.aDto() = SolicitudRequestDto(
    tipo.aApi(), motivo, destino, salida.toString(), retorno.toString(),
    residenteId, responsableNombre?.takeIf { it.isNotBlank() }, responsableCelular?.takeIf { it.isNotBlank() }, acompanante?.takeIf { it.isNotBlank() },
)

class FamiliaRepositoryImpl(private val api: FamiliaApi) : FamiliaRepository {
    override suspend fun hijos() = ejecutarLlamada { api.hijos().map { it.aDominio() } }
    override suspend fun solicitudes(estado: EstadoSolicitud?, pagina: Int) =
        ejecutarLlamada { api.solicitudes(estado?.aApi(), pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
    override suspend fun detalle(id: Long) = ejecutarLlamada { api.detalle(id).aDominio() }
    override suspend fun crear(solicitud: NuevaSolicitud) = ejecutarLlamada { api.crear(solicitud.aDto()).aDominio() }
    override suspend fun decidir(id: Long, aprobar: Boolean) = ejecutarLlamada { api.decidir(id, aprobar).aDominio() }
    override suspend fun cancelar(id: Long) = ejecutarLlamada { api.cancelar(id).aDominio() }
}

class CultoRepositoryImpl(private val api: CultoApi) : CultoRepository {
    override suspend fun abrirSesion(tipo: TipoCulto) = ejecutarLlamada { api.abrir(tipo.aApi()).aDominio() }
    override suspend fun sesionesDeHoy() = ejecutarLlamada { api.hoy().map { it.aDominio() } }
    override suspend fun registrarAsistencia(sesionId: Long, qr: String) = ejecutarLlamada { api.registrar(sesionId, qr).aRegistrada() }
    override suspend fun cerrarSesion(sesionId: Long) = ejecutarLlamada { api.cerrar(sesionId).aDominio() }
}

class LimpiezaRepositoryImpl(private val api: LimpiezaApi) : LimpiezaRepository {
    override suspend fun habitaciones(fecha: String) = ejecutarLlamada { api.habitaciones(fecha).map { it.aDominio() } }

    override suspend fun registrarHabitacion(evaluacion: NuevaEvaluacionHabitacion) = ejecutarLlamada {
        api.registrarHabitacion(
            evaluacion.habitacionId,
            LimpiezaHabitacionRequestDto(
                evaluacion.fecha, evaluacion.basura, evaluacion.olores,
                evaluacion.personas.map { PuntajePersonalDto(it.residenteId, it.ropero, it.cama, it.escritorio, it.piso) },
            ),
        ).map { it.aDominio() }
    }
}

class GaritaRepositoryImpl(private val api: GaritaApi) : GaritaRepository {
    override suspend fun verificar(qr: String) = ejecutarLlamada { api.verificar(qr).aDominio() }
    override suspend fun registrar(qr: String, tipo: TipoMovimiento) = ejecutarLlamada { api.registrar(qr, tipo.aApi()).aDominio() }
    override suspend fun historial(pagina: Int) = ejecutarLlamada { api.historial(pagina, TAMANIO_PAGINA).aDominio { it.aDominio() } }
}
