package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import pe.upeu.resi.resiup.presentation.components.BotonAtras
import pe.upeu.resi.resiup.presentation.components.LocalVolver
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.estadoParaResidente
import pe.upeu.resi.resiup.presentation.theme.Acento
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
    // Dentro del flujo, la flecha de cada pantalla vuelve a la lista (en la lista, al inicio).
    val alInicio = LocalVolver.current
    androidx.compose.runtime.CompositionLocalProvider(LocalVolver provides (if (ruta == RutaPermisos.Lista) alInicio else ({ ruta = RutaPermisos.Lista }))) {
        when (val r = ruta) {
            RutaPermisos.Lista -> ListaPermisos(onNueva = { ruta = RutaPermisos.Nueva }, onDetalle = { ruta = RutaPermisos.Detalle(it) })
            RutaPermisos.Nueva -> SolicitudNuevaScreen(onVolver = { ruta = RutaPermisos.Lista }, onCreada = { ruta = RutaPermisos.Detalle(it) })
            is RutaPermisos.Detalle -> SolicitudDetalleScreen(r.id, onVolver = { ruta = RutaPermisos.Lista })
        }
    }
}

private fun TipoPermiso.titulo() = "Permiso " + if (this == TipoPermiso.Pernocte) "Pernoctar" else etiqueta

@Composable
private fun ListaPermisos(onNueva: () -> Unit, onDetalle: (Long) -> Unit) {
    val vm = koinViewModel<MisSolicitudesViewModel>()
    val habilitacion = koinViewModel<HabilitacionViewModel>()
    val hab by habilitacion.estado.collectAsState()
    LaunchedEffect(Unit) { habilitacion.cargar() }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Permisos", Modifier.padding(top = Espacio.l), subtitulo = "Gestión e historial")
        hab?.takeIf { !it.habilitado }?.let { h ->
            BannerMensaje("Estás inhabilitado para solicitar permisos hasta el sábado ${h.hasta.fechaLegible()}: ${h.motivo.orEmpty()}. Si es una falta, el pastor puede justificarla hasta el lunes.", EstadoVisual.Advertencia)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ResiBoton("Crear permiso", onNueva, habilitado = hab?.habilitado != false)
        }
        val pendientes by vm.pendientes.collectAsState()
        pendientes.forEach { p ->
            ResiTarjeta {
                ElementoLista(
                    p.tipo.titulo(), subtitulo = p.salida.formatoLima(), icono = ResiIconos.Permisos, acento = Acento.Azul,
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
                EstadoVacio("Sin permisos", "Cuando crees un permiso aparecerá aquí con su estado.")
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    items(lista) { s ->
                        val (texto, visual) = s.estadoParaResidente()
                        ResiTarjeta(onClick = { onDetalle(s.id) }) {
                            ElementoLista(s.tipo.titulo(), subtitulo = s.salida.formatoLima(), icono = ResiIconos.Permisos, acento = Acento.Azul, final = { ChipEstado(texto, visual) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolicitudNuevaScreen(onVolver: () -> Unit, onCreada: (Long) -> Unit) {
    val vm = koinViewModel<SolicitudFormViewModel>()
    val s by remember(vm) { vm.descartarEnviado(); vm.estado }.collectAsState()
    LaunchedEffect(s.creada) { s.creada?.let { onCreada(it.id) } }
    LaunchedEffect(s.enCola) { if (s.enCola) onVolver() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Nuevo permiso", subtitulo = "Salida corta, larga o pernoctar fuera")
        CamposPermiso(s, vm)
        s.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Enviar permiso", vm::enviar, Modifier.fillMaxWidth(), cargando = s.enviando)
    }
}

@Composable
private fun SolicitudDetalleScreen(id: Long, onVolver: () -> Unit) {
    val vm = koinViewModel<SolicitudDetalleViewModel>(key = "detalle-$id", parameters = { parametersOf(id) })
    val estado by vm.estado.collectAsState()
    val accion by vm.accion.collectAsState()
    LaunchedEffect(id) { vm.recargar() }
    var confirmarCancelar by remember { mutableStateOf(false) }
    var verQr by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ContenidoCarga(estado, vm::cargar, Modifier.fillMaxWidth()) { s ->
            val (texto, visual) = s.estadoParaResidente()
            val aprobado = s.estado == EstadoSolicitud.Aprobado
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                // Como en el prototipo: flecha y estado a la izquierda (el estado debajo de la flecha) y el QR a la derecha.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        BotonAtras(onVolver)
                        ChipEstado(texto, visual, Modifier.width(128.dp), centrado = true)
                    }
                    Spacer(Modifier.weight(1f))
                    if (aprobado) {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary).clickable(onClickLabel = "Mostrar mi código QR", role = Role.Button) { verQr = true },
                            contentAlignment = Alignment.Center,
                        ) { ResiIcono(ResiIconos.Qr, null, tint = Color.White, tamano = 32.dp) }
                    }
                }
                ResiTarjeta { DatosSolicitud(s) }
                if (s.garitaSalida != null) {
                    ResiTarjeta {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                            DatoCelda("Garita Salida", s.garitaSalida?.formatoLima() ?: "-", Modifier.weight(1f))
                            DatoCelda("Garita Entrada", s.garitaEntrada?.formatoLima() ?: "-", Modifier.weight(1f))
                        }
                    }
                }
                s.comentarioDecision?.let { BannerMensaje("Comentario del pastor: $it", EstadoVisual.Info) }
                if (s.estado == EstadoSolicitud.PendienteApoderado) {
                    BannerMensaje("Esperando a tu padre/madre: debe aceptar este permiso; luego lo revisa el pastor.", EstadoVisual.Info)
                }
                if (s.estado == EstadoSolicitud.PendientePreceptor) {
                    BannerMensaje("El pastor está revisando tu permiso.", EstadoVisual.Info)
                }
                s.ampliaciones.forEach { BannerMensaje("Ampliación hasta ${it.retornoNuevo.formatoLima()}: ${it.estado.etiqueta}", EstadoVisual.Info) }
                s.justificaciones.forEach { BannerMensaje("Justificación: ${it.estado.etiqueta}${it.comentario?.let { c -> " — $c" } ?: ""}", if (it.estado == EstadoRevision.Aprobada) EstadoVisual.Exito else EstadoVisual.Info) }

                val vencido = aprobado && ahora() > s.retornoAutorizado
                if (vencido && s.justificaciones.none { it.estado == EstadoRevision.Pendiente }) {
                    TramiteResidente(s.id, justificacion = true, retornoAutorizado = s.retornoAutorizado, onEnviado = vm::recargar)
                } else if (aprobado && !vencido && s.garitaSalida != null && s.ampliaciones.none { it.estado == EstadoRevision.Pendiente }) {
                    TramiteResidente(s.id, justificacion = false, retornoAutorizado = s.retornoAutorizado, onEnviado = vm::recargar)
                }
                val cancelable = s.garitaSalida == null && (s.estado == EstadoSolicitud.PendienteApoderado || s.estado == EstadoSolicitud.PendientePreceptor || s.estado == EstadoSolicitud.Aprobado)
                if (cancelable) ResiBoton("Cancelar permiso", { confirmarCancelar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, habilitado = !accion.trabajando)
                accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
                accion.error?.takeIf { it !is pe.upeu.resi.resiup.domain.error.ErrorApi.Validacion }?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
            }
        }
    }
    if (verQr) QrFlotante({ verQr = false })
    if (confirmarCancelar) {
        DialogoConfirmacion("Cancelar permiso", "¿Seguro que deseas cancelar este permiso?", "Cancelar permiso", { confirmarCancelar = false; vm.cancelar() }, { confirmarCancelar = false }, destructivo = true)
    }
}

@Composable
internal fun DatosSolicitud(s: SolicitudPermiso) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        val izquierda = buildList {
            add("Tipo" to if (s.tipo == TipoPermiso.Pernocte) "Pernoctar" else s.tipo.etiqueta)
            add("Lugar" to s.destino)
            add("Motivo" to s.motivo)
            if (s.tipo == TipoPermiso.Pernocte) {
                s.responsableNombre?.let { add("Responsable de la casa" to it) }
                s.responsableCelular?.let { add("Celular del responsable" to it) }
                s.acompanante?.let { add("Acompañado de" to it) }
            }
        }
        val derecha = buildList {
            add("Salida" to s.salida.formatoLima())
            add("Ingreso" to s.retornoAutorizado.formatoLima())
            if (s.residenteNombre.isNotBlank()) add("Residente" to s.residenteNombre)
            add("Registrada" to s.creadoEn.formatoLima())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.m)) { izquierda.forEach { (e, v) -> DatoCelda(e, v) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.m)) { derecha.forEach { (e, v) -> DatoCelda(e, v) } }
        }
    }
}

@Composable
private fun DatoCelda(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
