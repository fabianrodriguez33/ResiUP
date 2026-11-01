package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.Pagina
import pe.upeu.resi.resiup.domain.model.Perfil
import pe.upeu.resi.resiup.domain.model.PerfilEditable
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.residente.EditarPerfilViewModel
import pe.upeu.resi.resiup.presentation.util.Sincronizador
import pe.upeu.resi.resiup.presentation.util.calcularEstadisticas

/** Racha y estadísticas de cultos, y la edición del perfil del residente. */
@OptIn(ExperimentalCoroutinesApi::class)
class EstadisticasPerfilTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun culto(fecha: String, tipo: TipoCulto = TipoCulto.Matutino) = AsistenciaCulto(1, fecha, tipo, Instant.fromEpochMilliseconds(0), false)

    // Octubre de 2026: el 1 es jueves; el 30 es viernes; el 31 sábado y el 1 de noviembre domingo.
    private val octubre = (1..30).map { LocalDate(2026, 10, it) }.filter { it.dayOfWeek.ordinal < 5 }

    @Test
    fun `la racha cuenta dias habiles seguidos y el fin de semana no la corta`() {
        val sinFalta = octubre.map { culto(it.toString()) }
        // Domingo 1 de noviembre: la racha llega hasta el viernes 30 sin cortarse por el fin de semana.
        assertEquals(22, calcularEstadisticas(sinFalta, LocalDate(2026, 11, 1)).racha)
        // Sábado 31: igual.
        assertEquals(22, calcularEstadisticas(sinFalta, LocalDate(2026, 10, 31)).racha)
    }

    @Test
    fun `una falta corta la racha y se cuenta desde la ultima asistencia`() {
        val sin8y9 = octubre.filter { it.day != 8 && it.day != 9 }.map { culto(it.toString()) }
        // Hay asistencia del 12 al 30: 15 días hábiles seguidos.
        assertEquals(15, calcularEstadisticas(sin8y9, LocalDate(2026, 11, 1)).racha)
        // Si falló el viernes 30, la racha de ese día es 0.
        val sin30 = octubre.filter { it.day != 30 }.map { culto(it.toString()) }
        assertEquals(0, calcularEstadisticas(sin30, LocalDate(2026, 11, 1)).racha)
    }

    @Test
    fun `hoy sin registrar todavia no corta la racha pero hoy registrado la suma`() {
        val hastaAyer = listOf(27, 28, 29).map { culto(LocalDate(2026, 10, it).toString()) }
        val jueves29Hoy = LocalDate(2026, 10, 30) // viernes, todavía sin culto
        assertEquals(3, calcularEstadisticas(hastaAyer, jueves29Hoy).racha)
        assertEquals(4, calcularEstadisticas(hastaAyer + culto("2026-10-30"), jueves29Hoy).racha)
        // El lunes sigue contando el viernes anterior.
        assertEquals(1, calcularEstadisticas(listOf(culto("2026-10-30")), LocalDate(2026, 11, 2)).racha)
    }

    @Test
    fun `dos cultos el mismo dia cuentan como un dia y sin datos la racha es cero`() {
        val doble = listOf(culto("2026-10-29"), culto("2026-10-29", TipoCulto.Vespertino), culto("2026-10-30"))
        assertEquals(2, calcularEstadisticas(doble, LocalDate(2026, 10, 30)).racha)
        assertEquals(0, calcularEstadisticas(emptyList(), LocalDate(2026, 10, 30)).racha)
    }

    @Test
    fun `los dias del mes se comparan con los dias habiles transcurridos`() {
        val e = calcularEstadisticas(octubre.take(10).map { culto(it.toString()) }, LocalDate(2026, 10, 30))
        assertEquals(10, e.diasAsistidosMes)
        assertEquals(22, e.diasHabilesMes, "octubre de 2026 tiene 22 días hábiles")
        // A mitad de mes solo cuentan los días hábiles ya transcurridos.
        assertEquals(10, calcularEstadisticas(emptyList(), LocalDate(2026, 10, 14)).diasHabilesMes)
        // Las asistencias de otro mes no suman.
        assertEquals(0, calcularEstadisticas(listOf(culto("2026-09-30")), LocalDate(2026, 10, 14)).diasAsistidosMes)
    }

    @Test
    fun `si el mes recien empieza sin dias habiles se muestra el mes pasado`() {
        val sinFalta = octubre.map { culto(it.toString()) }
        // Domingo 1 de noviembre: todavía no hay ningún día hábil de noviembre.
        val e = calcularEstadisticas(sinFalta, LocalDate(2026, 11, 1))
        assertTrue(e.delMesAnterior)
        assertEquals(22, e.diasHabilesMes)
        assertEquals(22, e.diasAsistidosMes)
        // El lunes 2 ya hay un día hábil: se vuelve al mes actual.
        val lunes = calcularEstadisticas(sinFalta, LocalDate(2026, 11, 2))
        assertTrue(!lunes.delMesAnterior)
        assertEquals(1, lunes.diasHabilesMes)
    }

    // ------------------------------------------------------------ edición del perfil

    private class ResidenteFalso(var respuesta: Result<Perfil>) : ResidenteRepository {
        var recibido: PerfilEditable? = null
        override suspend fun perfil() = respuesta
        override suspend fun qr() = Result.failure<pe.upeu.resi.resiup.domain.model.QrPersonal>(ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun asistencias(pagina: Int) = Result.success(Pagina<AsistenciaCulto>(emptyList(), 0, 0, true))
        override suspend fun limpieza(pagina: Int) = Result.failure<Pagina<pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza>>(ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun resumenLimpieza() = Result.failure<pe.upeu.resi.resiup.domain.model.ResumenLimpieza>(ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun movimientos(pagina: Int) = Result.failure<Pagina<pe.upeu.resi.resiup.domain.model.MovimientoRegistrado>>(ErrorApiException(ErrorApi.NoEncontrado))
        override suspend fun listarResidentes() = Result.success(emptyList<pe.upeu.resi.resiup.domain.model.ResidenteResumen>())
        override suspend fun actualizarPerfil(cambios: PerfilEditable): Result<Perfil> { recibido = cambios; return respuesta }
    }

    private val perfil = Perfil(1, "202410411", "Julián Andrés Rojas Paredes", "968110411", null, false, "j@upeu.edu.pe", "Ingeniería de Sistemas", 6)

    private fun sincFalso() = Sincronizador(object : pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository {
        override fun perfil() = kotlinx.coroutines.flow.flowOf<Perfil?>(null)
        override fun asistencias() = kotlinx.coroutines.flow.flowOf(emptyList<AsistenciaCulto>())
        override fun evaluaciones() = kotlinx.coroutines.flow.flowOf(emptyList<pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza>())
        override fun resumenLimpieza() = kotlinx.coroutines.flow.flowOf<pe.upeu.resi.resiup.domain.model.ResumenLimpieza?>(null)
        override fun solicitudes() = kotlinx.coroutines.flow.flowOf(emptyList<pe.upeu.resi.resiup.domain.model.SolicitudPermiso>())
        override fun ultimaSincronizacion() = kotlinx.coroutines.flow.flowOf<Long?>(null)
        override suspend fun sincronizar() = Result.success(Unit)
        override fun borrarTodo() {}
        override fun asegurarPropietario(usuario: String) {}
    }, { 0L })

    @Test
    fun `el formulario parte de los datos actuales y valida codigo correo telefono y ciclo`() = runTest {
        val repo = ResidenteFalso(Result.success(perfil))
        val vm = EditarPerfilViewModel(repo, sincFalso())
        vm.iniciar(perfil)
        assertEquals("202410411", vm.estado.value.codigo)
        assertEquals("6", vm.estado.value.ciclo)

        vm.codigo("20241035"); vm.email("no-es-correo"); vm.telefono("123"); vm.ciclo("15")
        vm.guardar()
        assertEquals(setOf("codigo", "email", "telefono", "ciclo"), vm.estado.value.errores.keys)
        assertNull(repo.recibido, "no se llama al servidor con datos inválidos")
    }

    @Test
    fun `el codigo solo admite digitos y nueve como maximo`() = runTest {
        val vm = EditarPerfilViewModel(ResidenteFalso(Result.success(perfil)), sincFalso())
        vm.iniciar(perfil)
        vm.codigo("2024-1035-4 abc 99")
        assertEquals("202410354", vm.estado.value.codigo)
        vm.ciclo("1a2b3")
        assertEquals("12", vm.estado.value.ciclo)
    }

    @Test
    fun `guardar envia los datos recortados y avisa cuando termina`() = runTest {
        val repo = ResidenteFalso(Result.success(perfil.copy(ciclo = 7)))
        val vm = EditarPerfilViewModel(repo, sincFalso())
        vm.iniciar(perfil)
        vm.email("  julio@upeu.edu.pe "); vm.carrera("Ingeniería de Sistemas"); vm.ciclo("7"); vm.telefono("968 110 422"); vm.codigo("202410354")
        vm.guardar()
        assertEquals(PerfilEditable("  julio@upeu.edu.pe ", "968 110 422", "202410354", "Ingeniería de Sistemas", 7), repo.recibido)
        assertTrue(vm.estado.value.guardado)
    }

    @Test
    fun `un codigo repetido o la falta de conexion se informan sin perder lo escrito`() = runTest {
        val repo = ResidenteFalso(Result.failure(ErrorApiException(ErrorApi.Conflicto("Ya existe un residente con el código 202410354"))))
        val vm = EditarPerfilViewModel(repo, sincFalso())
        vm.iniciar(perfil)
        vm.codigo("202410354")
        vm.guardar()
        assertEquals(ErrorApi.Conflicto("Ya existe un residente con el código 202410354"), vm.estado.value.error)
        assertEquals("202410354", vm.estado.value.codigo)
        assertTrue(!vm.estado.value.guardado && !vm.estado.value.guardando)

        repo.respuesta = Result.failure(ErrorApiException(ErrorApi.SinConexion))
        vm.guardar()
        assertEquals(ErrorApi.SinConexion, vm.estado.value.error)
        assertNotNull(vm.estado.value.codigo)
    }
}
