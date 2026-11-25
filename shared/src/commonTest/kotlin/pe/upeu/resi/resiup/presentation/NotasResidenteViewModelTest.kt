package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.ResumenNotas
import pe.upeu.resi.resiup.domain.repository.NotasRepository
import pe.upeu.resi.resiup.presentation.residente.NotasResidenteViewModel
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.hoyLima

class NotasResidenteViewModelTest {

    private class RepoFalso(private val falla: ErrorApi? = null) : NotasRepository {
        val pedidos = mutableListOf<Pair<Int, Int>>()
        override suspend fun resumen(anio: Int, mes: Int): Result<ResumenNotas> {
            pedidos += anio to mes
            return if (falla != null) Result.failure(ErrorApiException(falla))
            else Result.success(
                ResumenNotas(anio, mes, true, 1, "AD", false, true, 1, 1, 0, 100.0, 0, null, null),
            )
        }
    }

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    @Test
    fun `la pantalla de notas pide el mes en curso y el anterior al servidor`() {
        val repo = RepoFalso()
        val vm = NotasResidenteViewModel(repo)
        vm.cargar()
        val estado = vm.notas.value
        assertIs<Carga.Exito<*>>(estado)
        val hoy = hoyLima()
        val actual = hoy.year to (hoy.month.ordinal + 1)
        val anterior = if (actual.second == 1) (actual.first - 1) to 12 else actual.first to (actual.second - 1)
        assertEquals(listOf(actual, anterior), repo.pedidos)
    }

    @Test
    fun `sin conexion la pantalla de notas queda en fallo de conexion y se puede reintentar`() {
        val repo = RepoFalso(ErrorApi.SinConexion)
        val vm = NotasResidenteViewModel(repo)
        vm.cargar()
        val estado = vm.notas.value
        assertIs<Carga.Fallo>(estado)
        assertEquals(ErrorApi.SinConexion, estado.error)
        vm.cargar()
        assertIs<Carga.Fallo>(vm.notas.value)
    }
}
