package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import pe.upeu.resi.resiup.presentation.util.conHoraLima
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.upeu.resi.resiup.data.remote.dto.AmpliacionDto
import pe.upeu.resi.resiup.data.remote.dto.EvaluacionDto
import pe.upeu.resi.resiup.data.remote.dto.ResidenteGaritaDto
import pe.upeu.resi.resiup.data.remote.dto.SolicitudDto
import pe.upeu.resi.resiup.data.remote.dto.VerificacionDto
import pe.upeu.resi.resiup.data.remote.dto.aDominio
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.AsistenciaRegistrada
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EstadoRevision
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza
import pe.upeu.resi.resiup.domain.model.MovimientoRegistrado
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import pe.upeu.resi.resiup.domain.model.NuevaEvaluacionHabitacion
import pe.upeu.resi.resiup.domain.model.ResidenteLimpieza
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.ResidenteResumen
import pe.upeu.resi.resiup.domain.model.ResultadoGarita
import pe.upeu.resi.resiup.domain.model.SesionCulto
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.model.VerificacionGarita
import pe.upeu.resi.resiup.domain.repository.CultoRepository
import pe.upeu.resi.resiup.domain.repository.GaritaRepository
import pe.upeu.resi.resiup.domain.repository.LimpiezaRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.personal.AsistenciaCultoViewModel
import pe.upeu.resi.resiup.presentation.personal.GaritaEscanerViewModel
import pe.upeu.resi.resiup.presentation.personal.GaritaEstado
import pe.upeu.resi.resiup.presentation.personal.LimpiezaPreceptorViewModel
import pe.upeu.resi.resiup.presentation.residente.SolicitudFormViewModel
import pe.upeu.resi.resiup.presentation.util.ListaViewModel
import pe.upeu.resi.resiup.presentation.util.ahora
import pe.upeu.resi.resiup.presentation.util.formatoLima
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.aCarga
import kotlin.time.Duration.Companion.hours

private fun fallo(e: ErrorApi) = Result.failure<Nothing>(ErrorApiException(e))

private val VERIFICACION_OK = VerificacionGarita(
    ResultadoGarita.AutorizadoSalida, "Permiso vigente", "Ana Pérez", "A001", "D-12", TipoMovimiento.Salida, null, null,
)

private val MOVIMIENTO = MovimientoRegistrado(1, "A001", "Ana Pérez", TipoMovimiento.Salida, Instant.fromEpochMilliseconds(0), null, false)

@OptIn(ExperimentalCoroutinesApi::class)
class ModulosTest {

    @BeforeTest
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        pe.upeu.resi.resiup.presentation.util.HorarioEnvio.reloj = { kotlin.time.Instant.parse("2026-11-02T15:00:00Z") } // lunes 10:00 en Lima
    }

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    // ------------------------------------------------------------ lista paginada

    @Test
    fun `la lista agrega paginas, se detiene en la ultima y permite reintentar`() = runTest {
        var fallar = false
        val llamadas = mutableListOf<Int>()
        val vm = ListaViewModel<Int> { pagina ->
            llamadas += pagina
            if (fallar) fallo(ErrorApi.SinConexion) else Result.success(Pagina(listOf(pagina * 10, pagina * 10 + 1), pagina, 4, ultima = pagina == 1))
        }
        assertEquals(listOf(0, 1), vm.estado.value.items)
        assertFalse(vm.estado.value.ultima)

        fallar = true
        vm.cargarMas()
        assertEquals(ErrorApi.SinConexion, vm.estado.value.error)
        assertEquals(listOf(0, 1), vm.estado.value.items, "un fallo no borra lo ya cargado")

        fallar = false
        vm.cargarMas()
        assertEquals(listOf(0, 1, 10, 11), vm.estado.value.items)
        assertTrue(vm.estado.value.ultima)

        vm.cargarMas()
        assertEquals(listOf(0, 1, 1), llamadas, "no se pide más allá de la última página")
    }

    @Test
    fun `una lista que falla al inicio muestra el error y refrescar la recupera`() = runTest {
        var fallar = true
        val vm = ListaViewModel<String> { if (fallar) fallo(ErrorApi.Servidor) else Result.success(Pagina(listOf("a"), 0, 1, true)) }
        assertEquals(ErrorApi.Servidor, vm.estado.value.error)
        assertTrue(vm.estado.value.items.isEmpty())
        fallar = false
        vm.refrescar()
        assertEquals(listOf("a"), vm.estado.value.items)
        assertNull(vm.estado.value.error)
    }

    // ------------------------------------------------------------ solicitud de permiso

    private class PermisoFalso(var respuesta: Result<SolicitudPermiso>) : PermisoRepository {
        var recibida: NuevaSolicitud? = null
        override suspend fun crear(solicitud: NuevaSolicitud): Result<SolicitudPermiso> { recibida = solicitud; return respuesta }
        override suspend fun mias(pagina: Int) = Result.success(Pagina<SolicitudPermiso>(emptyList(), 0, 0, true))
        override suspend fun detalle(id: Long) = respuesta
        override suspend fun cancelar(id: Long) = respuesta
        override suspend fun justificar(id: Long, motivo: String) = Result.success(Unit)
        override suspend fun listar(estado: EstadoSolicitud?, pagina: Int) = Result.success(Pagina<SolicitudPermiso>(emptyList(), 0, 0, true))
        override suspend fun enlaceApoderado(id: Long) = fallo(ErrorApi.NoEncontrado)
        override suspend fun resumenDelResidente(id: Long) = fallo(ErrorApi.NoEncontrado)
        override suspend fun anular(id: Long, motivo: String) = respuesta
    }

    private fun solicitud(id: Long = 1) = SolicitudPermiso(
        id, "A001", "Ana", TipoPermiso.Corto, "m", "d", ahora(), ahora() + 2.hours, ahora() + 2.hours, EstadoSolicitud.PendienteApoderado, true, null, ahora(),
        emptyList(), emptyList(),
    )

    @Test
    fun `el formulario valida en el cliente antes de llamar al servidor`() = runTest {
        val repo = PermisoFalso(Result.success(solicitud()))
        val vm = SolicitudFormViewModel(repo)
        vm.enviar()
        assertEquals(setOf("motivo", "destino", "salida", "retorno"), vm.estado.value.errores.keys)
        assertNull(repo.recibida)

        vm.motivo("Compras"); vm.destino("Centro")
        vm.salida(ahora() + 3.hours); vm.retorno(ahora() + 2.hours)
        vm.enviar()
        assertEquals(setOf("retorno"), vm.estado.value.errores.keys)

        vm.salida(ahora() - 2.hours)
        vm.retorno(ahora() + 1.hours)
        vm.enviar()
        assertTrue("salida" in vm.estado.value.errores)
        assertNull(repo.recibida)
    }

    @Test
    fun `fuera del horario de envio la solicitud no se envia ni se encola`() = runTest {
        val repo = PermisoFalso(Result.success(solicitud(9)))
        val vm = SolicitudFormViewModel(repo)
        vm.tipo(TipoPermiso.Corto); vm.motivo("x"); vm.destino("y"); vm.salida(ahora() + 3.hours); vm.retorno(ahora() + 4.hours)
        pe.upeu.resi.resiup.presentation.util.HorarioEnvio.reloj = { kotlin.time.Instant.parse("2026-11-06T20:00:00Z") } // viernes 15:00 en Lima
        vm.enviar()
        assertNull(vm.estado.value.creada)
        assertIs<ErrorApi.Conflicto>(vm.estado.value.error)
        assertNull(repo.recibida)
    }

    @Test
    fun `una solicitud valida se envia y las reglas del servidor se muestran por campo`() = runTest {
        val repo = PermisoFalso(Result.success(solicitud(7)))
        val vm = SolicitudFormViewModel(repo)
        vm.tipo(TipoPermiso.Largo); vm.motivo("  Visita  "); vm.destino("Casa")
        // Mañana 08:00 a 13:00 en Lima: el mismo día sin importar la hora en que corra la prueba.
        val manana8 = (ahora() + 24.hours).conHoraLima(8, 0)
        vm.salida(manana8); vm.retorno(manana8 + 5.hours)
        vm.enviar()
        assertEquals(7, vm.estado.value.creada?.id)
        assertEquals("Visita", repo.recibida?.motivo, "se recorta el texto")
        assertEquals(TipoPermiso.Largo, repo.recibida?.tipo)

        val rechazada = PermisoFalso(fallo(ErrorApi.Validacion(mapOf("retorno" to "Excede las 8 horas permitidas"))))
        val vm2 = SolicitudFormViewModel(rechazada)
        vm2.tipo(TipoPermiso.Largo); vm2.motivo("x"); vm2.destino("y"); vm2.salida(manana8); vm2.retorno(manana8 + 5.hours)
        vm2.enviar()
        assertEquals("Excede las 8 horas permitidas", vm2.estado.value.errores["retorno"])
        // Cruzar la medianoche con un largo se detecta en el teléfono.
        val vmCruza = SolicitudFormViewModel(PermisoFalso(Result.success(solicitud(8))))
        vmCruza.tipo(TipoPermiso.Largo); vmCruza.motivo("x"); vmCruza.destino("y"); vmCruza.salida(manana8); vmCruza.retorno(manana8 + 17.hours)
        vmCruza.enviar()
        assertNull(vmCruza.estado.value.creada)
        assertTrue(vmCruza.estado.value.errores["retorno"]!!.contains("mismo día"))
        assertNull(vm2.estado.value.creada)
        assertFalse(vm2.estado.value.enviando)

        val conflicto = PermisoFalso(fallo(ErrorApi.Conflicto("Ya tienes un permiso en ese horario")))
        val vm3 = SolicitudFormViewModel(conflicto)
        vm3.motivo("x"); vm3.destino("y"); vm3.salida(ahora() + 1.hours); vm3.retorno(ahora() + 2.hours)
        vm3.enviar()
        assertEquals(ErrorApi.Conflicto("Ya tienes un permiso en ese horario"), vm3.estado.value.error)
    }

    // ------------------------------------------------------------ garita

    private class GaritaFalsa(var verificacion: Result<VerificacionGarita>, var registro: Result<MovimientoRegistrado>) : GaritaRepository {
        var verificaciones = 0
        override suspend fun verificar(qr: String): Result<VerificacionGarita> { verificaciones++; return verificacion }
        override suspend fun registrar(qr: String, tipo: TipoMovimiento) = registro
        override suspend fun historial(pagina: Int) = Result.success(Pagina<MovimientoRegistrado>(emptyList(), 0, 0, true))
    }

    @Test
    fun `garita muestra un resultado a la vez y registra solo tras la verificacion del servidor`() = runTest {
        val repo = GaritaFalsa(Result.success(VERIFICACION_OK), Result.success(MOVIMIENTO))
        val vm = GaritaEscanerViewModel(repo)
        vm.codigoLeido("qr-1")
        assertIs<GaritaEstado.Resultado>(vm.estado.value)
        vm.codigoLeido("qr-2")
        assertEquals(1, repo.verificaciones, "mientras hay un resultado en pantalla se ignoran otros códigos")

        vm.registrar(TipoMovimiento.Salida)
        assertIs<GaritaEstado.Registrado>(vm.estado.value)
        vm.siguiente()
        assertEquals(GaritaEstado.Esperando, vm.estado.value)
    }

    @Test
    fun `si el servidor rechaza el registro, el resultado queda y se informa el motivo`() = runTest {
        val repo = GaritaFalsa(Result.success(VERIFICACION_OK), fallo(ErrorApi.Conflicto("El residente ya registró su salida")))
        val vm = GaritaEscanerViewModel(repo)
        vm.codigoLeido("qr-1")
        vm.registrar(TipoMovimiento.Salida)
        val e = assertIs<GaritaEstado.Resultado>(vm.estado.value)
        assertEquals(ErrorApi.Conflicto("El residente ya registró su salida"), e.error)
        assertFalse(e.registrando)
    }

    @Test
    fun `sin conexion garita no autoriza nada`() = runTest {
        val repo = GaritaFalsa(fallo(ErrorApi.SinConexion), Result.success(MOVIMIENTO))
        val vm = GaritaEscanerViewModel(repo)
        vm.codigoLeido("qr-1")
        val e = assertIs<GaritaEstado.Fallo>(vm.estado.value)
        assertEquals(ErrorApi.SinConexion, e.error)
        vm.registrar(TipoMovimiento.Salida)
        assertIs<GaritaEstado.Fallo>(vm.estado.value, "no existe forma de registrar sin una verificación del servidor")
    }

    // ------------------------------------------------------------ asistencia a cultos

    private class CultoFalso : CultoRepository {
        var sesiones = listOf<SesionCulto>()
        var registros = 0
        var respuesta: Result<AsistenciaRegistrada> = Result.success(AsistenciaRegistrada("A001", "Ana", false))
        override suspend fun abrirSesion(tipo: TipoCulto) = Result.success(SesionCulto(5, "2026-11-02", tipo, false, 0, ahora()))
        override suspend fun sesionesDeHoy() = Result.success(sesiones)
        override suspend fun registrarAsistencia(sesionId: Long, qr: String): Result<AsistenciaRegistrada> { registros++; return respuesta }
        override suspend fun cerrarSesion(sesionId: Long) = Result.success(SesionCulto(sesionId, "2026-11-02", TipoCulto.Matutino, true, 0, ahora()))
    }

    @Test
    fun `la asistencia ignora el mismo codigo repetido por la camara y cuenta los registros`() = runTest {
        val repo = CultoFalso()
        val vm = AsistenciaCultoViewModel(repo)
        assertNull(vm.estado.value.sesion)
        vm.abrir(TipoCulto.Matutino)
        assertNotNull(vm.estado.value.sesion)

        vm.codigoLeido("qr-A"); vm.codigoLeido("qr-A"); vm.codigoLeido("qr-A")
        assertEquals(1, repo.registros)
        assertEquals(1, vm.estado.value.presentes)

        vm.codigoLeido("qr-B")
        assertEquals(2, repo.registros)
        assertEquals(2, vm.estado.value.presentes)
    }

    @Test
    fun `un registro rechazado se muestra con el motivo del servidor y no suma presentes`() = runTest {
        val repo = CultoFalso().apply { respuesta = fallo(ErrorApi.Conflicto("El residente ya registró su asistencia en este culto")) }
        val vm = AsistenciaCultoViewModel(repo)
        vm.abrir(TipoCulto.Vespertino)
        vm.codigoLeido("qr-A")
        val r = vm.estado.value.registros.first()
        assertFalse(r.exito)
        assertEquals("El residente ya registró su asistencia en este culto", r.detalle)
        assertEquals(0, vm.estado.value.presentes)
    }

    @Test
    fun `se retoma la sesion abierta de hoy y se ignoran las cerradas`() = runTest {
        val repo = CultoFalso().apply {
            sesiones = listOf(SesionCulto(1, "2026-11-02", TipoCulto.Matutino, true, 9, ahora()), SesionCulto(2, "2026-11-02", TipoCulto.Vespertino, false, 3, ahora()))
        }
        val vm = AsistenciaCultoViewModel(repo)
        assertEquals(2, vm.estado.value.sesion?.id)
        assertEquals(3, vm.estado.value.presentes)
    }

    // ------------------------------------------------------------ evaluación de limpieza

    private class ResidentesFalso : ResidenteRepository {
        override suspend fun perfil() = fallo(ErrorApi.NoEncontrado)
        override suspend fun qr() = fallo(ErrorApi.NoEncontrado)
        override suspend fun asistencias(pagina: Int) = fallo(ErrorApi.NoEncontrado)
        override suspend fun limpieza(pagina: Int) = fallo(ErrorApi.NoEncontrado)
        override suspend fun resumenLimpieza() = fallo(ErrorApi.NoEncontrado)
        override suspend fun movimientos(pagina: Int) = fallo(ErrorApi.NoEncontrado)
        override suspend fun listarResidentes() = Result.success(listOf(ResidenteResumen(1, "A001", "Ana", "D-12", true)))
        override suspend fun actualizarPerfil(cambios: pe.upeu.resi.resiup.domain.model.PerfilEditable) = fallo(ErrorApi.NoEncontrado)
    }

    private val habitacion101 = HabitacionLimpieza(
        10, "101", 2,
        listOf(ResidenteLimpieza(1, "A001", "Ana Pérez", "A", false), ResidenteLimpieza(2, "A002", "Beto Ríos", "B", false)), 0,
    )

    private class LimpiezaFalsa(
        var lista: Result<List<HabitacionLimpieza>> = Result.success(emptyList()),
        var respuesta: Result<List<EvaluacionLimpieza>>? = null,
    ) : LimpiezaRepository {
        var enviada: NuevaEvaluacionHabitacion? = null
        override suspend fun habitaciones(fecha: String) = lista
        override suspend fun registrarHabitacion(evaluacion: NuevaEvaluacionHabitacion): Result<List<EvaluacionLimpieza>> {
            enviada = evaluacion
            return respuesta ?: Result.success(evaluacion.personas.map { EvaluacionLimpieza(it.residenteId, evaluacion.fecha, 0, 20, emptyMap(), null) })
        }
    }

    @Test
    fun `la lista de limpieza trae habitaciones y no residentes sueltos`() = runTest {
        val vm = LimpiezaPreceptorViewModel(LimpiezaFalsa(lista = Result.success(listOf(habitacion101))))
        val carga = vm.estado.value.habitaciones
        assertTrue(carga is Carga.Exito)
        assertEquals("101", (carga as Carga.Exito).datos.single().numero)
    }

    @Test
    fun `basura y olores son comunes y el resto personal, con topes y total por residente`() = runTest {
        val repo = LimpiezaFalsa()
        val vm = LimpiezaPreceptorViewModel(repo)
        vm.abrir(habitacion101)
        repeat(9) { vm.ajustarComun(CriterioLimpieza.Basura, +1) }
        vm.ajustarComun(CriterioLimpieza.Olores, +1)
        repeat(9) { vm.ajustar(1, CriterioLimpieza.Ropero, +1) }
        vm.ajustar(1, CriterioLimpieza.Piso, -1)
        vm.ajustar(2, CriterioLimpieza.Cama, +1)

        assertEquals(3, vm.estado.value.basura, "tope de basura")
        assertEquals(5 + 3 + 1, vm.estado.value.totalDe(1), "ropero 5 + comunes 3 y 1")
        assertEquals(1 + 3 + 1, vm.estado.value.totalDe(2), "lo personal de uno no cambia al otro")

        vm.guardar()
        val enviada = repo.enviada!!
        assertEquals(10, enviada.habitacionId)
        assertEquals(3, enviada.basura)
        assertEquals(1, enviada.olores)
        assertEquals(setOf(1L, 2L), enviada.personas.map { it.residenteId }.toSet())
        assertEquals(5, enviada.personas.first { it.residenteId == 1L }.ropero)
        assertNull(vm.estado.value.habitacion, "tras guardar se vuelve a la lista")
        assertNotNull(vm.estado.value.mensaje)
    }

    @Test
    fun `los residentes ya evaluados no se vuelven a calificar`() = runTest {
        val repo = LimpiezaFalsa()
        val vm = LimpiezaPreceptorViewModel(repo)
        val parcial = habitacion101.copy(residentes = habitacion101.residentes.mapIndexed { i, r -> if (i == 0) r.copy(evaluado = true) else r }, evaluados = 1)
        vm.abrir(parcial)
        assertEquals(setOf(2L), vm.estado.value.personales.keys)
        vm.guardar()
        assertEquals(listOf(2L), repo.enviada!!.personas.map { it.residenteId })
    }

    @Test
    fun `un conflicto del servidor se muestra y no se pierde lo capturado`() = runTest {
        val repo = LimpiezaFalsa(respuesta = fallo(ErrorApi.Conflicto("Basura y olores ya se registraron ese día con otros valores")))
        val vm = LimpiezaPreceptorViewModel(repo)
        vm.abrir(habitacion101)
        vm.ajustar(1, CriterioLimpieza.Ropero, +1)
        vm.guardar()
        assertEquals(ErrorApi.Conflicto("Basura y olores ya se registraron ese día con otros valores"), vm.estado.value.error)
        assertNotNull(vm.estado.value.habitacion)
        assertEquals(1, vm.estado.value.totalDe(1))
    }

    // ------------------------------------------------------------ mapeadores y formato

    @Test
    fun `los mapeadores traducen estados, criterios y resultados del servidor`() {
        val s = SolicitudDto(
            1, "A001", "Ana", "LARGO", "m", "d", "2026-10-30T21:00:00Z", "2026-10-30T23:00:00Z", "2026-10-31T01:00:00Z", "PENDIENTE_PRECEPTOR",
            true, null, "2026-10-30T20:00:00Z", listOf(AmpliacionDto(2, "x", "2026-10-31T01:00:00Z", "APROBADA")),
        ).aDominio()
        assertEquals(TipoPermiso.Largo, s.tipo)
        assertEquals(EstadoSolicitud.PendientePreceptor, s.estado)
        assertEquals(EstadoRevision.Aprobada, s.ampliaciones.single().estado)
        assertTrue(s.retornoAutorizado > s.retornoSolicitado)

        val e = EvaluacionDto(1, "2026-10-30", 17, 20, mapOf("ROPERO" to 5, "CAMA" to 3, "DESCONOCIDO" to 1)).aDominio()
        assertEquals(mapOf(CriterioLimpieza.Ropero to 5, CriterioLimpieza.Cama to 3), e.puntajes, "un criterio nuevo del servidor no rompe la app")

        val v = VerificacionDto("PERMISO_VENCIDO", "vencido", ResidenteGaritaDto(1, "A001", "Ana", "D-12")).aDominio()
        assertEquals(ResultadoGarita.PermisoVencido, v.resultado)
        assertNull(v.movimientoPermitido)
        assertEquals(ResultadoGarita.Desconocido, VerificacionDto("ALGO_NUEVO").aDominio().resultado)
    }

    @Test
    fun `las fechas se muestran en hora de Lima`() {
        assertEquals("vie 30 oct, 4:00 p. m.", Instant.parse("2026-10-30T21:00:00Z").formatoLima())
        assertEquals("sáb 31 oct, 12:05 a. m.", Instant.parse("2026-10-31T05:05:00Z").formatoLima())
    }

    @Test
    fun `carga exito y fallo se construyen desde un Result`() {
        assertIs<Carga.Exito<Int>>(Result.success(1).aCarga())
        val f = assertIs<Carga.Fallo>(fallo(ErrorApi.SinPermiso).aCarga())
        assertEquals(ErrorApi.SinPermiso, f.error)
        assertIs<Carga.Fallo>(Result.failure<Int>(RuntimeException("x")).aCarga())
    }
}
