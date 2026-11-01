package pe.upeu.resi.resiup.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.upeu.resi.resiup.db.ResiDb
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.HabitacionResumen
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoPermiso

/**
 * Base SQLite local (SQLDelight) con la última copia de CONSULTA del residente. Es de sólo lectura para la UI:
 * se escribe únicamente al sincronizar con el servidor. No guarda contraseñas, tokens ni el QR.
 */
class DatosLocales(db: ResiDb) {
    private val q = db.consultaQueries

    private companion object {
        const val PROPIETARIO = "propietario"
        const val SINCRONIZADO_EN = "sincronizado_en"
    }

    // ---------------------------------------------------------------- lectura reactiva

    fun perfil(): Flow<Perfil?> = q.observarPerfil().asFlow().mapToOneOrNull(Dispatchers.Default).map { f ->
        f?.let { Perfil(it.id, it.codigo, it.nombre, it.telefono, it.habitacion_id?.let { id -> HabitacionResumen(id, it.habitacion_numero.orEmpty(), it.habitacion_cama) }, it.puede_solicitar_solo != 0L, it.email, it.carrera, it.ciclo?.toInt()) }
    }

    fun asistencias(): Flow<List<AsistenciaCulto>> = q.observarAsistencias().asFlow().mapToList(Dispatchers.Default).map { filas ->
        filas.map { AsistenciaCulto(it.id, it.fecha, enumONull<TipoCulto>(it.tipo) ?: TipoCulto.Matutino, Instant.parse(it.registrado_en), it.despues_corte != 0L) }
    }

    fun evaluaciones(): Flow<List<EvaluacionLimpieza>> = q.observarEvaluaciones().asFlow().mapToList(Dispatchers.Default).map { filas ->
        filas.map { EvaluacionLimpieza(it.id, it.fecha, it.total.toInt(), it.maximo.toInt(), puntajesDesdeTexto(it.puntajes), it.observacion) }
    }

    fun resumenLimpieza(): Flow<ResumenLimpieza?> = q.observarResumen().asFlow().mapToOneOrNull(Dispatchers.Default).map { f ->
        f?.let { ResumenLimpieza(it.evaluaciones, it.promedio, it.ultimo_total?.toInt(), it.ultima_fecha) }
    }

    fun solicitudes(): Flow<List<SolicitudPermiso>> = q.observarSolicitudes().asFlow().mapToList(Dispatchers.Default).map { filas ->
        filas.map {
            SolicitudPermiso(
                id = it.id, residenteCodigo = it.residente_codigo, residenteNombre = it.residente_nombre,
                tipo = enumONull<TipoPermiso>(it.tipo) ?: TipoPermiso.Pernocte, motivo = it.motivo, destino = it.destino,
                salida = Instant.parse(it.salida), retornoSolicitado = Instant.parse(it.retorno_solicitado),
                retornoAutorizado = Instant.parse(it.retorno_autorizado), estado = enumONull<EstadoSolicitud>(it.estado) ?: EstadoSolicitud.Vencido,
                requiereApoderado = it.requiere_apoderado != 0L, comentarioDecision = it.comentario_decision, creadoEn = Instant.parse(it.creado_en),
                ampliaciones = emptyList(), justificaciones = emptyList(), // el detalle completo se consulta en línea
            )
        }
    }

    /** Milisegundos de la última sincronización completa, o null si nunca se completó. */
    fun ultimaSincronizacion(): Flow<Long?> = q.leerMeta(SINCRONIZADO_EN).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.toLongOrNull() }

    // ---------------------------------------------------------------- escritura (sólo al sincronizar)

    fun reemplazarPerfil(p: Perfil) = q.transaction {
        q.borrarPerfil()
        q.guardarPerfil(p.id, p.codigo, p.nombreCompleto, p.telefono, p.habitacion?.id, p.habitacion?.numero, p.habitacion?.cama, if (p.puedeSolicitarSolo) 1L else 0L, p.email, p.carrera, p.ciclo?.toLong())
    }

    fun reemplazarAsistencias(lista: List<AsistenciaCulto>) = q.transaction {
        q.borrarAsistencias()
        lista.forEach { q.guardarAsistencia(it.id, it.fecha, it.tipo.name, it.registradoEn.toString(), if (it.despuesDelCorte) 1 else 0) }
    }

    fun reemplazarEvaluaciones(lista: List<EvaluacionLimpieza>) = q.transaction {
        q.borrarEvaluaciones()
        lista.forEach { q.guardarEvaluacion(it.id, it.fecha, it.total.toLong(), it.maximo.toLong(), puntajesATexto(it.puntajes), it.observacion) }
    }

    fun reemplazarResumen(r: ResumenLimpieza) = q.transaction {
        q.borrarResumen()
        q.guardarResumen(r.evaluaciones, r.promedio, r.ultimoTotal?.toLong(), r.ultimaFecha)
    }

    fun reemplazarSolicitudes(lista: List<SolicitudPermiso>) = q.transaction {
        q.borrarSolicitudes()
        lista.forEach {
            q.guardarSolicitud(
                it.id, it.residenteCodigo, it.residenteNombre, it.tipo.name, it.motivo, it.destino, it.salida.toString(),
                it.retornoSolicitado.toString(), it.retornoAutorizado.toString(), it.estado.name, if (it.requiereApoderado) 1 else 0,
                it.comentarioDecision, it.creadoEn.toString(),
            )
        }
    }

    fun marcarSincronizado(ahoraMillis: Long) = q.guardarMeta(SINCRONIZADO_EN, ahoraMillis.toString())

    // ---------------------------------------------------------------- propietario y limpieza

    fun propietario(): String? = q.leerMeta(PROPIETARIO).executeAsOneOrNull()

    /** Si los datos guardados pertenecen a otro usuario se descartan antes de mostrar nada. */
    fun asegurarPropietario(usuario: String) {
        if (propietario() != usuario) {
            borrarTodo()
            q.guardarMeta(PROPIETARIO, usuario)
        }
    }

    fun borrarTodo() = q.transaction {
        q.borrarPerfil(); q.borrarAsistencias(); q.borrarEvaluaciones(); q.borrarResumen(); q.borrarSolicitudes(); q.borrarMeta()
    }
}

private inline fun <reified E : Enum<E>> enumONull(nombre: String): E? = enumValues<E>().firstOrNull { it.name == nombre }

private fun puntajesATexto(p: Map<CriterioLimpieza, Int>) = p.entries.joinToString(";") { "${it.key.name}=${it.value}" }

private fun puntajesDesdeTexto(t: String): Map<CriterioLimpieza, Int> = t.split(';').mapNotNull { par ->
    val (k, v) = par.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
    val criterio = CriterioLimpieza.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
    v.toIntOrNull()?.let { criterio to it }
}.toMap()
