package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.resi.resiup.domain.model.EstadoRevision
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.OrigenSolicitud
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CampoFechaHora
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ListaPaginada
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.aEstadoVisual
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.tramites.TramiteResidente
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.ContenidoConsulta
import pe.upeu.resi.resiup.presentation.util.ahora
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.formatoLima

private sealed interface RutaPermisos {
    data object Lista : RutaPermisos
    data object Nueva : RutaPermisos
    data class Detalle(val id: Long) : RutaPermisos
}

private enum class Seccion(val etiqueta: String) { Solicitudes("Solicitudes"), Movimientos("Salidas y retornos") }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PermisosResidenteScreen() {
    var ruta by remember { mutableStateOf<RutaPermisos>(RutaPermisos.Lista) }
    BackHandler(enabled = ruta != RutaPermisos.Lista) { ruta = RutaPermisos.Lista }
    when (val r = ruta) {
        RutaPermisos.Lista -> ListaPermisos(onNueva = { ruta = RutaPermisos.Nueva }, onDetalle = { ruta = RutaPermisos.Detalle(it) })
        RutaPermisos.Nueva -> SolicitudNuevaScreen(onVolver = { ruta = RutaPermisos.Lista }, onCreada = { ruta = RutaPermisos.Detalle(it) })
        is RutaPermisos.Detalle -> SolicitudDetalleScreen(r.id, onVolver = { ruta = RutaPermisos.Lista })
    }
}

@Composable
private fun ListaPermisos(onNueva: () -> Unit, onDetalle: (Long) -> Unit) {
    val vm = koinViewModel<MisSolicitudesViewModel>()
    val movimientos = koinViewModel<MovimientosViewModel>()
    val habilitacion = koinViewModel<HabilitacionViewModel>()
    val hab by habilitacion.estado.collectAsState()
    LaunchedEffect(Unit) { habilitacion.cargar() }
    var seccion by remember { mutableStateOf(Seccion.Solicitudes) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Permisos", Modifier.padding(top = Espacio.l))
        Segmentos(Seccion.entries, seccion, { it.etiqueta }, { seccion = it })
        if (seccion == Seccion.Solicitudes) {
            hab?.takeIf { !it.habilitado }?.let { h ->
                BannerMensaje("Estás inhabilitado para solicitar permisos hasta el sábado ${h.hasta.fechaLegible()}: ${h.motivo.orEmpty()}. Si es una falta, el pastor puede justificarla hasta el lunes.", EstadoVisual.Advertencia)
            }
            ResiBoton("Solicitar permiso", onNueva, Modifier.fillMaxWidth(), habilitado = hab?.habilitado != false)
            val pendientes by vm.pendientes.collectAsState()
            pendientes.forEach { p ->
                ResiTarjeta {
                    ElementoLista(
                        p.destino, subtitulo = "${p.tipo.etiqueta} · ${p.salida.formatoLima()}", icono = ResiIconos.Permisos,
                        final = { ChipEstado(if (p.error == null) "Pendiente de envío" else "No enviado", if (p.error == null) EstadoVisual.Advertencia else EstadoVisual.Error) },
                    )
                    if (p.error == null) {
                        Text("Sin conexión: se enviará solo cuando vuelva la red.", Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        BannerMensaje(p.error, EstadoVisual.Error, Modifier.padding(top = Espacio.s))
                        ResiBoton("Descartar", { vm.descartar(p.clave) }, Modifier.fillMaxWidth().padding(top = Espacio.s), variante = VarianteBoton.Secundario)
                    }
                }
            }
            val estado by vm.estado.collectAsState()
            ContenidoConsulta(estado, vm::reintentar) { lista ->
                if (lista.isEmpty()) {
                    EstadoVacio("Sin solicitudes", "Cuando solicites un permiso de salida aparecerá aquí con su estado.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                        items(lista) { s ->
                            ResiTarjeta(onClick = { onDetalle(s.id) }) {
                                ElementoLista(s.destino, subtitulo = "${s.tipo.etiqueta} · ${s.salida.formatoLima()}", icono = ResiIconos.Permisos, final = { ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual()) })
                            }
                        }
                    }
                }
            }
        } else {
            val estado by movimientos.estado.collectAsState()
            ListaPaginada(estado, movimientos::refrescar, movimientos::cargarMas, "Sin movimientos", "Tus salidas y retornos registrados en garita aparecerán aquí.") { m ->
                ResiTarjeta {
                    ElementoLista(
                        m.tipo.etiqueta, subtitulo = m.registradoEn.formatoLima(), icono = ResiIconos.Salir,
                        final = { m.minutosTarde?.takeIf { it > 0 }?.let { ChipEstado("$it min tarde", EstadoVisual.Advertencia) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SolicitudNuevaScreen(onVolver: () -> Unit, onCreada: (Long) -> Unit) {
    val vm = koinViewModel<SolicitudFormViewModel>()
    val s by vm.estado.collectAsState()
    val privilegio by vm.puedeSolicitarSolo.collectAsState()
    LaunchedEffect(s.creada) { s.creada?.let { onCreada(it.id) } }
    LaunchedEffect(s.enCola) { if (s.enCola) onVolver() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        TituloPantalla("Nuevo permiso", subtitulo = "Salida corta o larga")
        BannerMensaje(
            if (privilegio == true) "Tu permiso quedará aprobado al enviarlo y el pastor lo verá en su lista."
            else "Tu solicitud irá primero a tu padre/madre; cuando la acepte quedará aprobada.",
            EstadoVisual.Info,
        )
        CamposPermiso(s, vm)
        s.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Enviar solicitud", vm::enviar, Modifier.fillMaxWidth(), cargando = s.enviando)
        Text(
            "¿Vas a pernoctar fuera? Ese permiso lo envía tu padre o madre desde su cuenta.",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SolicitudDetalleScreen(id: Long, onVolver: () -> Unit) {
    val vm = koinViewModel<SolicitudDetalleViewModel>(key = "detalle-$id", parameters = { parametersOf(id) })
    val estado by vm.estado.collectAsState()
    val accion by vm.accion.collectAsState()
    LaunchedEffect(id) { vm.recargar() }
    var confirmarCancelar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        ContenidoCarga(estado, vm::cargar, Modifier.fillMaxWidth()) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TituloPantalla(s.destino, subtitulo = "Permiso ${s.tipo.etiqueta.lowercase()}")
                ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual())
                ResiTarjeta { DatosSolicitud(s) }
                s.comentarioDecision?.let { BannerMensaje("Comentario del pastor: $it", EstadoVisual.Info) }
                if (s.estado == EstadoSolicitud.PendienteApoderado) {
                    BannerMensaje("Esperando a tu padre/madre: debe aceptar este permiso desde su cuenta (o con el enlace que le dé el personal).", EstadoVisual.Info)
                }
                s.ampliaciones.forEach { BannerMensaje("Ampliación hasta ${it.retornoNuevo.formatoLima()}: ${it.estado.etiqueta}", EstadoVisual.Info) }
                s.justificaciones.forEach { BannerMensaje("Justificación: ${it.estado.etiqueta}${it.comentario?.let { c -> " — $c" } ?: ""}", if (it.estado == EstadoRevision.Aprobada) EstadoVisual.Exito else EstadoVisual.Info) }

                val aprobado = s.estado == EstadoSolicitud.Aprobado
                val vencido = aprobado && ahora() > s.retornoAutorizado
                if (vencido && s.justificaciones.none { it.estado == EstadoRevision.Pendiente }) {
                    TramiteResidente(s.id, justificacion = true, retornoAutorizado = s.retornoAutorizado, onEnviado = vm::recargar)
                } else if (aprobado && !vencido && s.ampliaciones.none { it.estado == EstadoRevision.Pendiente }) {
                    TramiteResidente(s.id, justificacion = false, retornoAutorizado = s.retornoAutorizado, onEnviado = vm::recargar)
                }
                val cancelable = s.estado == EstadoSolicitud.PendienteApoderado || s.estado == EstadoSolicitud.Aprobado
                if (cancelable) ResiBoton("Cancelar solicitud", { confirmarCancelar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, habilitado = !accion.trabajando)
                accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
                accion.error?.takeIf { it !is pe.upeu.resi.resiup.domain.error.ErrorApi.Validacion }?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
            }
        }
    }
    if (confirmarCancelar) {
        DialogoConfirmacion("Cancelar solicitud", "¿Seguro que deseas cancelar este permiso?", "Cancelar solicitud", { confirmarCancelar = false; vm.cancelar() }, { confirmarCancelar = false }, destructivo = true)
    }
}

@Composable
internal fun DatosSolicitud(s: SolicitudPermiso) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Dato("Tipo", s.tipo.etiqueta)
        Dato("Lugar", s.destino)
        Dato("Motivo", s.motivo)
        if (s.tipo == TipoPermiso.Pernocte) {
            s.responsableNombre?.let { Dato("Responsable de la casa", it) }
            s.responsableCelular?.let { Dato("Celular del responsable", it) }
            s.acompanante?.let { Dato("Acompañado de", it) }
        }
        Dato("Salida", s.salida.formatoLima())
        Dato("Ingreso solicitado", s.retornoSolicitado.formatoLima())
        if (s.retornoAutorizado != s.retornoSolicitado) Dato("Ingreso autorizado", s.retornoAutorizado.formatoLima())
        if (s.residenteNombre.isNotBlank()) Dato("Residente", "${s.residenteNombre} (${s.residenteCodigo})")
        Dato("Enviado por", if (s.origen == OrigenSolicitud.Apoderado) "Padre/madre/apoderado" else "El residente")
        Dato("Registrada", s.creadoEn.formatoLima())
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
