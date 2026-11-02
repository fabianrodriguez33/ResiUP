package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.upeu.resi.resiup.data.remote.SesionEventos
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.model.Credenciales
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.domain.repository.AuthRepository
import pe.upeu.resi.resiup.domain.usecase.CerrarSesionUseCase
import pe.upeu.resi.resiup.domain.usecase.ObtenerSesionGuardadaUseCase
import pe.upeu.resi.resiup.presentation.clave.CambiarClaveViewModel
import pe.upeu.resi.resiup.presentation.clave.OlvideClaveViewModel
import pe.upeu.resi.resiup.presentation.clave.validarClaveNueva
import pe.upeu.resi.resiup.presentation.sesion.EstadoSesion
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion

private val PROVISIONAL = Sesion("t", "ana", "Ana Pérez", Rol.Garita, emptySet(), Long.MAX_VALUE, debeCambiarClave = true)

private class AuthFalso(var cambio: Result<Unit> = Result.success(Unit)) : AuthRepository {
    var pedidoOlvide: String? = null
    var cambioRecibido: Pair<String, String>? = null
    override suspend fun iniciarSesion(credenciales: Credenciales) = Result.success(PROVISIONAL)
    override suspend fun sesionGuardada(): Sesion? = PROVISIONAL
    override suspend fun cerrarSesion() = Unit
    override suspend fun olvideClave(usuario: String): Result<Unit> { pedidoOlvide = usuario; return Result.success(Unit) }
    override suspend fun cambiarClave(actual: String, nueva: String): Result<Unit> { cambioRecibido = actual to nueva; return cambio }
}

private fun gestorDe(repo: AuthRepository) = GestorSesion(ObtenerSesionGuardadaUseCase(repo), CerrarSesionUseCase(repo), SesionEventos())

@OptIn(ExperimentalCoroutinesApi::class)
class ClaveTest {
    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    @Test
    fun `la clave nueva debe tener 8 caracteres, ser distinta y repetirse igual`() {
        assertEquals(emptyMap(), validarClaveNueva("Vieja-123", "Nueva-1234", "Nueva-1234"))
        assertEquals(setOf("nueva"), validarClaveNueva("Vieja-123", "corta", "corta").keys)
        assertEquals(setOf("nueva"), validarClaveNueva("Vieja-1234", "Vieja-1234", "Vieja-1234").keys)
        assertEquals(setOf("repetir"), validarClaveNueva("Vieja-123", "Nueva-1234", "Nueva-12345").keys)
        assertEquals(setOf("actual"), validarClaveNueva("", "Nueva-1234", "Nueva-1234").keys)
    }

    @Test
    fun `al cambiar la clave provisional la sesion deja de exigirlo`() = runTest {
        val repo = AuthFalso()
        val gestor = gestorDe(repo)
        gestor.restaurar()
        assertTrue((gestor.estado.value as EstadoSesion.Activa).sesion.debeCambiarClave)

        val vm = CambiarClaveViewModel(repo, gestor)
        vm.actual("Provisional1"); vm.nueva("Nueva-1234"); vm.repetir("Nueva-1234")
        vm.guardar()
        assertEquals("Provisional1" to "Nueva-1234", repo.cambioRecibido)
        assertTrue(vm.estado.value.listo)
        assertFalse((gestor.estado.value as EstadoSesion.Activa).sesion.debeCambiarClave)
    }

    @Test
    fun `una contraseña actual incorrecta se muestra en su campo y no cierra el formulario`() = runTest {
        val repo = AuthFalso(Result.failure(ErrorApiException(ErrorApi.Validacion(mapOf("claveActual" to "No es la contraseña actual")))))
        val gestor = gestorDe(repo)
        gestor.restaurar()
        val vm = CambiarClaveViewModel(repo, gestor)
        vm.actual("mala"); vm.nueva("Nueva-1234"); vm.repetir("Nueva-1234")
        vm.guardar()
        assertEquals("No es la contraseña actual", vm.estado.value.errores["actual"])
        assertFalse(vm.estado.value.listo)
        assertTrue((gestor.estado.value as EstadoSesion.Activa).sesion.debeCambiarClave)
    }

    @Test
    fun `datos invalidos no llaman al servidor`() = runTest {
        val repo = AuthFalso()
        val vm = CambiarClaveViewModel(repo, gestorDe(repo))
        vm.actual("x"); vm.nueva("corta"); vm.repetir("otra")
        vm.guardar()
        assertNull(repo.cambioRecibido)
        assertEquals(setOf("nueva", "repetir"), vm.estado.value.errores.keys)
    }

    @Test
    fun `olvide mi contraseña envia el usuario y confirma sin revelar nada`() = runTest {
        val repo = AuthFalso()
        val vm = OlvideClaveViewModel(repo)
        vm.usuario("202410411")
        vm.enviar()
        assertEquals("202410411", repo.pedidoOlvide)
        assertTrue(vm.estado.value.enviado)
    }
}
