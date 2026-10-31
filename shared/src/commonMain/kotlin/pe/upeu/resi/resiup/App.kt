package pe.upeu.resi.resiup

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.upeu.resi.resiup.presentation.catalogo.CatalogoScreen
import pe.upeu.resi.resiup.presentation.login.LoginScreen
import pe.upeu.resi.resiup.presentation.login.LoginViewModel
import pe.upeu.resi.resiup.presentation.navigation.Rol
import pe.upeu.resi.resiup.presentation.shell.ShellPrincipal
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

private sealed interface Pantalla {
    data object Login : Pantalla
    data object Catalogo : Pantalla
    data class Principal(val rol: Rol) : Pantalla
}

/**
 * Raíz de la app. Sin sesión persistida todavía (Fase 3): siempre se inicia en el login.
 * [onCabeceraDeMarca] avisa a la plataforma cuando la parte superior es azul (iconos de barra de estado claros).
 * [modoDesarrollo] habilita la vista previa de roles y el catálogo; nunca debe estar activo en release.
 */
@Composable
fun App(modoDesarrollo: Boolean = false, onCabeceraDeMarca: (Boolean) -> Unit = {}) {
    ResiTheme {
        Box(Modifier.fillMaxSize()) {
            var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Login) }
            val loginVm = viewModel { LoginViewModel() }
            val loginEstado by loginVm.estado.collectAsState()

            LaunchedEffect(pantalla) { onCabeceraDeMarca(pantalla == Pantalla.Login) }

            Crossfade(pantalla) { destino ->
                when (destino) {
                    Pantalla.Login -> LoginScreen(
                        estado = loginEstado,
                        onUsuario = loginVm::alCambiarUsuario,
                        onClave = loginVm::alCambiarClave,
                        onIngresar = loginVm::ingresar,
                        modoDesarrollo = modoDesarrollo,
                        onVistaPrevia = { pantalla = Pantalla.Principal(it) },
                        onCatalogo = { pantalla = Pantalla.Catalogo },
                    )
                    Pantalla.Catalogo -> CatalogoScreen(onVolver = { pantalla = Pantalla.Login })
                    is Pantalla.Principal -> ShellPrincipal(destino.rol, onCerrarSesion = { pantalla = Pantalla.Login })
                }
            }
        }
    }
}
