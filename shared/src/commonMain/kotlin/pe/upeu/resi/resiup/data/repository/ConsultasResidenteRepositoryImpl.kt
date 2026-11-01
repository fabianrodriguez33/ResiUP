package pe.upeu.resi.resiup.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository

/** Máximo de páginas que se guardan localmente por lista (3 × 20 registros más recientes). */
private const val MAX_PAGINAS = 3

class ConsultasResidenteRepositoryImpl(
    private val remoto: ResidenteRepository,
    private val permisos: PermisoRepository,
    private val calendario: pe.upeu.resi.resiup.domain.repository.CalendarioRepository,
    private val local: DatosLocales,
    private val ahoraMillis: () -> Long,
) : ConsultasResidenteRepository {

    override fun perfil() = local.perfil()
    override fun asistencias() = local.asistencias()
    override fun evaluaciones() = local.evaluaciones()
    override fun resumenLimpieza() = local.resumenLimpieza()
    override fun solicitudes() = local.solicitudes()
    override fun diasSinCulto() = local.diasSinCulto()
    override fun ultimaSincronizacion() = local.ultimaSincronizacion()
    override fun borrarTodo() = local.borrarTodo()
    override fun asegurarPropietario(usuario: String) = local.asegurarPropietario(usuario)

    override suspend fun sincronizar(): Result<Unit> = coroutineScope {
        val perfil = async { remoto.perfil() }
        val asistencias = async { traerTodo { remoto.asistencias(it) } }
        val evaluaciones = async { traerTodo { remoto.limpieza(it) } }
        val resumen = async { remoto.resumenLimpieza() }
        val solicitudes = async { traerTodo { permisos.mias(it) } }
        val diasSinCulto = async { calendario.diasSinCulto() }

        var primerError: Throwable? = null
        fun <T> Result<T>.guardar(accion: (T) -> Unit) {
            onSuccess(accion).onFailure { if (primerError == null) primerError = it }
        }
        perfil.await().guardar { local.reemplazarPerfil(it) }
        asistencias.await().guardar { local.reemplazarAsistencias(it) }
        evaluaciones.await().guardar { local.reemplazarEvaluaciones(it) }
        resumen.await().guardar { local.reemplazarResumen(it) }
        solicitudes.await().guardar { local.reemplazarSolicitudes(it) }
        diasSinCulto.await().guardar { local.reemplazarDiasSinCulto(it) }

        // La copia sólo se considera vigente si TODO se descargó; así nunca se mezcla una mitad nueva con una vieja sin avisar.
        val error = primerError
        if (error == null) {
            local.marcarSincronizado(ahoraMillis())
            Result.success(Unit)
        } else {
            Result.failure(error)
        }
    }

    private suspend fun <T> traerTodo(pedir: suspend (Int) -> Result<Pagina<T>>): Result<List<T>> {
        val todos = mutableListOf<T>()
        var pagina = 0
        while (pagina < MAX_PAGINAS) {
            val r = pedir(pagina)
            val p = r.getOrElse { return Result.failure(it) }
            todos += p.contenido
            if (p.ultima) break
            pagina++
        }
        return Result.success(todos)
    }
}
