package pe.upeu.resi.resiup.presentation.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.resi.resiup.data.remote.ConfigEntorno
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.domain.model.ResultadoGarita
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoMovimiento
import pe.upeu.resi.resiup.platform.EscanerCamara
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ListaPaginada
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TarjetaEstadistica
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.aEstadoVisual
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.residente.DatosSolicitud
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.formatoLima
import pe.upeu.resi.resiup.presentation.util.hoyLima
import pe.upeu.resi.resiup.presentation.util.soloHoraLima

// ================================================================= escaneo (común)

/** Cámara + entrada manual del código. La entrada manual sólo existe en builds de desarrollo (pruebas sin cámara). */
@Composable
private fun ZonaEscaneo(activo: Boolean, onCodigo: (String) -> Unit) {
    val dev = koinInject<ConfigEntorno>().depuracion
    var manual by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Box(Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            EscanerCamara(activo = activo, onCodigo = onCodigo)
        }
        if (dev) {
            ChipEstado("Solo desarrollo", EstadoVisual.Advertencia)
            ResiCampoTexto(manual, { manual = it.trim() }, "Contenido del QR (sin cámara)", habilitado = activo)
            ResiBoton("Usar este código", { onCodigo(manual); manual = "" }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario, habilitado = activo && manual.isNotBlank())
        }
    }
}

// ================================================================= preceptor: inicio

@Composable
fun InicioPreceptorScreen(nombre: String, onIr: (String) -> Unit, vm: InicioPreceptorViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    ContenidoCarga(estado, vm::cargar) { ui ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TituloPantalla("Hola, ${nombre.substringBefore(' ')}", subtitulo = "Panel del preceptor")
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Box(Modifier.weight(1f)) {
                    ResiTarjeta(onClick = { onIr("solicitudes") }) {
                        Text("Por revisar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ui.porRevisar.toString(), style = pe.upeu.resi.resiup.presentation.theme.ResiEstilos.Numero)
                        Text("solicitudes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.weight(1f)) {
                    ResiTarjeta(onClick = { onIr("solicitudes") }) {
                        Text("Con apoderado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ui.conApoderado.toString(), style = pe.upeu.resi.resiup.presentation.theme.ResiEstilos.Numero)
                        Text("pendientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            ResiTarjeta(onClick = { onIr("escanear") }) {
                Text("Cultos de hoy", style = MaterialTheme.typography.titleMedium)
                if (ui.sesiones.isEmpty()) Text("Aún no se abrió ningún culto. Abre uno desde «Escanear».", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ui.sesiones.forEach { s ->
                    ElementoLista("Culto ${s.tipo.etiqueta.lowercase()}", subtitulo = "${s.presentes} presentes · corte ${s.corte.soloHoraLima()}", final = { ChipEstado(if (s.cerrada) "Cerrado" else "Abierto", if (s.cerrada) EstadoVisual.Neutro else EstadoVisual.Exito) })
                }
            }
        }
    }
}

// ================================================================= preceptor: asistencia

@Composable
fun AsistenciaCultoScreen(vm: AsistenciaCultoViewModel = koinViewModel()) {
    val e by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Asistencia a cultos", subtitulo = "Lunes a viernes")
        val sesion = e.sesion
        when {
            e.cargandoSesion -> Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
            sesion == null -> {
                e.errorSesion?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
                Text("Elige el culto que vas a registrar:", style = MaterialTheme.typography.bodyLarge)
                TipoCulto.entries.forEach { t -> ResiBoton("Abrir culto ${t.etiqueta.lowercase()}", { vm.abrir(t) }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario) }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    TarjetaEstadistica("Culto", sesion.tipo.etiqueta, Modifier.weight(1f), "corte ${sesion.corte.soloHoraLima()}")
                    TarjetaEstadistica("Presentes", e.presentes.toString(), Modifier.weight(1f))
                }
                ZonaEscaneo(activo = !e.procesando, onCodigo = vm::codigoLeido)
                e.errorSesion?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
                e.registros.forEach { r ->
                    BannerMensaje("${r.texto}${r.detalle?.let { " — $it" } ?: ""}", if (r.exito) EstadoVisual.Exito else EstadoVisual.Error)
                }
                ResiBoton("Cerrar culto", vm::cerrarSesion, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
            }
        }
    }
}

// ================================================================= preceptor: solicitudes

private sealed interface RutaSolicitudes {
    data object Lista : RutaSolicitudes
    data class Revision(val id: Long) : RutaSolicitudes
}

private enum class FiltroSolicitudes(val etiqueta: String) { PorRevisar("Por revisar"), ConApoderado("Con apoderado") }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SolicitudesPreceptorScreen() {
    var ruta by remember { mutableStateOf<RutaSolicitudes>(RutaSolicitudes.Lista) }
    BackHandler(enabled = ruta != RutaSolicitudes.Lista) { ruta = RutaSolicitudes.Lista }
    when (val r = ruta) {
        RutaSolicitudes.Lista -> ListaSolicitudesPreceptor { ruta = RutaSolicitudes.Revision(it) }
        is RutaSolicitudes.Revision -> RevisionSolicitudScreen(r.id) { ruta = RutaSolicitudes.Lista }
    }
}

@Composable
private fun ListaSolicitudesPreceptor(onAbrir: (Long) -> Unit) {
    var filtro by remember { mutableStateOf(FiltroSolicitudes.PorRevisar) }
    val porRevisar = koinViewModel<SolicitudesPorRevisarViewModel>()
    val conApoderado = koinViewModel<SolicitudesConApoderadoViewModel>()
    LaunchedEffect(Unit) { porRevisar.refrescar(); conApoderado.refrescar() }
    val vm = if (filtro == FiltroSolicitudes.PorRevisar) porRevisar else conApoderado
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Solicitudes", Modifier.padding(top = Espacio.l))
        Segmentos(FiltroSolicitudes.entries, filtro, { it.etiqueta }, { filtro = it })
        ListaPaginada(estado, vm::refrescar, vm::cargarMas, "Todo al día", "No hay solicitudes en este estado.") { s ->
            ResiTarjeta(onClick = { onAbrir(s.id) }) {
                ElementoLista(s.residenteNombre, subtitulo = "${s.destino} · ${s.salida.formatoLima()}", icono = ResiIconos.Solicitudes, final = { ChipEstado(s.tipo.etiqueta, EstadoVisual.Info) })
            }
        }
    }
}

@Composable
private fun RevisionSolicitudScreen(id: Long, onVolver: () -> Unit) {
    val vm = koinViewModel<SolicitudRevisionViewModel>(key = "revision-$id", parameters = { parametersOf(id) })
    val estado by vm.estado.collectAsState()
    val accion by vm.accion.collectAsState()
    LaunchedEffect(id) { vm.recargar() }
    var comentario by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        ContenidoCarga(estado, vm::cargar, Modifier.fillMaxWidth()) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TituloPantalla(s.residenteNombre, subtitulo = "${s.tipo.etiqueta} · ${s.destino}")
                ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual())
                ResiTarjeta { DatosSolicitud(s) }
                if (s.estado == EstadoSolicitud.PendienteApoderado) {
                    BannerMensaje("Falta la autorización del apoderado. Genera un enlace de un solo uso y entrégalo por el medio institucional.", EstadoVisual.Info)
                    ResiBoton("Generar enlace para el apoderado", vm::generarEnlace, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario, cargando = accion.trabajando)
                    accion.enlace?.let { e ->
                        ResiTarjeta {
                            Text("Enlace para ${e.apoderado.ifBlank { "el apoderado" }}", style = MaterialTheme.typography.titleMedium)
                            Text("Vence ${e.expiraEn.formatoLima()}; sólo sirve una vez.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(e.ruta, Modifier.padding(vertical = Espacio.s), style = MaterialTheme.typography.bodyMedium)
                            Text("Agrega la dirección del servidor delante de esta ruta al enviarlo.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (s.estado == EstadoSolicitud.PendientePreceptor) {
                    ResiCampoTexto(comentario, { comentario = it.take(300) }, "Comentario (opcional)", unaLinea = false)
                    ResiBoton("Aprobar", { vm.decidir(true, comentario) }, Modifier.fillMaxWidth(), cargando = accion.trabajando)
                    ResiBoton("Rechazar", { vm.decidir(false, comentario) }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, habilitado = !accion.trabajando)
                }
                accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
                accion.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
            }
        }
    }
}

// ================================================================= preceptor: limpieza

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LimpiezaPreceptorScreen(vm: LimpiezaPreceptorViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    BackHandler(enabled = s.seleccionado != null) { vm.seleccionar(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Evaluación de limpieza", Modifier.padding(top = Espacio.l), subtitulo = "Hoy, ${hoyLima().toString().fechaLegible()}")
        s.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        val sel = s.seleccionado
        if (sel == null) {
            ContenidoCarga(s.residentes, vm::cargarResidentes) { lista ->
                if (lista.isEmpty()) {
                    pe.upeu.resi.resiup.presentation.components.EstadoVacio("Sin residentes activos", "Registra residentes desde administración para poder evaluarlos.")
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        Text("Elige al residente:", style = MaterialTheme.typography.bodyLarge)
                        lista.forEach { r ->
                            ResiTarjeta(onClick = { vm.seleccionar(r) }) {
                                ElementoLista(r.nombreCompleto, subtitulo = "${r.codigo} · ${r.habitacion?.let { "Hab. $it" } ?: "Sin habitación"}", icono = ResiIconos.Perfil)
                            }
                        }
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                ResiTarjeta { ElementoLista(sel.nombreCompleto, subtitulo = "${sel.codigo} · ${sel.habitacion?.let { "Hab. $it" } ?: "Sin habitación"}", icono = ResiIconos.Perfil, final = { ChipEstado("${s.total}/20", EstadoVisual.Info) }) }
                CriterioLimpieza.entries.forEach { c ->
                    ResiTarjeta {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            Column(Modifier.weight(1f)) {
                                Text(c.etiqueta, style = MaterialTheme.typography.titleMedium)
                                Text("máx. ${c.maximo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                s.errores[c.name.uppercase()]?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
                            }
                            ResiBoton("−", { vm.ajustar(c, -1) }, variante = VarianteBoton.Secundario, habilitado = s.puntajes.getValue(c) > 0)
                            Text(s.puntajes.getValue(c).toString(), Modifier.padding(horizontal = Espacio.s), style = MaterialTheme.typography.titleLarge)
                            ResiBoton("+", { vm.ajustar(c, 1) }, variante = VarianteBoton.Secundario, habilitado = s.puntajes.getValue(c) < c.maximo)
                        }
                    }
                }
                ResiCampoTexto(s.observacion, vm::observacion, "Observación (opcional)", unaLinea = false)
                s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
                s.errores["fecha"]?.let { BannerMensaje(it, EstadoVisual.Error) }
                ResiBoton("Guardar evaluación", vm::guardar, Modifier.fillMaxWidth(), cargando = s.enviando)
                ResiBoton("Cancelar", { vm.seleccionar(null) }, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario)
            }
        }
    }
}

// ================================================================= garita

@Composable
fun EscanerGaritaScreen(vm: GaritaEscanerViewModel = koinViewModel()) {
    val e by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Garita", subtitulo = "Escanea el QR del residente. Requiere conexión con el servidor.")
        when (val x = e) {
            GaritaEstado.Esperando -> ZonaEscaneo(activo = true, onCodigo = vm::codigoLeido)
            GaritaEstado.Verificando -> Text("Verificando con el servidor…", style = MaterialTheme.typography.titleMedium)
            is GaritaEstado.Fallo -> {
                val sinRed = x.error == ErrorApi.SinConexion || x.error == ErrorApi.TiempoAgotado
                BannerMensaje(
                    if (sinRed) "No se pudo verificar: sin conexión con el servidor. No autorices la salida con datos locales; aplica el procedimiento manual institucional." else x.error.aMensaje(),
                    EstadoVisual.Error,
                )
                ResiBoton("Escanear otro", vm::siguiente, Modifier.fillMaxWidth())
            }
            is GaritaEstado.Resultado -> ResultadoGaritaVista(x, onRegistrar = vm::registrar, onSiguiente = vm::siguiente)
            is GaritaEstado.Registrado -> {
                BannerMensaje("${x.movimiento.tipo.etiqueta} registrada: ${x.movimiento.nombreCompleto} (${x.movimiento.codigo}) a las ${x.movimiento.registradoEn.soloHoraLima()}", EstadoVisual.Exito)
                x.movimiento.minutosTarde?.takeIf { it > 0 }?.let { BannerMensaje("Retorno con $it min de retraso. El preceptor lo revisará.", EstadoVisual.Advertencia) }
                ResiBoton("Escanear otro", vm::siguiente, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ResultadoGaritaVista(x: GaritaEstado.Resultado, onRegistrar: (TipoMovimiento) -> Unit, onSiguiente: () -> Unit) {
    val v = x.verificacion
    val permitido = v.movimientoPermitido
    val visual = when (v.resultado) {
        ResultadoGarita.AutorizadoSalida, ResultadoGarita.AutorizadoRetorno -> EstadoVisual.Exito
        ResultadoGarita.RetornoFueraDeHorario, ResultadoGarita.PermisoAunNoVigente -> EstadoVisual.Advertencia
        else -> EstadoVisual.Error
    }
    val titulo = when (v.resultado) {
        ResultadoGarita.AutorizadoSalida -> "AUTORIZADO PARA SALIR"
        ResultadoGarita.AutorizadoRetorno -> "RETORNO"
        ResultadoGarita.RetornoFueraDeHorario -> "RETORNO FUERA DE HORARIO"
        ResultadoGarita.QrInvalido -> "QR NO VÁLIDO"
        ResultadoGarita.QrRevocado -> "QR REVOCADO"
        ResultadoGarita.ResidenteInactivo -> "RESIDENTE INACTIVO"
        ResultadoGarita.SinPermiso -> "SIN PERMISO"
        ResultadoGarita.PermisoAunNoVigente -> "PERMISO AÚN NO VIGENTE"
        ResultadoGarita.PermisoVencido -> "PERMISO VENCIDO"
        ResultadoGarita.Desconocido -> "REQUIERE REVISIÓN"
    }
    BannerMensaje(titulo, visual)
    ResiTarjeta {
        v.nombreCompleto?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        v.codigo?.let { Text("Código $it", style = MaterialTheme.typography.bodyLarge) }
        v.habitacion?.let { Text("Habitación $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(v.mensaje, Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.bodyMedium)
        v.retornoAutorizado?.let { Text("Retorno autorizado: ${it.formatoLima()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    x.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
    if (permitido != null) {
        ResiBoton("Registrar ${permitido.etiqueta.lowercase()}", { onRegistrar(permitido) }, Modifier.fillMaxWidth(), cargando = x.registrando)
    }
    ResiBoton("Escanear otro", onSiguiente, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario, habilitado = !x.registrando)
}

@Composable
fun HistorialGaritaScreen(vm: HistorialGaritaViewModel = koinViewModel()) {
    LaunchedEffect(Unit) { vm.refrescar() }
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l)) {
        TituloPantalla("Historial", Modifier.padding(vertical = Espacio.l), subtitulo = "Movimientos recientes")
        ListaPaginada(estado, vm::refrescar, vm::cargarMas, "Sin movimientos", "Aún no se registraron salidas ni retornos.") { m ->
            ResiTarjeta {
                ElementoLista(
                    "${m.tipo.etiqueta} · ${m.nombreCompleto}", subtitulo = "${m.codigo} · ${m.registradoEn.formatoLima()}", icono = ResiIconos.Salir,
                    final = {
                        when {
                            m.anulado -> ChipEstado("Anulado", EstadoVisual.Neutro)
                            (m.minutosTarde ?: 0) > 0 -> ChipEstado("${m.minutosTarde} min tarde", EstadoVisual.Advertencia)
                        }
                    },
                )
            }
        }
    }
}
