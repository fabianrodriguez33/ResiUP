package pe.upeu.resi.resiup.presentation.familia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.Hijo
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ListaPaginada
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.aEstadoVisual
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.navigation.Destinos
import pe.upeu.resi.resiup.presentation.residente.CamposPermiso
import pe.upeu.resi.resiup.presentation.residente.DatosSolicitud
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.formatoLima

private fun Hijo.ubicacion() = listOfNotNull(habitacion?.let { "Habitación $it" }, cama?.let { "Cama $habitacion-$it" }).joinToString(" · ").ifBlank { "Sin habitación asignada" }

// ================================================================= inicio

@Composable
fun InicioFamiliaScreen(nombre: String, irA: (String) -> Unit) {
    val vm = koinViewModel<FamiliaInicioViewModel>()
    val s by vm.estado.collectAsState()
    var rechazando by remember { mutableStateOf<SolicitudPermiso?>(null) }
    LaunchedEffect(Unit) { vm.cargar() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Hola, ${nombre.substringBefore(' ')}", subtitulo = "Permisos de tus hijos")
        s.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }

        ContenidoCarga(s.hijos, vm::cargar, Modifier.fillMaxWidth()) { hijos ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                hijos.forEach { h ->
                    ResiTarjeta {
                        ElementoLista(
                            h.nombre, subtitulo = "${h.codigo} · ${h.ubicacion()}", icono = ResiIconos.Perfil,
                            final = { if (h.pendientes > 0) ChipEstado("${h.pendientes} por aceptar", EstadoVisual.Advertencia) },
                        )
                        Text(
                            if (h.puedeSolicitarSolo) "Puede pedir permisos corto y largo directamente al pastor." else "Sus permisos corto y largo pasan primero por ti.",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (hijos.isEmpty()) BannerMensaje("Tu cuenta todavía no tiene hijos vinculados. Comunícate con administración.", EstadoVisual.Info)
            }
        }

        ResiBoton("Enviar permiso (corto, largo o pernoctar fuera)", { irA(Destinos.Nuevo) }, Modifier.fillMaxWidth())

        Text("Por aceptar", style = MaterialTheme.typography.titleMedium)
        ContenidoCarga(s.porAceptar, vm::cargar, Modifier.fillMaxWidth()) { lista ->
            if (lista.isEmpty()) {
                Text("No hay permisos esperando tu respuesta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            lista.forEach { p ->
                ResiTarjeta {
                    ElementoLista(p.residenteNombre, subtitulo = "Permiso ${p.tipo.etiqueta.lowercase()} · ${p.destino}", icono = ResiIconos.Permisos)
                    DatosSolicitud(p)
                    ResiBoton("Aceptar", { vm.decidir(p.id, true) }, Modifier.fillMaxWidth().padding(top = Espacio.s), cargando = s.trabajandoId == p.id)
                    ResiBoton("Rechazar", { rechazando = p }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, habilitado = s.trabajandoId == null)
                }
            }
        }
    }
    rechazando?.let { p ->
        DialogoConfirmacion(
            "Rechazar permiso", "¿Rechazar el permiso de ${p.residenteNombre} a ${p.destino}? No llegará al pastor.", "Rechazar",
            { rechazando = null; vm.decidir(p.id, false) }, { rechazando = null }, destructivo = true,
        )
    }
}

// ================================================================= historial y detalle

private sealed interface RutaFamilia {
    data object Lista : RutaFamilia
    data class Detalle(val id: Long) : RutaFamilia
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SolicitudesFamiliaScreen() {
    var ruta by remember { mutableStateOf<RutaFamilia>(RutaFamilia.Lista) }
    BackHandler(enabled = ruta != RutaFamilia.Lista) { ruta = RutaFamilia.Lista }
    when (val r = ruta) {
        RutaFamilia.Lista -> ListaFamilia { ruta = RutaFamilia.Detalle(it) }
        is RutaFamilia.Detalle -> DetalleFamilia(r.id) { ruta = RutaFamilia.Lista }
    }
}

@Composable
private fun ListaFamilia(onAbrir: (Long) -> Unit) {
    val vm = koinViewModel<FamiliaSolicitudesViewModel>()
    LaunchedEffect(Unit) { vm.refrescar() }
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Permisos de tus hijos", Modifier.padding(top = Espacio.l))
        ListaPaginada(estado, vm::refrescar, vm::cargarMas, "Sin permisos", "Aquí aparecerán los permisos que tus hijos pidan y los que tú envíes.") { s ->
            ResiTarjeta(onClick = { onAbrir(s.id) }) {
                ElementoLista(
                    s.residenteNombre, subtitulo = "${s.tipo.etiqueta} · ${s.destino} · ${s.salida.formatoLima()}", icono = ResiIconos.Permisos,
                    final = { ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual()) },
                )
            }
        }
    }
}

@Composable
private fun DetalleFamilia(id: Long, onVolver: () -> Unit) {
    val vm = koinViewModel<FamiliaDetalleViewModel>(key = "familia-$id", parameters = { parametersOf(id) })
    val estado by vm.estado.collectAsState()
    val accion by vm.accion.collectAsState()
    LaunchedEffect(id) { vm.recargar() }
    var confirmarCancelar by remember { mutableStateOf(false) }
    var confirmarRechazo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        ContenidoCarga(estado, vm::cargar, Modifier.fillMaxWidth()) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TituloPantalla(s.residenteNombre, subtitulo = "Permiso ${s.tipo.etiqueta.lowercase()} · ${s.destino}")
                ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual())
                ResiTarjeta { DatosSolicitud(s) }
                s.comentarioDecision?.let { BannerMensaje("Comentario del pastor: $it", EstadoVisual.Info) }
                if (s.estado == EstadoSolicitud.PendienteApoderado) {
                    BannerMensaje("${s.residenteNombre} pidió este permiso. Si lo aceptas, pasa al pastor para su aprobación.", EstadoVisual.Info)
                    ResiBoton("Aceptar", { vm.decidir(true) }, Modifier.fillMaxWidth(), cargando = accion.trabajando)
                    ResiBoton("Rechazar", { confirmarRechazo = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, habilitado = !accion.trabajando)
                }
                if (s.estado == EstadoSolicitud.PendientePreceptor) {
                    BannerMensaje("Enviado al pastor. Esperando su aprobación.", EstadoVisual.Info)
                }
                val cancelable = s.estado == EstadoSolicitud.PendienteApoderado || s.estado == EstadoSolicitud.PendientePreceptor || s.estado == EstadoSolicitud.Aprobado
                if (cancelable) ResiBoton("Cancelar permiso", { confirmarCancelar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !accion.trabajando)
                accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
                accion.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
            }
        }
    }
    if (confirmarCancelar) {
        DialogoConfirmacion("Cancelar permiso", "¿Seguro que deseas cancelar este permiso?", "Cancelar permiso", { confirmarCancelar = false; vm.cancelar() }, { confirmarCancelar = false }, destructivo = true)
    }
    if (confirmarRechazo) {
        DialogoConfirmacion("Rechazar permiso", "No llegará al pastor. ¿Rechazar este permiso?", "Rechazar", { confirmarRechazo = false; vm.decidir(false) }, { confirmarRechazo = false }, destructivo = true)
    }
}

// ================================================================= nuevo permiso

@Composable
fun NuevoPermisoFamiliaScreen(irA: (String) -> Unit) {
    val vm = koinViewModel<FamiliaNuevoViewModel>()
    val s by vm.estado.collectAsState()
    val hijos by vm.hijos.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Enviar permiso", subtitulo = "Va directo al pastor para su aprobación")
        val creada = s.creada
        if (creada != null) {
            BannerMensaje("Permiso enviado al pastor: ${creada.tipo.etiqueta.lowercase()} de ${creada.residenteNombre} a ${creada.destino}.", EstadoVisual.Exito)
            ResiBoton("Enviar otro permiso", vm::reiniciar, Modifier.fillMaxWidth())
            ResiBoton("Ver permisos", { irA(Destinos.Solicitudes) }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
        } else {
            ContenidoCarga(hijos, vm::cargarHijos, Modifier.fillMaxWidth()) { lista ->
                if (lista.isEmpty()) {
                    BannerMensaje("Tu cuenta todavía no tiene hijos vinculados. Comunícate con administración.", EstadoVisual.Info)
                } else {
                    if (lista.size > 1) {
                        Text("¿Para quién es el permiso?", style = MaterialTheme.typography.labelMedium)
                        lista.forEach { h ->
                            ResiBoton(
                                h.nombre, { vm.hijo(h.id) }, Modifier.fillMaxWidth(),
                                variante = if (s.residenteId == h.id) VarianteBoton.Primario else VarianteBoton.Secundario,
                            )
                        }
                        s.errores["residenteId"]?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
                    } else {
                        Text("Para ${lista.first().nombre}", style = MaterialTheme.typography.titleMedium)
                    }
                    CamposPermiso(s, vm)
                    s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
                    ResiBoton("Enviar al pastor", vm::enviar, Modifier.fillMaxWidth(), cargando = s.enviando)
                }
            }
        }
    }
}
