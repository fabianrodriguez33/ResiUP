package pe.upeu.resi.resiup.presentation.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.components.BarraNavegacionResi
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.navigation.Destinos
import pe.upeu.resi.resiup.presentation.navigation.Rol
import pe.upeu.resi.resiup.presentation.navigation.destinos
import pe.upeu.resi.resiup.presentation.theme.Espacio

@Composable
fun ShellPrincipal(rol: Rol, onCerrarSesion: () -> Unit) {
    val items = remember(rol) { rol.destinos() }
    var seleccionado by rememberSaveable(rol) { mutableStateOf(items.first().id) }
    val actual = items.first { it.id == seleccionado }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 84.dp)) {
            when (actual.id) {
                Destinos.Perfil, Destinos.Mas -> PantallaCuenta(rol, onCerrarSesion)
                else -> PantallaPendiente(actual.etiqueta, rol)
            }
        }
        BarraNavegacionResi(
            items, seleccionado, { seleccionado = it },
            Modifier.align(Alignment.BottomCenter).padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }
}

/** Marcador honesto: el módulo todavía no está conectado al backend. */
@Composable
private fun PantallaPendiente(titulo: String, rol: Rol) {
    Column(Modifier.fillMaxSize().padding(Espacio.l)) {
        TituloPantalla(titulo, subtitulo = rol.etiqueta)
        EstadoVacio(
            titulo = "Módulo en construcción",
            mensaje = "Esta sección se implementa en la Fase 4 y mostrará datos reales del servidor.",
        )
    }
}

@Composable
private fun PantallaCuenta(rol: Rol, onCerrarSesion: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Espacio.l),
    ) {
        TituloPantalla("Cuenta")
        ResiTarjeta {
            ElementoLista(
                titulo = "Sesión de vista previa",
                subtitulo = "Datos del usuario disponibles al conectar con el servidor",
                icono = ResiIconos.Perfil,
                final = { ChipEstado(rol.etiqueta, EstadoVisual.Info) },
            )
        }
        ResiBoton("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
    }
    if (confirmar) {
        DialogoConfirmacion(
            titulo = "Cerrar sesión",
            mensaje = "¿Deseas salir de ResiUP en este dispositivo?",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmar = false; onCerrarSesion() },
            onCancelar = { confirmar = false },
            destructivo = true,
        )
    }
}
