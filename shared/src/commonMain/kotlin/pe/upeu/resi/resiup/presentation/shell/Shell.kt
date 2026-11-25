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
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import pe.upeu.resi.resiup.presentation.theme.ResiTheme
import resiup.shared.generated.resources.Res
import resiup.shared.generated.resources.residencia_fondo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.backhandler.BackHandler
import pe.upeu.resi.resiup.presentation.components.LocalVolver
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
import pe.upeu.resi.resiup.presentation.notificaciones.BandejaScreen
import pe.upeu.resi.resiup.presentation.notificaciones.CampanaAvisos
import pe.upeu.resi.resiup.presentation.notificaciones.GestorAvisos
import org.koin.compose.koinInject
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import pe.upeu.resi.resiup.presentation.perfil.PerfilCuentaScreen
import pe.upeu.resi.resiup.presentation.familia.InicioFamiliaScreen
import pe.upeu.resi.resiup.presentation.familia.NuevoPermisoFamiliaScreen
import pe.upeu.resi.resiup.presentation.familia.SolicitudesFamiliaScreen
import pe.upeu.resi.resiup.presentation.personal.AsistenciaCultoScreen
import pe.upeu.resi.resiup.presentation.personal.EscanerGaritaScreen
import pe.upeu.resi.resiup.presentation.personal.HistorialGaritaScreen
import pe.upeu.resi.resiup.presentation.personal.InicioPreceptorScreen
import pe.upeu.resi.resiup.presentation.personal.LimpiezaPreceptorScreen
import pe.upeu.resi.resiup.presentation.personal.SolicitudesPreceptorScreen
import pe.upeu.resi.resiup.presentation.residente.AsistenciasScreen
import pe.upeu.resi.resiup.presentation.residente.InicioResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.LimpiezaResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.NotasResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.PerfilResidenteScreen
import pe.upeu.resi.resiup.presentation.residente.PermisosResidenteScreen
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.presentation.navigation.destinos
import pe.upeu.resi.resiup.presentation.theme.Espacio

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ShellPrincipal(rol: Rol, sesion: Sesion?, onCerrarSesion: () -> Unit) {
    val items = remember(rol) { rol.destinos() }
    var seleccionado by rememberSaveable(rol) { mutableStateOf(items.first().id) }
    val actual = items.firstOrNull { it.id == seleccionado } ?: items.first() // las pantallas sin pestaña (reservas) usan solo [seleccionado]
    val avisos: GestorAvisos = koinInject()
    val noLeidas by avisos.noLeidas.collectAsState()
    var verBandeja by rememberSaveable { mutableStateOf(false) }
    var subpantalla by remember { mutableStateOf(false) } // el administrador navega por dentro de su inicio
    // Pantalla secundaria abierta desde los accesos del inicio (por ahora, Notas del residente). La barra sigue visible.
    var secundaria by rememberSaveable { mutableStateOf<String?>(null) }
    // Residente y pastor navegan desde los accesos del inicio (sin barra inferior): cada pantalla se abre con flecha de regreso.
    val sinBarra = true // todos los roles navegan desde los accesos del inicio, con flecha de regreso
    val irA: (String) -> Unit = { destino -> if (destino == Destinos.Notas) secundaria = destino else { seleccionado = destino; secundaria = null } }
    val inicio = items.first().id
    val volver: (() -> Unit)? = if (sinBarra && (seleccionado != inicio || secundaria != null)) ({ seleccionado = inicio; secundaria = null }) else null
    BackHandler(enabled = volver != null) { volver?.invoke() }
    if (sesion != null) LaunchedEffect(sesion.usuario) { avisos.vigilar() }
    val cola: pe.upeu.resi.resiup.data.repository.ColaPermisos = koinInject()
    if (sesion != null && rol == Rol.Residente) LaunchedEffect(sesion.usuario) { cola.vigilar() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Ilustración de la residencia, muy tenue, al pie de todas las pantallas.
        Image(
            painterResource(Res.drawable.residencia_fondo), null,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().alpha(if (ResiTheme.oscuro) 0.10f else 0.22f), contentScale = ContentScale.FillWidth,
        )
        CompositionLocalProvider(LocalVolver provides volver) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(bottom = if (sinBarra) 0.dp else 84.dp)) {
            if (sesion != null && verBandeja) {
                BandejaScreen(onVolver = { verBandeja = false }, onIr = { n ->
                    // Tocar un aviso lleva a la pantalla de la que habla (según su texto y el rol).
                    val d = destinoDeAviso(rol, n)
                    if (d != null) { verBandeja = false; irA(d) }
                })
            } else if (sesion == null) {
                // Vista previa de desarrollo: sin sesión no se consulta el servidor.
                when (actual.id) {
                    Destinos.Perfil, Destinos.Mas -> PantallaCuenta(rol, null, onCerrarSesion)
                    else -> PantallaPendiente(actual.etiqueta, rol)
                }
            } else if (rol == Rol.Residente && secundaria == Destinos.Notas) {
                NotasResidenteScreen(onVolver = { secundaria = null })
            } else {
                ContenidoReal(rol, sesion, seleccionado, irA, onCerrarSesion, { subpantalla = it })
            }
        }
        }
        if (sesion != null && !verBandeja && volver == null) {
            CampanaAvisos(noLeidas, { verBandeja = true }, Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(top = Espacio.xs, end = Espacio.s))
        }
        // Semestre en curso, visible en el inicio de cada rol (arriba a la izquierda).
        if (sesion != null && !verBandeja && volver == null && !subpantalla) {
            val api = koinInject<pe.upeu.resi.resiup.domain.repository.AdministracionRepository>()
            val semestre by androidx.compose.runtime.produceState<String?>(initialValue = null, sesion.usuario) { value = api.semestreActual().getOrNull()?.nombre }
            semestre?.let { n ->
                Text("Semestre $n", Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(top = Espacio.m, start = Espacio.l), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        pe.upeu.resi.resiup.presentation.components.AvisoAlPie(Modifier.align(Alignment.BottomCenter))
        if (!sinBarra) BarraNavegacionResi(
            items, seleccionado, { seleccionado = it; verBandeja = false; secundaria = null },
            Modifier.align(Alignment.BottomCenter).padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }
}

@Composable
private fun ContenidoReal(rol: Rol, sesion: Sesion, destino: String, irA: (String) -> Unit, onCerrarSesion: () -> Unit, onSubpantalla: (Boolean) -> Unit = {}) {
    when (rol) {
        Rol.Residente -> when (destino) {
            Destinos.Inicio -> InicioResidenteScreen(irA)
            Destinos.Permisos -> PermisosResidenteScreen()
            Destinos.Asistencias -> AsistenciasScreen()
            Destinos.Limpieza -> LimpiezaResidenteScreen()
            Destinos.Reservas -> pe.upeu.resi.resiup.presentation.residente.ReservasResidenteScreen()
            Destinos.Ranking -> pe.upeu.resi.resiup.presentation.residente.RankingSemanaScreen()
            else -> PerfilResidenteScreen(sesion, onCerrarSesion)
        }
        Rol.Preceptor -> when (destino) {
            Destinos.Inicio -> InicioPreceptorScreen(sesion.nombreCompleto, irA)
            Destinos.Escanear -> AsistenciaCultoScreen(onCultos = { irA(Destinos.Cultos) })
            Destinos.Cultos -> pe.upeu.resi.resiup.presentation.personal.CultosPastorScreen()
            Destinos.Reservas -> pe.upeu.resi.resiup.presentation.personal.ReservasPastorScreen()
            Destinos.Solicitudes -> SolicitudesPreceptorScreen()
            Destinos.Limpieza -> LimpiezaPreceptorScreen()
            else -> PerfilCuentaScreen(rol, onCerrarSesion)
        }
        Rol.Garita -> when (destino) {
            Destinos.Inicio -> pe.upeu.resi.resiup.presentation.personal.InicioGaritaScreen(sesion.nombreCompleto, irA)
            Destinos.Escanear -> EscanerGaritaScreen()
            Destinos.Solicitudes -> pe.upeu.resi.resiup.presentation.personal.PermisosGaritaScreen()
            Destinos.Historial -> HistorialGaritaScreen()
            else -> PerfilCuentaScreen(rol, onCerrarSesion)
        }
        Rol.Apoderado -> when (destino) {
            Destinos.Inicio -> InicioFamiliaScreen(sesion.nombreCompleto, irA)
            Destinos.Solicitudes -> SolicitudesFamiliaScreen()
            Destinos.Nuevo -> NuevoPermisoFamiliaScreen(irA)
            else -> PerfilCuentaScreen(rol, onCerrarSesion)
        }
        Rol.Administrador -> when (destino) {
            Destinos.Inicio -> pe.upeu.resi.resiup.presentation.admin.AdministradorScreen(sesion.nombreCompleto, irA, onSubpantalla)
            else -> PerfilCuentaScreen(rol, onCerrarSesion)
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
            mensaje = "Esta es una vista previa de navegación. Inicia sesión con un usuario real para ver los datos del servidor.",
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

/** Pantalla a la que lleva un aviso, según de qué habla; `null` si no corresponde a ninguna (se queda en la bandeja). */
internal fun destinoDeAviso(rol: Rol, n: pe.upeu.resi.resiup.domain.model.Notificacion): String? {
    val t = (n.titulo + " " + n.mensaje).lowercase()
    return when {
        "reserva" in t -> when (rol) { Rol.Residente -> Destinos.Reservas; Rol.Preceptor -> Destinos.Reservas; else -> null }
        "culto" in t || "asistencia" in t -> when (rol) { Rol.Residente -> Destinos.Asistencias; Rol.Preceptor -> Destinos.Cultos; else -> null }
        "limpieza" in t || "evaluaci" in t || "cuarto" in t || "habitaci" in t -> when (rol) { Rol.Residente -> Destinos.Notas; Rol.Preceptor -> Destinos.Limpieza; else -> null }
        "permiso" in t || "salida" in t || "retorno" in t || "pernocte" in t -> when (rol) {
            Rol.Residente -> Destinos.Permisos
            Rol.Preceptor, Rol.Apoderado, Rol.Garita -> Destinos.Solicitudes
            else -> null
        }
        else -> null
    }
}
