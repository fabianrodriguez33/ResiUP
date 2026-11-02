package pe.upeu.resi.resiup.presentation.personal

import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.components.TarjetaDato
import pe.upeu.resi.resiup.presentation.components.ModuloUi
import pe.upeu.resi.resiup.presentation.components.EncabezadoSeccion
import pe.upeu.resi.resiup.presentation.components.CuadriculaModulos
import pe.upeu.resi.resiup.presentation.components.AvatarInicial
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
import pe.upeu.resi.resiup.presentation.util.unDecimal
import pe.upeu.resi.resiup.domain.model.ResumenResidente
import pe.upeu.resi.resiup.presentation.util.Carga
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import pe.upeu.resi.resiup.domain.model.CRITERIOS_PERSONALES
import pe.upeu.resi.resiup.domain.model.CRITERIOS_DE_HABITACION
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                AvatarInicial(nombre, Acento.Azul)
                Column(Modifier.weight(1f)) {
                    Text("Hola, ${nombre.substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium)
                    Text("Panel del pastor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                TarjetaDato(ui.aprobados.toString(), "Permisos aprobados", ResiIconos.Permisos, Acento.Azul, Modifier.weight(1f))
                TarjetaDato(ui.enEspera.toString(), "Esperan a los padres", ResiIconos.Solicitudes, Acento.Ambar, Modifier.weight(1f))
                TarjetaDato(ui.sesiones.size.toString(), "Cultos hoy", ResiIconos.Asistencias, Acento.Verde, Modifier.weight(1f), detalle = "${ui.sesiones.sumOf { it.presentes }} presentes")
            }
            EncabezadoSeccion("Accesos")
            CuadriculaModulos(
                listOf(
                    ModuloUi("Asistencia a cultos", ResiIconos.Escanear, Acento.Verde, { onIr("escanear") }),
                    ModuloUi("Permisos", ResiIconos.Solicitudes, Acento.Azul, { onIr("solicitudes") }, insignia = ui.enEspera.takeIf { it > 0 }?.toString()),
                    ModuloUi("Limpieza", ResiIconos.Limpieza, Acento.Naranja, { onIr("limpieza") }),
                    ModuloUi("Días sin culto", ResiIconos.Calendario, Acento.Turquesa, { onIr("mas") }),
                    ModuloUi("Mi cuenta", ResiIconos.Perfil, Acento.Rosa, { onIr("mas") }),
                ),
            )
            EncabezadoSeccion("Cultos de hoy")
            ResiTarjeta(onClick = { onIr("escanear") }) {
                if (ui.sesiones.isEmpty()) Text("Aún no se abrió ningún culto. Abre uno desde «Asistencia a cultos».", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ui.sesiones.forEach { s ->
                    ElementoLista("Culto ${s.tipo.etiqueta.lowercase()}", subtitulo = "${s.presentes} presentes · corte ${s.corte.soloHoraLima()}", icono = ResiIconos.Asistencias, acento = Acento.Verde,
                        final = { ChipEstado(if (s.cerrada) "Cerrado" else "Abierto", if (s.cerrada) EstadoVisual.Neutro else EstadoVisual.Exito) })
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
    data object Tramites : RutaSolicitudes
}

private enum class FiltroPermisos(val etiqueta: String) { Aprobados("Aprobados"), EnEspera("En espera"), Todos("Todos") }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SolicitudesPreceptorScreen() {
    var ruta by remember { mutableStateOf<RutaSolicitudes>(RutaSolicitudes.Lista) }
    BackHandler(enabled = ruta != RutaSolicitudes.Lista) { ruta = RutaSolicitudes.Lista }
    when (val r = ruta) {
        RutaSolicitudes.Lista -> ListaPermisosPastor(onAbrir = { ruta = RutaSolicitudes.Revision(it) }, onTramites = { ruta = RutaSolicitudes.Tramites })
        RutaSolicitudes.Tramites -> pe.upeu.resi.resiup.presentation.tramites.RevisionTramitesScreen(onVolver = { ruta = RutaSolicitudes.Lista })
        is RutaSolicitudes.Revision -> DetallePermisoPastor(r.id) { ruta = RutaSolicitudes.Lista }
    }
}

private fun TipoPermiso.acento() = when (this) {
    TipoPermiso.Corto -> Acento.Verde
    TipoPermiso.Largo -> Acento.Azul
    TipoPermiso.Pernocte -> Acento.Violeta
}

/** El pastor solo consulta: aquí no se aprueba nada. */
@Composable
private fun ListaPermisosPastor(onAbrir: (Long) -> Unit, onTramites: () -> Unit) {
    val tramites = koinViewModel<pe.upeu.resi.resiup.presentation.tramites.RevisionTramitesViewModel>()
    val ampliaciones by tramites.ampliaciones.collectAsState()
    val justificaciones by tramites.justificaciones.collectAsState()
    val porRevisar = tramites.total(ampliaciones, justificaciones)
    LaunchedEffect(Unit) { tramites.cargar() }
    var filtro by remember { mutableStateOf(FiltroPermisos.Aprobados) }
    val aprobados = koinViewModel<PermisosAprobadosViewModel>()
    val enEspera = koinViewModel<PermisosEnEsperaViewModel>()
    val todos = koinViewModel<PermisosTodosViewModel>()
    LaunchedEffect(Unit) { aprobados.refrescar(); enEspera.refrescar(); todos.refrescar() }
    val vm = when (filtro) { FiltroPermisos.Aprobados -> aprobados; FiltroPermisos.EnEspera -> enEspera; FiltroPermisos.Todos -> todos }
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Permisos", Modifier.padding(top = Espacio.l), subtitulo = "Lista de permisos generados")
        ResiTarjeta(onClick = onTramites) {
            ElementoLista(
                "Ampliaciones y justificaciones", subtitulo = "Pedidos de más tiempo y explicaciones de tardanza", icono = ResiIconos.Solicitudes, acento = Acento.Naranja,
                final = { if (porRevisar != null && porRevisar > 0) ChipEstado("$porRevisar por revisar", EstadoVisual.Advertencia) },
            )
        }
        Segmentos(FiltroPermisos.entries, filtro, { it.etiqueta }, { filtro = it })
        ListaPaginada(estado, vm::refrescar, vm::cargarMas, "Sin permisos", "No hay permisos en esta lista.") { s ->
            ResiTarjeta(onClick = { onAbrir(s.id) }) {
                ElementoLista(
                    s.residenteNombre, subtitulo = "${s.tipo.etiqueta} · ${s.destino}\n${s.salida.formatoLima()}", icono = ResiIconos.Permisos, acento = s.tipo.acento(),
                    final = { ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual()) },
                )
            }
        }
    }
}

@Composable
private fun DetallePermisoPastor(id: Long, onVolver: () -> Unit) {
    val vm = koinViewModel<SolicitudRevisionViewModel>(key = "revision-$id", parameters = { parametersOf(id) })
    val estado by vm.estado.collectAsState()
    val accion by vm.accion.collectAsState()
    var anulando by remember { mutableStateOf(false) }
    LaunchedEffect(id) { vm.recargar() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)
        ContenidoCarga(estado, vm::cargar, Modifier.fillMaxWidth()) { s ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TituloPantalla(s.residenteNombre, subtitulo = "${s.tipo.etiqueta} · ${s.destino}")
                ChipEstado(s.estado.etiqueta, s.estado.aEstadoVisual())
                ResiTarjeta { DatosSolicitud(s) }
                ResumenDelResidente(vm.resumen.collectAsState().value)
                when (s.estado) {
                    EstadoSolicitud.PendienteApoderado -> {
                        BannerMensaje("Falta que el padre/madre acepte el permiso (desde su cuenta o con el enlace de un solo uso).", EstadoVisual.Info)
                        ResiBoton("Generar enlace para el padre/madre", vm::generarEnlace, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario, cargando = accion.trabajando)
                        accion.enlace?.let { e ->
                            ResiTarjeta {
                                Text("Enlace para ${e.apoderado.ifBlank { "el apoderado" }}", style = MaterialTheme.typography.titleMedium)
                                Text("Vence ${e.expiraEn.formatoLima()}; sólo sirve una vez.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(e.ruta, Modifier.padding(vertical = Espacio.s), style = MaterialTheme.typography.bodyMedium)
                                Text("Agrega la dirección del servidor delante de esta ruta al enviarlo.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    EstadoSolicitud.Aprobado -> {
                        BannerMensaje("Aprobado: garita lo verá como autorizado en el horario indicado.", EstadoVisual.Exito)
                        ResiBoton("Anular permiso", { anulando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo, cargando = accion.trabajando)
                    }
                    EstadoSolicitud.Anulado -> BannerMensaje(
                        "Anulado por el pastor" + (s.comentarioDecision?.let { ": $it" } ?: "") + ". Garita ya no lo autoriza.", EstadoVisual.Error,
                    )
                    else -> {}
                }
                accion.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
                accion.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
            }
        }
    }
    if (anulando) {
        pe.upeu.resi.resiup.presentation.components.DialogoMotivo(
            titulo = "Anular permiso",
            mensaje = "Garita ya no dejará salir al residente con este permiso. Escribe el motivo; lo verá el residente.",
            etiqueta = "Motivo de la anulación",
            textoConfirmar = "Anular",
            onConfirmar = { anulando = false; vm.anular(it) },
            onCancelar = { anulando = false },
            destructivo = true,
        )
    }
}

/** Cultos y limpieza de los últimos 30 días; es informativo: el pastor decide. */
@Composable
private fun ResumenDelResidente(estado: Carga<ResumenResidente>) {
    ResiTarjeta {
        Text("Últimos 30 días", style = MaterialTheme.typography.titleMedium)
        when (estado) {
            is Carga.Cargando -> Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
            is Carga.Fallo -> Text("No se pudo cargar el resumen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is Carga.Exito -> {
                val r = estado.datos
                Text(
                    "Cultos: asistió a ${r.asistencias} de ${r.cultosRealizados}" + if (r.despuesDelCorte > 0) " (${r.despuesDelCorte} después del corte)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    r.promedioLimpieza?.let { "Limpieza: promedio ${it.unDecimal()}/20 en ${r.evaluaciones} ${if (r.evaluaciones == 1L) "evaluación" else "evaluaciones"}" } ?: "Limpieza: sin evaluaciones",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

// ================================================================= preceptor: limpieza

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LimpiezaPreceptorScreen(vm: LimpiezaPreceptorViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    val hab = s.habitacion
    BackHandler(enabled = hab != null) { vm.cerrar() }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            if (hab == null) "Evaluación de limpieza" else "Habitación ${hab.numero}", Modifier.padding(top = Espacio.l),
            subtitulo = if (hab == null) "Hoy, ${hoyLima().toString().fechaLegible()} · elige una habitación" else "Hoy, ${hoyLima().toString().fechaLegible()}",
        )
        s.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        val finDeSemana = hoyLima().dayOfWeek.let { it == kotlinx.datetime.DayOfWeek.SATURDAY || it == kotlinx.datetime.DayOfWeek.SUNDAY }
        if (hab == null && finDeSemana) {
            pe.upeu.resi.resiup.presentation.components.EstadoVacio("Hoy no se evalúa", "La limpieza se revisa de lunes a viernes. Vuelve el lunes para evaluar las habitaciones.")
        } else if (hab == null) {
            ContenidoCarga(s.habitaciones, vm::cargar) { lista ->
                if (lista.isEmpty()) {
                    pe.upeu.resi.resiup.presentation.components.EstadoVacio("Sin habitaciones ocupadas", "Asigna residentes a las habitaciones desde administración para poder evaluarlas.")
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                        lista.groupBy { it.numero.take(1) }.forEach { (piso, habitaciones) ->
                            Text(if (piso.first().isDigit()) "Piso $piso" else "Otras", Modifier.padding(top = Espacio.s).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                            habitaciones.forEach { h ->
                                ResiTarjeta(onClick = { vm.abrir(h) }) {
                                    ElementoLista(
                                        "Habitación ${h.numero}",
                                        subtitulo = h.residentes.joinToString(", ") { it.nombreCompleto.substringBefore(' ') },
                                        icono = ResiIconos.Limpieza,
                                        final = {
                                            if (h.completa) ChipEstado("Evaluada", EstadoVisual.Exito)
                                            else ChipEstado("${h.evaluados}/${h.residentes.size}", if (h.evaluados > 0) EstadoVisual.Advertencia else EstadoVisual.Neutro)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            FormularioHabitacion(s, hab, vm)
        }
    }
}

@Composable
private fun FormularioHabitacion(s: LimpiezaFormState, hab: HabitacionLimpieza, vm: LimpiezaPreceptorViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiTarjeta {
            Text("Común a toda la habitación", style = MaterialTheme.typography.titleMedium)
            CRITERIOS_DE_HABITACION.forEach { c -> Contador(c.etiqueta, c.maximo, s.comun(c)) { vm.ajustarComun(c, it) } }
        }
        hab.residentes.forEach { r ->
            ResiTarjeta {
                val etiquetaCama = r.cama?.let { "Cama ${hab.numero}-$it · " }.orEmpty()
                ElementoLista(
                    r.nombreCompleto, subtitulo = etiquetaCama + r.codigo,
                    final = { if (r.evaluado) ChipEstado("Ya evaluado", EstadoVisual.Exito) else ChipEstado("${s.totalDe(r.residenteId)}/20", EstadoVisual.Info) },
                )
                val puntajes = s.personales[r.residenteId]
                if (puntajes != null) {
                    CRITERIOS_PERSONALES.forEach { c -> Contador(c.etiqueta, c.maximo, puntajes.getValue(c)) { vm.ajustar(r.residenteId, c, it) } }
                }
            }
        }
        s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else if (it is ErrorApi.Validacion) it.porCampo.values.firstOrNull() ?: it.aMensaje() else it.aMensaje(), EstadoVisual.Error) }
        if (s.personales.isEmpty()) {
            BannerMensaje("Todos los residentes de esta habitación ya fueron evaluados hoy.", EstadoVisual.Info)
        } else {
            ResiBoton("Guardar evaluación", vm::guardar, Modifier.fillMaxWidth(), cargando = s.enviando)
        }
        ResiBoton("Volver a las habitaciones", vm::cerrar, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario)
    }
}

/** Fila de puntaje con − / + : etiqueta y máximo a la izquierda, valor al centro. */
@Composable
private fun Contador(etiqueta: String, maximo: Int, valor: Int, onCambio: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = Espacio.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
            Text("máx. $maximo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BotonPaso("−", "Restar a $etiqueta", valor > 0) { onCambio(-1) }
        Text(
            valor.toString(), Modifier.widthIn(min = 24.dp).semantics { contentDescription = "$etiqueta: $valor de $maximo" },
            style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        BotonPaso("+", "Sumar a $etiqueta", valor < maximo) { onCambio(1) }
    }
}

/** Botón cuadrado compacto (44 dp) para sumar o restar un punto. */
@Composable
private fun BotonPaso(texto: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(12.dp)
    val color = if (habilitado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Box(
        Modifier.size(44.dp).clip(forma).border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = MaterialTheme.typography.titleMedium, color = color) }
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
