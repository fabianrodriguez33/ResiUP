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
import pe.upeu.resi.resiup.presentation.personal.AsistenciaCultoScreen
import pe.upeu.resi.resiup.presentation.personal.EscanerGaritaScreen
import pe.upeu.resi.resiup.presentation.personal.HistorialGaritaScreen
import pe.upeu.resi.resiup.presentation.personal.InicioPreceptorScreen
import pe.upeu.resi.resiup.presentation.personal.LimpiezaPreceptorScreen
import pe.upeu.resi.resiup.presentation.personal.SolicitudesPreceptorScreen
import pe.upeu.resi.resiup.presentation.residente.AsistenciasScreen
import pe.upeu.resi.resiup.presentation.residente.InicioResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.LimpiezaResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.PerfilResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.PermisosResidenteScreen
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.presentation.navigation.destinos
import pe.upeu.resi.resiup.presentation.theme.Espacio

@Composable
fun ShellPrincipal(rol: Rol, sesion: Sesion?, onCerrarSesion: () -> Unit) {
    val items = remember(rol) { rol.destinos() }
    var seleccionado by rememberSaveable(rol) { mutableStateOf(items.first().id) }
    val actual = items.first { it.id == seleccionado }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = 84.dp)) {
            if (sesion == null) {
                // Vista previa de desarrollo: sin sesión no se consulta el servidor.
                when (actual.id) {
                    Destinos.Perfil, Destinos.Mas -> PantallaCuenta(rol, null, onCerrarSesion)
                    else -> PantallaPendiente(actual.etiqueta, rol)
                }
            } else {
                ContenidoReal(rol, sesion, actual.id, { seleccionado = it }, onCerrarSesion)
            }
        }
        BarraNavegacionResi(
            items, seleccionado, { seleccionado = it },
            Modifier.align(Alignment.BottomCenter).padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }
}

@Composable
private fun ContenidoReal(rol: Rol, sesion: Sesion, destino: String, irA: (String) -> Unit, onCerrarSesion: () -> Unit) {
    when (rol) {
        Rol.Residente -> when (destino) {
            Destinos.Inicio -> InicioResidenteScreen(irA)
            Destinos.Permisos -> PermisosResidenteScreen()
            Destinos.Asistencias -> AsistenciasScreen()
            Destinos.Limpieza -> LimpiezaResidenteScreen()
            else -> PerfilResidenteScreen(sesion, onCerrarSesion)
        }
        Rol.Preceptor -> when (destino) {
            Destinos.Inicio -> InicioPreceptorScreen(sesion.nombreCompleto, irA)
            Destinos.Escanear -> AsistenciaCultoScreen()
            Destinos.Solicitudes -> SolicitudesPreceptorScreen()
            Destinos.Limpieza -> LimpiezaPreceptorScreen()
            else -> PantallaCuenta(rol, sesion, onCerrarSesion)
        }
        Rol.Garita -> when (destino) {
            Destinos.Escanear -> EscanerGaritaScreen()
            Destinos.Historial -> HistorialGaritaScreen()
            else -> PantallaCuenta(rol, sesion, onCerrarSesion)
        }
        Rol.Administrador -> when (destino) {
            Destinos.Inicio -> Column(Modifier.fillMaxSize().padding(Espacio.l)) {
                TituloPantalla("Administración", subtitulo = sesion.nombreCompleto)
                EstadoVacio("Gestión desde el panel web", "La administración de usuarios, residentes y habitaciones se hará desde un panel web que todavía no está aprobado ni construido. La API ya la soporta.")
            }
            else -> PantallaCuenta(rol, sesion, onCerrarSesion)
        }
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
private fun PantallaCuenta(rol: Rol, sesion: Sesion?, onCerrarSesion: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Espacio.l),
    ) {
        TituloPantalla("Cuenta")
        ResiTarjeta {
            ElementoLista(
                titulo = sesion?.nombreCompleto ?: "Vista previa (sin sesión)",
                subtitulo = sesion?.usuario ?: "Modo de desarrollo: no hay un usuario autenticado",
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
