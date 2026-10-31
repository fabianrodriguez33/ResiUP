package pe.upeu.resi.resiup

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.presentation.catalogo.CatalogoScreen
import pe.upeu.resi.resiup.presentation.login.LoginScreen
import pe.upeu.resi.resiup.presentation.login.LoginViewModel
import pe.upeu.resi.resiup.presentation.sesion.EstadoSesion
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion
import pe.upeu.resi.resiup.presentation.shell.ShellPrincipal
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

/** Pantallas de apoyo que sólo existen en modo desarrollo. */
private sealed interface VistaDev {
    data object Catalogo : VistaDev
    data class PrevisualizarRol(val rol: Rol) : VistaDev
}

/**
 * Raíz de la app. La sesión se restaura del almacén seguro; si no hay una vigente se muestra el login.
 * Requiere que Koin esté iniciado (ver [pe.upeu.resi.resiup.di.initKoin]).
 *
 * [onSesionResuelta] avisa a la plataforma cuando ya se sabe si hay sesión (para soltar el splash nativo).
 * [onCabeceraDeMarca] indica que la parte superior es azul (iconos de barra de estado claros).
 * [modoDesarrollo] habilita la vista previa de roles y el catálogo; nunca debe estar activo en release.
 */
@Composable
fun App(
    modoDesarrollo: Boolean = false,
    onCabeceraDeMarca: (Boolean) -> Unit = {},
    onSesionResuelta: () -> Unit = {},
) {
    ResiTheme {
        val gestor = koinInject<GestorSesion>()
        val estado by gestor.estado.collectAsState()
        val alcance = rememberCoroutineScope()
        var vista by remember { mutableStateOf<VistaDev?>(null) }

        LaunchedEffect(Unit) { gestor.restaurar() }
        LaunchedEffect(Unit) { gestor.eventos.expirada.collect { gestor.sesionExpirada() } }
        LaunchedEffect(estado) {
            if (estado != EstadoSesion.Arrancando) onSesionResuelta()
            onCabeceraDeMarca(estado !is EstadoSesion.Activa && vista == null)
        }
        LaunchedEffect(vista) { onCabeceraDeMarca(estado !is EstadoSesion.Activa && vista == null) }

        Crossfade(estado to vista) { (e, v) ->
            when {
                e is EstadoSesion.Arrancando -> Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background))
                e is EstadoSesion.Activa -> StoreDeSesion(e.sesion.token) {
                    ShellPrincipal(e.sesion.rol, e.sesion) { alcance.launch { gestor.cerrarSesion() } }
                }
                v is VistaDev.Catalogo -> CatalogoScreen(onVolver = { vista = null })
                v is VistaDev.PrevisualizarRol -> ShellPrincipal(v.rol, null) { vista = null }
                else -> {
                    val vm = koinViewModel<LoginViewModel>()
                    val login by vm.estado.collectAsState()
                    LoginScreen(
                        estado = login,
                        onUsuario = vm::alCambiarUsuario,
                        onClave = vm::alCambiarClave,
                        onIngresar = vm::ingresar,
                        aviso = (e as? EstadoSesion.SinSesion)?.aviso,
                        modoDesarrollo = modoDesarrollo,
                        onVistaPrevia = { vista = VistaDev.PrevisualizarRol(it) },
                        onCatalogo = { vista = VistaDev.Catalogo },
                    )
                }
            }
        }
    }
}

/**
 * Los ViewModels de las pantallas con datos viven en un almacén propio de la sesión: al cerrar sesión se descartan, de modo
 * que otro usuario en el mismo dispositivo nunca ve datos en memoria del anterior.
 */
@Composable
private fun StoreDeSesion(clave: String, contenido: @Composable () -> Unit) {
    val dueno = remember(clave) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(dueno) { onDispose { dueno.viewModelStore.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides dueno) { contenido() }
}
