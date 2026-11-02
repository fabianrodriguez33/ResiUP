package pe.upeu.resi.resiup.presentation

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
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
import pe.upeu.resi.resiup.domain.usecase.IniciarSesionUseCase
import pe.upeu.resi.resiup.domain.usecase.ObtenerSesionGuardadaUseCase
import pe.upeu.resi.resiup.presentation.login.LoginViewModel
import pe.upeu.resi.resiup.presentation.sesion.EstadoSesion
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion

private val SESION = Sesion("t", "ana", "Ana Pérez", Rol.Preceptor, setOf("RESIDENTE_VER"), Long.MAX_VALUE)

private class RepoFalso(
    var guardada: Sesion? = null,
    var respuesta: suspend () -> Result<Sesion> = { Result.success(SESION) },
) : AuthRepository {
    var intentos = 0
    var cerrada = false
    override suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion> { intentos++; return respuesta() }
    override suspend fun sesionGuardada(): Sesion? = guardada
    override suspend fun cerrarSesion() { cerrada = true; guardada = null }
    override suspend fun olvideClave(usuario: String) = Result.success(Unit)
    override suspend fun cambiarClave(actual: String, nueva: String) = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginYSesionTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun armar(repo: RepoFalso): Pair<LoginViewModel, GestorSesion> {
        val gestor = GestorSesion(ObtenerSesionGuardadaUseCase(repo), CerrarSesionUseCase(repo), SesionEventos())
        return LoginViewModel(IniciarSesionUseCase(repo), gestor) to gestor
    }

    @Test
    fun `campos vacios muestran errores por campo y no llaman al servidor`() = runTest {
        val repo = RepoFalso()
        val (vm, _) = armar(repo)
        vm.ingresar()
        assertEquals("Ingresa tu usuario o código", vm.estado.value.usuarioError)
        assertEquals("Ingresa tu contraseña", vm.estado.value.claveError)
        assertEquals(0, repo.intentos)
        assertEquals(false, vm.estado.value.cargando)
    }

    @Test
    fun `login correcto activa la sesion y limpia la contrasena de la pantalla`() = runTest {
        val repo = RepoFalso()
        val (vm, gestor) = armar(repo)
        gestor.restaurar()
        vm.alCambiarUsuario(" ana ")
        vm.alCambiarClave("secreta")
        vm.ingresar()
        val estado = assertIs<EstadoSesion.Activa>(gestor.estado.value)
        assertEquals(Rol.Preceptor, estado.sesion.rol)
        assertEquals("", vm.estado.value.clave)
    }

    @Test
    fun `cada error del servidor tiene un mensaje claro y no deja la pantalla cargando`() = runTest {
        val casos = mapOf(
            ErrorApi.CredencialesInvalidas to "Usuario o contraseña incorrectos",
            ErrorApi.SinConexion to "No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.",
            ErrorApi.TiempoAgotado to "El servidor tardó demasiado en responder. Inténtalo de nuevo.",
            ErrorApi.Servidor to "El servidor tuvo un problema. Inténtalo más tarde.",
        )
        for ((error, mensaje) in casos) {
            val repo = RepoFalso(respuesta = { Result.failure(ErrorApiException(error)) })
            val (vm, gestor) = armar(repo)
            gestor.restaurar()
            vm.alCambiarUsuario("ana"); vm.alCambiarClave("x")
            vm.ingresar()
            assertEquals(mensaje, vm.estado.value.error)
            assertEquals(false, vm.estado.value.cargando)
            assertEquals("", vm.estado.value.clave, "tras un fallo se borra la contraseña escrita")
            assertIs<EstadoSesion.SinSesion>(gestor.estado.value)
        }
    }

    @Test
    fun `un segundo toque mientras se inicia sesion se ignora`() = runTest {
        val espera = CompletableDeferred<Result<Sesion>>()
        val repo = RepoFalso(respuesta = { espera.await() })
        val (vm, _) = armar(repo)
        vm.alCambiarUsuario("ana"); vm.alCambiarClave("x")
        vm.ingresar()
        assertTrue(vm.estado.value.cargando)
        vm.ingresar()
        assertEquals(1, repo.intentos)
        espera.complete(Result.success(SESION))
    }

    @Test
    fun `restaurar usa la sesion guardada o muestra el login`() = runTest {
        val con = GestorSesion(ObtenerSesionGuardadaUseCase(RepoFalso(guardada = SESION)), CerrarSesionUseCase(RepoFalso()), SesionEventos())
        assertEquals(EstadoSesion.Arrancando, con.estado.value)
        con.restaurar()
        assertIs<EstadoSesion.Activa>(con.estado.value)

        val sin = GestorSesion(ObtenerSesionGuardadaUseCase(RepoFalso()), CerrarSesionUseCase(RepoFalso()), SesionEventos())
        sin.restaurar()
        assertEquals(EstadoSesion.SinSesion(), sin.estado.value)
    }

    @Test
    fun `una sesion expirada borra los datos locales y vuelve al login con aviso`() = runTest {
        val repo = RepoFalso(guardada = SESION)
        val (_, gestor) = armar(repo)
        gestor.restaurar()
        gestor.sesionExpirada()
        val estado = assertIs<EstadoSesion.SinSesion>(gestor.estado.value)
        assertNotNull(estado.aviso)
        assertTrue(repo.cerrada)
        assertNull(repo.guardada)
    }

    @Test
    fun `cerrar sesion elimina la sesion local`() = runTest {
        val repo = RepoFalso(guardada = SESION)
        val (_, gestor) = armar(repo)
        gestor.restaurar()
        gestor.cerrarSesion()
        assertEquals(EstadoSesion.SinSesion(), gestor.estado.value)
        assertTrue(repo.cerrada)
    }
}
