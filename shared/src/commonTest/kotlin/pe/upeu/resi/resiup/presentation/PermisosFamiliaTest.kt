package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.upeu.resi.resiup.data.remote.dto.SolicitudDto
import pe.upeu.resi.resiup.data.remote.dto.aDominio
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.Hijo
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.OrigenSolicitud
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.FamiliaRepository
import pe.upeu.resi.resiup.presentation.familia.FamiliaInicioViewModel
import pe.upeu.resi.resiup.presentation.familia.FamiliaNuevoViewModel
import pe.upeu.resi.resiup.presentation.residente.SolicitudFormViewModel
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ahora
import pe.upeu.resi.resiup.presentation.util.conHoraLima

/** Formularios de permiso (corto, largo, pernoctar fuera) y perfil de padres. */
@OptIn(ExperimentalCoroutinesApi::class)
class PermisosFamiliaTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private class PermisoFalso : pe.upeu.resi.resiup.domain.repository.PermisoRepository {
        var recibida: NuevaSolicitud? = null
        override suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso> { recibida = solicitud; return Result.success(solicitud(1)) }
        override suspend fun mias(pagina: Int) = Result.success(Pagina<SolicitudPermiso>(emptyList(), 0, 0, true))
        override suspend fun detalle(id: Long) = Result.success(solicitud(id))
        override suspend fun cancelar(id: Long) = Result.success(solicitud(id))
        override suspend fun justificar(id: Long, motivo: String) = Result.success(Unit)
        override suspend fun listar(estado: EstadoSolicitud?, pagina: Int) = Result.success(Pagina<SolicitudPermiso>(emptyList(), 0, 0, true))
        override suspend fun enlaceApoderado(id: Long) = Result.failure<pe.upeu.resi.resiup.domain.model.EnlaceApoderado>(pe.upeu.resi.resiup.domain.error.ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun resumenDelResidente(id: Long) = Result.failure<pe.upeu.resi.resiup.domain.model.ResumenResidente>(pe.upeu.resi.resiup.domain.error.ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun anular(id: Long, motivo: String) = Result.success(solicitud(id))
    }

    private class FamiliaFalsa(val hijos: List<Hijo>, val pendientes: MutableList<SolicitudPermiso> = mutableListOf()) : FamiliaRepository {
        var recibida: NuevaSolicitud? = null
        val decisiones = mutableListOf<Pair<Long, Boolean>>()
        override suspend fun hijos() = Result.success(hijos)
        override suspend fun solicitudes(estado: EstadoSolicitud?, pagina: Int) = Result.success(Pagina(pendientes.toList(), 0, pendientes.size.toLong(), true))
        override suspend fun detalle(id: Long) = Result.success(solicitud(id))
        override suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso> { recibida = solicitud; return Result.success(solicitud(1, TipoPermiso.Pernocte)) }
        override suspend fun decidir(id: Long, aprobar: Boolean): Result<SolicitudPermiso> {
            decisiones += id to aprobar
            pendientes.removeAll { it.id == id }
            return Result.success(solicitud(id))
        }
        override suspend fun cancelar(id: Long) = Result.success(solicitud(id))
    }

    companion object {
        fun solicitud(id: Long, tipo: TipoPermiso = TipoPermiso.Largo) = SolicitudPermiso(
            id, "A001", "Ana", tipo, "m", "d", ahora(), ahora() + 5.hours, ahora() + 5.hours, EstadoSolicitud.PendienteApoderado, true, null, ahora(),
            emptyList(), emptyList(),
        )
    }

    private val hijo1 = Hijo(1, "A001", "Ana Pérez", "101", "A", false, 1)
    private val hijo2 = Hijo(2, "A002", "Beto Pérez", "103", "B", true, 0)

    @Test
    fun `el residente solo elige corto o largo y el pernoctar queda para su padre`() = runTest {
        val vm = SolicitudFormViewModel(PermisoFalso())
        assertEquals(listOf(TipoPermiso.Corto, TipoPermiso.Largo), vm.tiposPermitidos)
        assertEquals(listOf(TipoPermiso.Corto, TipoPermiso.Largo, TipoPermiso.Pernocte), FamiliaNuevoViewModel(FamiliaFalsa(listOf(hijo1))).tiposPermitidos)
    }

    @Test
    fun `un permiso corto no pasa de dos horas y se sugiere el largo`() = runTest {
        val repo = PermisoFalso()
        val vm = SolicitudFormViewModel(repo)
        vm.destino("La Era"); vm.motivo("Trabajo grupal")
        val salida = ahora() + 1.hours
        vm.salida(salida)
        vm.retorno(salida + 121.minutes)
        vm.enviar()
        assertTrue(vm.estado.value.errores.getValue("retorno").contains("Largo"), "el mensaje sugiere pedir un permiso largo")
        assertNull(repo.recibida)

        vm.retorno(salida + 120.minutes)
        vm.enviar()
        assertEquals(TipoPermiso.Corto, repo.recibida?.tipo, "dos horas exactas sí se permiten")
        assertNull(repo.recibida?.responsableNombre, "un corto no lleva datos de casa")
    }

    @Test
    fun `el mismo horario de tres horas se envia como permiso largo`() = runTest {
        val repo = PermisoFalso()
        val vm = SolicitudFormViewModel(repo)
        vm.tipo(TipoPermiso.Largo); vm.destino("Alameda"); vm.motivo("Grupo pequeño")
        val salida = ahora() + 1.hours
        vm.salida(salida); vm.retorno(salida + 150.minutes)
        vm.enviar()
        assertEquals(TipoPermiso.Largo, repo.recibida?.tipo)
        assertNotNull(vm.estado.value.creada)
    }

    @Test
    fun `en un corto la hora de ingreso se mueve con el dia de salida`() = runTest {
        val vm = SolicitudFormViewModel(PermisoFalso())
        val dia1 = ahora().plus(1.days).conHoraLima(14, 45)
        vm.salida(dia1)
        vm.retorno(dia1.conHoraLima(16, 30))
        val dia2 = ahora().plus(2.days).conHoraLima(9, 0)
        vm.salida(dia2)
        val retorno = vm.estado.value.retorno!!
        assertEquals(dia2.conHoraLima(16, 30), retorno, "el ingreso sigue siendo a las 16:30 pero del nuevo día")
    }

    @Test
    fun `pernoctar exige responsable celular y volver otro dia`() = runTest {
        val familia = FamiliaFalsa(listOf(hijo1))
        val vm = FamiliaNuevoViewModel(familia)
        vm.tipo(TipoPermiso.Pernocte)
        vm.destino("Guadalupe - La Libertad"); vm.motivo("Visita familiar y elecciones")
        val salida = ahora().plus(3.days).conHoraLima(14, 0)
        vm.salida(salida); vm.retorno(salida.conHoraLima(23, 0))
        vm.enviar()
        assertEquals(setOf("responsableNombre", "responsableCelular", "retorno"), vm.estado.value.errores.keys)
        assertNull(familia.recibida)

        vm.responsableNombre("Haydee Bazán Abanto"); vm.responsableCelular("+51 968 110 422")
        vm.retorno(salida.plus(3.days).conHoraLima(10, 30))
        vm.enviar()
        val enviada = familia.recibida!!
        assertEquals(TipoPermiso.Pernocte, enviada.tipo)
        assertEquals(1L, enviada.residenteId, "con un solo hijo se elige solo")
        assertEquals("Haydee Bazán Abanto", enviada.responsableNombre)
        assertEquals("+51 968 110 422", enviada.responsableCelular)
        assertEquals("Nadie", enviada.acompanante, "sin acompañante se registra «Nadie»")
    }

    @Test
    fun `con varios hijos el padre debe elegir para quien es el permiso`() = runTest {
        val familia = FamiliaFalsa(listOf(hijo1, hijo2))
        val vm = FamiliaNuevoViewModel(familia)
        vm.destino("Casa"); vm.motivo("Visita")
        val salida = ahora() + 1.hours
        vm.salida(salida); vm.retorno(salida + 1.hours)
        vm.enviar()
        assertTrue("residenteId" in vm.estado.value.errores)
        assertNull(familia.recibida)
        vm.hijo(2)
        vm.enviar()
        assertEquals(2L, familia.recibida?.residenteId)
    }

    @Test
    fun `el padre acepta o rechaza y la lista se actualiza`() = runTest {
        val familia = FamiliaFalsa(listOf(hijo1), mutableListOf(solicitud(10), solicitud(11)))
        val vm = FamiliaInicioViewModel(familia)
        assertEquals(2, (vm.estado.value.porAceptar as Carga.Exito).datos.size)
        vm.decidir(10, true)
        assertEquals(listOf(10L to true), familia.decisiones)
        assertEquals(listOf(11L), (vm.estado.value.porAceptar as Carga.Exito).datos.map { it.id })
        assertNotNull(vm.estado.value.mensaje)
        vm.decidir(11, false)
        assertEquals(10L to true, familia.decisiones.first())
        assertEquals(11L to false, familia.decisiones.last())
    }

    @Test
    fun `el servidor informa quien envio el permiso y los datos de la casa`() {
        val dto = SolicitudDto(
            1, "A001", "Ana", "PERNOCTE", "Visita", "Guadalupe", "2026-10-02T19:00:00Z", "2026-10-05T15:30:00Z", "2026-10-05T15:30:00Z", "PENDIENTE_PRECEPTOR",
            false, null, "2026-09-25T15:00:00Z", origen = "APODERADO", responsableNombre = "Haydee", responsableCelular = "968110422", acompanante = "Nadie",
        ).aDominio()
        assertEquals(TipoPermiso.Pernocte, dto.tipo)
        assertEquals(OrigenSolicitud.Apoderado, dto.origen)
        assertEquals("Haydee", dto.responsableNombre)
        assertEquals(EstadoSolicitud.PendientePreceptor, dto.estado)
        // Un tipo desconocido o el antiguo «ESPECIAL» no rompe la lista.
        assertEquals(TipoPermiso.Pernocte, SolicitudDto(2, tipo = "ESPECIAL", motivo = "m", destino = "d", salidaSolicitada = "2026-10-02T19:00:00Z",
            retornoSolicitado = "2026-10-03T19:00:00Z", retornoAutorizado = "2026-10-03T19:00:00Z", estado = "APROBADO", creadoEn = "2026-09-25T15:00:00Z").aDominio().tipo)
    }
}
