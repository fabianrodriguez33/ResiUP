package pe.upeu.resi.resiup.data.remote.dto

import kotlin.time.Instant
import pe.upeu.resi.resiup.domain.model.Ampliacion
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.AsistenciaRegistrada
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EnlaceApoderado
import pe.upeu.resi.resiup.domain.model.EstadoRevision
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import pe.upeu.resi.resiup.domain.model.HabitacionResumen
import pe.upeu.resi.resiup.domain.model.Hijo
import pe.upeu.resi.resiup.domain.model.OrigenSolicitud
import pe.upeu.resi.resiup.domain.model.ResumenResidente
import pe.upeu.resi.resiup.domain.model.ResidenteLimpieza
import pe.upeu.resi.resiup.domain.model.Justificacion
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.ResidenteResumen
import pe.upeu.resi.resiup.domain.model.ResultadoGarita
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.SesionCulto
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.model.VerificacionGarita

private fun String.instante(): Instant = Instant.parse(this)

fun String.aTipoCulto(): TipoCulto = if (equals("VESPERTINO", true)) TipoCulto.Vespertino else TipoCulto.Matutino

fun TipoCulto.aApi(): String = name.uppercase()

fun String.aTipoMovimiento(): TipoMovimiento = if (equals("RETORNO", true)) TipoMovimiento.Retorno else TipoMovimiento.Salida

fun TipoMovimiento.aApi(): String = name.uppercase()

fun TipoPermiso.aApi(): String = name.uppercase()

fun CriterioLimpieza.aApi(): String = when (this) {
    CriterioLimpieza.Cama -> "CAMA"
    else -> name.uppercase()
}

private fun String.aCriterio(): CriterioLimpieza? = CriterioLimpieza.entries.firstOrNull { it.aApi() == this }

private fun String.aTipoPermiso(): TipoPermiso = TipoPermiso.entries.firstOrNull { it.aApi() == this } ?: TipoPermiso.Pernocte

private fun String.aEstadoSolicitud(): EstadoSolicitud = when (this) {
    "BORRADOR" -> EstadoSolicitud.Borrador
    "PENDIENTE_APODERADO" -> EstadoSolicitud.PendienteApoderado
    "PENDIENTE_PRECEPTOR" -> EstadoSolicitud.PendientePreceptor
    "APROBADO" -> EstadoSolicitud.Aprobado
    "RECHAZADO" -> EstadoSolicitud.Rechazado
    "CANCELADO" -> EstadoSolicitud.Cancelado
    else -> EstadoSolicitud.Vencido
}

fun EstadoSolicitud.aApi(): String = when (this) {
    EstadoSolicitud.PendienteApoderado -> "PENDIENTE_APODERADO"
    EstadoSolicitud.PendientePreceptor -> "PENDIENTE_PRECEPTOR"
    else -> name.uppercase()
}

private fun String.aEstadoRevision(): EstadoRevision = when (this) {
    "APROBADA" -> EstadoRevision.Aprobada
    "RECHAZADA" -> EstadoRevision.Rechazada
    else -> EstadoRevision.Pendiente
}

fun <D, M> PaginaDto<D>.aDominio(mapeo: (D) -> M) = Pagina(contenido.map(mapeo), pagina, totalElementos, ultima)

fun ResidenteDto.aPerfil() = Perfil(id, codigo, nombreCompleto, telefono, habitacion?.let { HabitacionResumen(it.id, it.numero, it.cama) }, puedeSolicitarSolo, email, carrera, ciclo)

fun ResidenteDto.aResumen() = ResidenteResumen(id, codigo, nombreCompleto, habitacion?.numero, activo)

fun AsistenciaDto.aDominio() = AsistenciaCulto(id, fecha, tipo.aTipoCulto(), registradoEn.instante(), despuesDelCorte)

fun AsistenciaDto.aRegistrada() = AsistenciaRegistrada(codigo, nombreCompleto, despuesDelCorte)

fun EvaluacionDto.aDominio() = EvaluacionLimpieza(
    id, fecha, total, maximo,
    puntajes.mapNotNull { (k, v) -> k.aCriterio()?.let { it to v } }.toMap(),
    observacion,
)

fun ResumenLimpiezaDto.aDominio() = ResumenLimpieza(evaluaciones, promedio, ultimoTotal, ultimaFecha)

fun SolicitudDto.aDominio() = SolicitudPermiso(
    id = id, residenteCodigo = residenteCodigo, residenteNombre = residenteNombre, tipo = tipo.aTipoPermiso(), motivo = motivo,
    destino = destino, salida = salidaSolicitada.instante(), retornoSolicitado = retornoSolicitado.instante(),
    retornoAutorizado = retornoAutorizado.instante(), estado = estado.aEstadoSolicitud(), requiereApoderado = requiereApoderado,
    comentarioDecision = comentarioDecision, creadoEn = creadoEn.instante(),
    ampliaciones = ampliaciones.orEmpty().map { Ampliacion(it.id, it.motivo, it.retornoNuevo.instante(), it.estado.aEstadoRevision()) },
    justificaciones = justificaciones.orEmpty().map { Justificacion(it.id, it.motivo, it.estado.aEstadoRevision(), it.comentario) },
    origen = if (origen == "APODERADO") OrigenSolicitud.Apoderado else OrigenSolicitud.Residente,
    responsableNombre = responsableNombre, responsableCelular = responsableCelular, acompanante = acompanante,
)

fun HijoDto.aDominio() = Hijo(residenteId, codigo, nombreCompleto, habitacion, cama, puedeSolicitarSolo, pendientes)

fun ResumenResidenteDto.aDominio() = ResumenResidente(dias, cultosRealizados, asistencias, despuesDelCorte, promedioLimpieza, evaluaciones)

fun EnlaceDto.aDominio() = EnlaceApoderado(token, ruta, expiraEn.instante(), apoderado)

fun SesionCultoDto.aDominio() = SesionCulto(id, fecha, tipo.aTipoCulto(), estado == "CERRADA", presentes, corte.instante())

private fun String.aResultado(): ResultadoGarita = when (this) {
    "AUTORIZADO_SALIDA" -> ResultadoGarita.AutorizadoSalida
    "AUTORIZADO_RETORNO" -> ResultadoGarita.AutorizadoRetorno
    "RETORNO_FUERA_DE_HORARIO" -> ResultadoGarita.RetornoFueraDeHorario
    "QR_INVALIDO" -> ResultadoGarita.QrInvalido
    "QR_REVOCADO" -> ResultadoGarita.QrRevocado
    "RESIDENTE_INACTIVO" -> ResultadoGarita.ResidenteInactivo
    "SIN_PERMISO" -> ResultadoGarita.SinPermiso
    "PERMISO_AUN_NO_VIGENTE" -> ResultadoGarita.PermisoAunNoVigente
    "PERMISO_VENCIDO" -> ResultadoGarita.PermisoVencido
    else -> ResultadoGarita.Desconocido
}

fun VerificacionDto.aDominio() = VerificacionGarita(
    resultado = resultado.aResultado(), mensaje = mensaje, nombreCompleto = residente?.nombreCompleto, codigo = residente?.codigo,
    habitacion = residente?.habitacion, movimientoPermitido = movimientoPermitido?.aTipoMovimiento(),
    retornoAutorizado = retornoAutorizado?.instante(), minutosTarde = minutosTarde,
)

fun MovimientoDto.aDominio() = MovimientoRegistrado(id, codigo, nombreCompleto, tipo.aTipoMovimiento(), registradoEn.instante(), minutosTarde, anulado)

fun HabitacionLimpiezaDto.aDominio() = HabitacionLimpieza(
    habitacionId, numero, capacidad,
    residentes.map { ResidenteLimpieza(it.residenteId, it.codigo, it.nombreCompleto, it.cama, it.evaluado) },
    evaluados.toInt(),
)
