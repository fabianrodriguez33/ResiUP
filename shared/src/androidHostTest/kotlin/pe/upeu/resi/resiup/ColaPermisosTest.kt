package pe.upeu.resi.resiup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.data.repository.ColaPermisos
import pe.upeu.resi.resiup.db.ResiDb
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.EnlaceApoderado
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.ResumenResidente
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.PermisoRepository

private val T0 = Instant.fromEpochMilliseconds(1_800_000_000_000)

private fun solicitudCreada() = SolicitudPermiso(
    1, "A001", "Ana", TipoPermiso.Corto, "Compras", "Centro", T0, T0, T0, EstadoSolicitud.PendienteApoderado, true, null, T0, emptyList(), emptyList(),
)

private class PermisosRemotos(var respuesta: Result<SolicitudPermiso>) : PermisoRepository {
    val claves = mutableListOf<String?>()
    override suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso> { claves += solicitud.claveCliente; return respuesta }
    override suspend fun mias(pagina: Int) = Result.success(Pagina(emptyList<SolicitudPermiso>(), 0, 0, true))
    override suspend fun detalle(id: Long) = Result.failure<SolicitudPermiso>(ErrorApiException(ErrorApi.NoEncontrado))
    override suspend fun cancelar(id: Long) = Result.failure<SolicitudPermiso>(ErrorApiException(ErrorApi.NoEncontrado))
    override suspend fun justificar(id: Long, motivo: String) = Result.success(Unit)
    override suspend fun listar(estado: EstadoSolicitud?, pagina: Int) = Result.success(Pagina(emptyList<SolicitudPermiso>(), 0, 0, true))
    override suspend fun enlaceApoderado(id: Long) = Result.failure<EnlaceApoderado>(ErrorApiException(ErrorApi.NoEncontrado))
    override suspend fun resumenDelResidente(id: Long) = Result.failure<ResumenResidente>(ErrorApiException(ErrorApi.NoEncontrado))
    override suspend fun anular(id: Long, motivo: String) = Result.failure<SolicitudPermiso>(ErrorApiException(ErrorApi.NoEncontrado))
}

private fun nueva() = NuevaSolicitud(TipoPermiso.Corto, "Compras", "Centro", T0, T0.plus(Duration.parse("1h")))

private fun armar(remoto: PermisosRemotos, enviados: () -> Unit = {}): Pair<ColaPermisos, DatosLocales> {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    ResiDb.Schema.create(driver)
    val locales = DatosLocales(ResiDb(driver))
    return ColaPermisos(locales, remoto, { 1000L }, enviados) to locales
}

class ColaPermisosTest {
    @Test
    fun `sin red el permiso queda pendiente y al volver la red se envia una sola vez con su clave`() = runTest {
        val remoto = PermisosRemotos(Result.failure(ErrorApiException(ErrorApi.SinConexion)))
        var avisos = 0
        val (cola, locales) = armar(remoto) { avisos++ }
        cola.encolar(nueva().copy(claveCliente = cola.nuevaClave()))
        assertEquals(1, locales.pendientesPorEnviar().size)

        assertEquals(0, cola.enviarPendientes()) // sigue sin red: se conserva
        assertEquals(1, locales.pendientesPorEnviar().size)

        remoto.respuesta = Result.success(solicitudCreada())
        assertEquals(1, cola.enviarPendientes())
        assertEquals(0, locales.pendientesPorEnviar().size)
        assertEquals(1, avisos)
        assertEquals(remoto.claves.first(), remoto.claves.last()) // mismo permiso, misma clave
        assertNotNull(remoto.claves.first())
        assertEquals(0, cola.enviarPendientes()) // nada más que enviar
    }

    @Test
    fun `si el servidor lo rechaza queda marcado con el motivo y no se reintenta`() = runTest {
        val remoto = PermisosRemotos(Result.failure(ErrorApiException(ErrorApi.Conflicto("Ya hay un permiso que se superpone"))))
        val (cola, locales) = armar(remoto)
        cola.encolar(nueva())
        cola.enviarPendientes()
        assertEquals(0, locales.pendientesPorEnviar().size)
        // sigue visible, con el motivo, para que la persona lo descarte
        val visible = locales.pendientes().first().single()
        assertEquals("Ya hay un permiso que se superpone", visible.error)
        cola.enviarPendientes()
        assertEquals(1, remoto.claves.size) // no se reintentó
        cola.descartar(visible.clave)
        assertEquals(0, locales.pendientes().first().size)
    }
}
