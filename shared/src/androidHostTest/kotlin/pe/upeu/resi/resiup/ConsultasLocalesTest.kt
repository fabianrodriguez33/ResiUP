package pe.upeu.resi.resiup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.data.repository.ConsultasResidenteRepositoryImpl
import pe.upeu.resi.resiup.db.ResiDb
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.HabitacionResumen
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.QrPersonal
import pe.upeu.resi.resiup.domain.model.ResidenteResumen
import pe.upeu.resi.resiup.domain.model.ResumenLimpieza
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.util.Consulta
import pe.upeu.resi.resiup.presentation.util.EstadoSync
import pe.upeu.resi.resiup.presentation.util.Sincronizador
import pe.upeu.resi.resiup.presentation.util.comoConsulta
import pe.upeu.resi.resiup.presentation.util.unDecimal

private val T0 = Instant.parse("2026-10-30T21:00:00Z")

private fun baseEnMemoria(): DatosLocales {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ResiDb.Schema.create(driver)
    return DatosLocales(ResiDb(driver))
}

private fun asistencia(id: Long) = AsistenciaCulto(id, "2026-10-30", TipoCulto.Vespertino, T0, despuesDelCorte = id % 2 == 0L)

private fun solicitud(id: Long, estado: EstadoSolicitud = EstadoSolicitud.Aprobado) = SolicitudPermiso(
    id, "A001", "Ana", TipoPermiso.Corto, "Compras", "Centro", T0, T0, T0, estado, true, "ok", T0, emptyList(), emptyList(),
)

private fun <T> pagina(items: List<T>, ultima: Boolean = true) = Result.success(Pagina(items, 0, items.size.toLong(), ultima))

private class ResidenteRemoto : ResidenteRepository {
    var caido: ErrorApi? = null
    var paginasAsistencias = 0
    var asistenciasSinFin = false
    override suspend fun perfil() = caido?.let { Result.failure(ErrorApiException(it)) } ?: Result.success(Perfil(1, "A001", "Ana Pérez", null, HabitacionResumen(3, "D-12")))
    override suspend fun qr() = Result.success(QrPersonal("no se guarda"))
    override suspend fun asistencias(pagina: Int): Result<Pagina<AsistenciaCulto>> {
        paginasAsistencias++
        return caido?.let { Result.failure(ErrorApiException(it)) } ?: pagina(listOf(asistencia(pagina * 2L + 1), asistencia(pagina * 2L + 2)), ultima = !asistenciasSinFin && pagina == 0)
    }
    override suspend fun limpieza(pagina: Int) = caido?.let { Result.failure(ErrorApiException(it)) }
        ?: pagina(listOf(EvaluacionLimpieza(1, "2026-10-29", 17, 20, mapOf(CriterioLimpieza.Ropero to 5, CriterioLimpieza.Piso to 2), "Bien")))
    override suspend fun resumenLimpieza() = caido?.let { Result.failure(ErrorApiException(it)) } ?: Result.success(ResumenLimpieza(1, 17.0, 17, "2026-10-29"))
    override suspend fun movimientos(pagina: Int) = pagina<MovimientoRegistrado>(emptyList())
    override suspend fun listarResidentes() = Result.success(emptyList<ResidenteResumen>())
    override suspend fun actualizarPerfil(cambios: pe.upeu.resi.resiup.domain.model.PerfilEditable) = Result.failure<Perfil>(ErrorApiException(ErrorApi.NoEncontrado))
}

private class PermisosRemoto(val remoto: ResidenteRemoto) : PermisoRepository {
    override suspend fun crear(solicitud: NuevaSolicitud) = Result.failure<SolicitudPermiso>(ErrorApiException(ErrorApi.Servidor))
    override suspend fun mias(pagina: Int) = remoto.caido?.let { Result.failure(ErrorApiException(it)) } ?: pagina(listOf(solicitud(9)))
    override suspend fun detalle(id: Long) = Result.success(solicitud(id))
    override suspend fun cancelar(id: Long) = Result.success(solicitud(id))
    override suspend fun justificar(id: Long, motivo: String) = Result.success(Unit)
    override suspend fun listar(estado: EstadoSolicitud?, pagina: Int) = pagina<SolicitudPermiso>(emptyList())
    override suspend fun enlaceApoderado(id: Long) = Result.failure<pe.upeu.resi.resiup.domain.model.EnlaceApoderado>(ErrorApiException(ErrorApi.NoEncontrado))
    override suspend fun resumenDelResidente(id: Long) = Result.failure<pe.upeu.resi.resiup.domain.model.ResumenResidente>(ErrorApiException(ErrorApi.NoEncontrado))
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConsultasLocalesTest {

    private fun armar(ahora: () -> Long = { 5_000L }): Triple<ConsultasResidenteRepositoryImpl, ResidenteRemoto, DatosLocales> {
        val local = baseEnMemoria()
        val remoto = ResidenteRemoto()
        return Triple(ConsultasResidenteRepositoryImpl(remoto, PermisosRemoto(remoto), local, ahora), remoto, local)
    }

    @Test
    fun `la base local guarda, observa y reemplaza sin duplicar`() = runTest {
        val local = baseEnMemoria()
        local.reemplazarAsistencias(listOf(asistencia(1), asistencia(2)))
        assertEquals(listOf(1L, 2L), local.asistencias().first().map { it.id }.sorted())
        local.reemplazarAsistencias(listOf(asistencia(2), asistencia(3), asistencia(4)))
        assertEquals(listOf(2L, 3L, 4L), local.asistencias().first().map { it.id }.sorted(), "reemplazar descarta lo que ya no existe en el servidor")

        local.reemplazarEvaluaciones(listOf(EvaluacionLimpieza(1, "2026-10-29", 17, 20, mapOf(CriterioLimpieza.Ropero to 5, CriterioLimpieza.Olores to 2), null)))
        assertEquals(mapOf(CriterioLimpieza.Ropero to 5, CriterioLimpieza.Olores to 2), local.evaluaciones().first().single().puntajes)

        local.reemplazarSolicitudes(listOf(solicitud(7, EstadoSolicitud.PendienteApoderado)))
        val s = local.solicitudes().first().single()
        assertEquals(EstadoSolicitud.PendienteApoderado, s.estado)
        assertEquals(T0, s.salida)
        assertTrue(s.requiereApoderado)

        local.reemplazarPerfil(Perfil(1, "A001", "Ana", "999", null))
        assertNull(local.perfil().first()?.habitacion)
        local.reemplazarPerfil(Perfil(1, "A001", "Ana", "999", HabitacionResumen(3, "D-12")))
        assertEquals("D-12", local.perfil().first()?.habitacion?.numero)
    }

    @Test
    fun `sincronizar descarga todo, lo guarda y marca la fecha`() = runTest {
        val (repo, _, _) = armar { 12_345L }
        assertNull(repo.ultimaSincronizacion().first(), "antes de sincronizar no hay copia válida")
        assertTrue(repo.sincronizar().isSuccess)
        assertEquals("Ana Pérez", repo.perfil().first()?.nombreCompleto)
        assertEquals(2, repo.asistencias().first().size)
        assertEquals(17, repo.evaluaciones().first().single().total)
        assertEquals(17.0, repo.resumenLimpieza().first()?.promedio)
        assertEquals(9L, repo.solicitudes().first().single().id)
        assertEquals(12_345L, repo.ultimaSincronizacion().first())
    }

    @Test
    fun `sin conexion se conserva la ultima copia y no se marca como actualizada`() = runTest {
        var reloj = 1_000L
        val (repo, remoto, _) = armar { reloj }
        repo.sincronizar().getOrThrow()

        reloj = 99_000L
        remoto.caido = ErrorApi.SinConexion
        val r = repo.sincronizar()
        assertEquals(ErrorApi.SinConexion, (r.exceptionOrNull() as ErrorApiException).error)
        assertEquals("Ana Pérez", repo.perfil().first()?.nombreCompleto, "los datos guardados siguen disponibles")
        assertEquals(2, repo.asistencias().first().size)
        assertEquals(1_000L, repo.ultimaSincronizacion().first(), "la fecha de última actualización no avanza si falló")
    }

    @Test
    fun `se guardan como maximo tres paginas por lista`() = runTest {
        val (repo, remoto, _) = armar()
        remoto.asistenciasSinFin = true
        repo.sincronizar().getOrThrow()
        assertEquals(3, remoto.paginasAsistencias)
        assertEquals(6, repo.asistencias().first().size)
    }

    @Test
    fun `los datos de otro usuario se descartan y los propios se conservan`() = runTest {
        val (repo, _, _) = armar()
        repo.asegurarPropietario("ana")
        repo.sincronizar().getOrThrow()
        repo.asegurarPropietario("ana")
        assertNotNull(repo.perfil().first())

        repo.asegurarPropietario("luis")
        assertNull(repo.perfil().first(), "otro usuario no ve el perfil del anterior")
        assertTrue(repo.asistencias().first().isEmpty())
        assertNull(repo.ultimaSincronizacion().first())
    }

    @Test
    fun `cerrar sesion borra todo lo guardado`() = runTest {
        val (repo, _, _) = armar()
        repo.sincronizar().getOrThrow()
        repo.borrarTodo()
        assertNull(repo.perfil().first())
        assertTrue(repo.solicitudes().first().isEmpty())
        assertTrue(repo.evaluaciones().first().isEmpty())
        assertNull(repo.resumenLimpieza().first())
        assertNull(repo.ultimaSincronizacion().first())
    }

    @Test
    fun `el sincronizador informa errores, limita la frecuencia y permite reintentar`() = runTest {
        var reloj = 0L
        val (repo, remoto, _) = armar { reloj }
        val sync = Sincronizador(repo) { reloj }
        remoto.caido = ErrorApi.SinConexion

        sync.refrescar()
        esperar { !sync.estado.value.sincronizando }
        assertEquals(ErrorApi.SinConexion, sync.estado.value.error)

        remoto.caido = null
        reloj = 1_000L
        sync.refrescar() // hay un error previo: se permite reintentar sin esperar
        esperar { !sync.estado.value.sincronizando }
        assertNull(sync.estado.value.error)
        assertEquals(1_000L, repo.ultimaSincronizacion().first())

        val antes = remoto.paginasAsistencias
        reloj = 2_000L
        sync.refrescar() // sincronizó hace 1 s: no repite
        esperar { !sync.estado.value.sincronizando }
        assertEquals(antes, remoto.paginasAsistencias)

        sync.refrescar(forzar = true)
        esperar { remoto.paginasAsistencias > antes && !sync.estado.value.sincronizando }
        assertTrue(remoto.paginasAsistencias > antes)
    }

    @Test
    fun `la consulta distingue cargando, fallo inicial y contenido desactualizado`() = runTest {
        val datos = flowOf("x")
        assertEquals(Consulta.Cargando, datos.comoConsulta(flowOf(EstadoSync(sincronizando = true)), flowOf(null)).first())

        val fallo = datos.comoConsulta(flowOf(EstadoSync(error = ErrorApi.SinConexion)), flowOf(null)).first()
        assertEquals(Consulta.Fallo(ErrorApi.SinConexion), fallo)

        val viejo = assertIs<Consulta.Contenido<String>>(datos.comoConsulta(flowOf(EstadoSync(error = ErrorApi.SinConexion)), flowOf(1_000L)).first())
        assertEquals("x", viejo.datos)
        assertEquals(ErrorApi.SinConexion, viejo.aviso.error)
        assertFalse(viejo.aviso.sincronizando)
    }

    /** Espera en tiempo real (el sincronizador usa hilos reales; el reloj virtual de runTest no sirve aquí). */
    private suspend fun esperar(condicion: () -> Boolean) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        repeat(300) {
            if (condicion()) return@withContext
            kotlinx.coroutines.delay(10)
        }
        error("La condición no se cumplió a tiempo")
    }

    @Test
    fun `el promedio se muestra con un decimal y coma`() {
        assertEquals("17,5", 17.5.unDecimal())
        assertEquals("10,0", 10.0.unDecimal())
        assertEquals("8,7", 8.66.unDecimal())
    }
}
