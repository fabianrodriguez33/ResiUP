package pe.upeu.resi.resiup.presentation.personal

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import pe.upeu.resi.resiup.domain.model.HabitacionLimpieza
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CalendarioAsistencias
import pe.upeu.resi.resiup.presentation.components.CampoDesplegable
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.FranjaSemana
import pe.upeu.resi.resiup.presentation.components.IconoAccion
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.components.LocalVolver
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.hoyLima

private sealed interface RutaLimpieza {
    data object Inicio : RutaLimpieza
    data object Habitaciones : RutaLimpieza
}

private val PISOS = listOf("1", "2", "3", "4")

private fun CriterioLimpieza.nombre() = when (this) {
    CriterioLimpieza.Cama -> "CAMA"; CriterioLimpieza.Ropero -> "ROPERO"; CriterioLimpieza.Escritorio -> "ESCRITORIO"
    CriterioLimpieza.Basura -> "BASURA"; CriterioLimpieza.Olores -> "OLORES"; CriterioLimpieza.Piso -> "PISO"
}

private fun CriterioLimpieza.icono() = when (this) {
    CriterioLimpieza.Cama -> ResiIconos.Cama; CriterioLimpieza.Ropero -> ResiIconos.Ropero; CriterioLimpieza.Escritorio -> ResiIconos.Escritorio
    CriterioLimpieza.Basura -> ResiIconos.Basura; CriterioLimpieza.Olores -> ResiIconos.Olores; CriterioLimpieza.Piso -> ResiIconos.Piso
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LimpiezaPreceptorScreen(vm: LimpiezaPreceptorViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    var ruta by remember { mutableStateOf<RutaLimpieza>(RutaLimpieza.Inicio) }
    val hab = s.habitacion
    // Atrás: habitación -> habitaciones -> inicio de limpieza -> inicio de la app.
    BackHandler(enabled = hab != null || ruta != RutaLimpieza.Inicio) { if (hab != null) vm.cerrar() else ruta = RutaLimpieza.Inicio }
    val alInicio = LocalVolver.current
    val volver: (() -> Unit)? = if (hab != null) ({ vm.cerrar() }) else if (ruta != RutaLimpieza.Inicio) ({ ruta = RutaLimpieza.Inicio }) else alInicio
    CompositionLocalProvider(LocalVolver provides volver) {
        when {
            hab != null -> Habitacion(s, hab, vm)
            ruta == RutaLimpieza.Habitaciones -> Habitaciones(s, vm)
            else -> InicioLimpieza(s, vm, onCrear = { ruta = RutaLimpieza.Habitaciones })
        }
    }
}

@Composable
private fun InicioLimpieza(s: LimpiezaFormState, vm: LimpiezaPreceptorViewModel, onCrear: () -> Unit) {
    val hoy = remember { hoyLima() }
    var calendario by remember { mutableStateOf(false) }
    val habilesHoy = s.fecha == hoy && s.fecha.dayOfWeek.ordinal < 5
    var eliminandoDia by remember { mutableStateOf(false) }
    val accionesDia = org.koin.compose.koinInject<AccionesPastorApi>()
    val alcanceDia = androidx.compose.runtime.rememberCoroutineScope()
    if (eliminandoDia) {
        pe.upeu.resi.resiup.presentation.components.DialogoMotivo(
            "Eliminar toda la evaluación", "Se eliminan las evaluaciones de todas las habitaciones de ${s.fecha.toString().fechaLegible()}. Queda anotado quién y por qué.", "Motivo", "Eliminar",
            { m -> eliminandoDia = false; alcanceDia.launch { pe.upeu.resi.resiup.data.remote.ejecutarLlamada { accionesDia.eliminarLimpiezaDelDia(s.fecha.toString(), m) }.onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Evaluación eliminada exitosamente"); vm.cargar() } } },
            { eliminandoDia = false }, destructivo = true,
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            "Limpieza", Modifier.padding(top = Espacio.l), subtitulo = "Evaluación individual sobre 20 puntos",
            accion = { IconoAccion(ResiIconos.Calendario, "Abrir calendario", { calendario = true }) },
        )
        FranjaSemana(s.fecha, vm::cambiarFecha)
        Text("Evaluaciones del ${s.fecha.toString().fechaLegible()}", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        s.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        when (val h = s.habitaciones) {
            Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
            is Carga.Fallo -> BannerMensaje(h.error.aMensaje(), EstadoVisual.Error)
            is Carga.Exito -> if (!h.datos.any { it.evaluados > 0 }) {
                // Sin ninguna evaluación ese día: se puede crear (solo hoy y de lunes a viernes).
                ResiBoton("Crear nueva evaluación", onCrear, Modifier.fillMaxWidth(), habilitado = habilesHoy)
            } else {
                val completa = h.datos.all { it.completa || it.residentes.isEmpty() }
                ResiBoton("Eliminar toda la evaluación", { eliminandoDia = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
                ResiTarjeta(onClick = onCrear) {
                    ElementoLista(
                        "Evaluación", subtitulo = s.fecha.toString().fechaLegible(), icono = ResiIconos.Limpieza, acento = Acento.Ambar,
                        final = { ChipEstado(if (completa) "Terminada" else "En Curso", if (completa) EstadoVisual.Exito else EstadoVisual.Advertencia) },
                    )
                }
            }
        }
    }
    if (calendario) Dialog(onDismissRequest = { calendario = false }) { CalendarioAsistencias(emptyList(), hoy, onElegir = { vm.cambiarFecha(it); calendario = false }, conMarcas = false) }
}

@Composable
private fun Habitaciones(s: LimpiezaFormState, vm: LimpiezaPreceptorViewModel) {
    var piso by remember { mutableStateOf<String?>(null) }
    var eliminando by remember { mutableStateOf<HabitacionLimpieza?>(null) }
    var elegidaEvaluada by remember { mutableStateOf<HabitacionLimpieza?>(null) }
    elegidaEvaluada?.let { h ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { elegidaEvaluada = null }) {
            ResiTarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Text("Habitación ${h.numero}", style = MaterialTheme.typography.titleLarge)
                    Text("Ya fue evaluada. Puedes modificar los puntajes o eliminar la evaluación para volver a hacerla.", style = MaterialTheme.typography.bodyMedium)
                    ResiBoton("Modificar evaluación", { elegidaEvaluada = null; vm.editar(h) }, Modifier.fillMaxWidth())
                    ResiBoton("Eliminar evaluación", { elegidaEvaluada = null; eliminando = h }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
                    ResiBoton("Cerrar", { elegidaEvaluada = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                }
            }
        }
    }
    val acciones = org.koin.compose.koinInject<AccionesPastorApi>()
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    eliminando?.let { h ->
        pe.upeu.resi.resiup.presentation.components.DialogoMotivo(
            "Eliminar evaluación", "Se elimina la evaluación de la habitación ${h.numero} de ${s.fecha.toString().fechaLegible()} para volver a evaluarla. Queda anotado quién y por qué.", "Motivo", "Eliminar",
            { m -> eliminando = null; alcance.launch { pe.upeu.resi.resiup.data.remote.ejecutarLlamada { acciones.eliminarLimpieza(h.id, s.fecha.toString(), m) }.onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Evaluación eliminada exitosamente"); vm.cargar() } } },
            { eliminando = null }, destructivo = true,
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Evaluación ${s.fecha.toString().fechaLegible().substringBeforeLast(' ')}", Modifier.padding(top = Espacio.l), subtitulo = "Elige la habitación")
        // El título, la flecha y el selector de piso quedan quietos; solo las habitaciones se desplazan.
        CampoDesplegable("Elige el piso", PISOS, piso, { "$it PISO" }, { piso = it })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            when (val h = s.habitaciones) {
                Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
                is Carga.Fallo -> BannerMensaje(h.error.aMensaje(), EstadoVisual.Error)
                is Carga.Exito -> {
                    val delPiso = h.datos.filter { it.numero.take(1) == piso }
                    if (piso == null || delPiso.isEmpty()) {
                        Text("No hay habitaciones", Modifier.fillMaxWidth().padding(top = Espacio.l), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                    delPiso.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            fila.forEach { hab ->
                                ResiTarjeta(Modifier.weight(1f), onClick = { if (hab.completa) elegidaEvaluada = hab else vm.abrir(hab) }) {
                                    ElementoLista(
                                        hab.numero, subtitulo = if (hab.completa) "Evaluado" else "Falta evaluación", icono = ResiIconos.Limpieza,
                                        acento = if (hab.completa) Acento.Verde else Acento.Ambar,
                                    )
                                }
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Habitacion(s: LimpiezaFormState, hab: HabitacionLimpieza, vm: LimpiezaPreceptorViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Habitación ${hab.numero}", Modifier.padding(top = Espacio.l), subtitulo = "Elija el criterio")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            if (s.personales.isEmpty()) BannerMensaje("Todos los residentes de esta habitación ya fueron evaluados hoy.", EstadoVisual.Info)
            CampoDesplegable("Criterio", CRITERIOS_EN_MENU.filter { it !in s.registrados }, s.criterio, { it.nombre() }, vm::elegirCriterio)
            val c = s.criterio
            if (c != null) {
                if (c in CRITERIOS_POR_RESIDENTE) {
                    hab.pendientes.forEach { r ->
                        Fila(c, r.nombreCompleto, "${c.nombre().lowercase().replaceFirstChar { it.uppercase() }} ${r.cama ?: ""}".trim(), s.personales[r.residenteId]?.get(c)) { vm.ajustar(r.residenteId, c, it) }
                    }
                } else {
                    Fila(c, "Habitación", c.nombre().lowercase().replaceFirstChar { it.uppercase() }, s.comun(c)) { vm.ajustarComun(c, it) }
                }
            }
            s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else if (it is ErrorApi.Validacion) it.porCampo.values.firstOrNull() ?: it.aMensaje() else it.aMensaje(), EstadoVisual.Error) }
        }
        if (s.edicion) ResiBoton("Guardar cambios", vm::guardarCambios, Modifier.fillMaxWidth(), habilitado = !s.enviando, cargando = s.enviando)
        ResiBoton("Registrar evaluación", vm::registrarCriterio, Modifier.fillMaxWidth().padding(bottom = Espacio.m), habilitado = s.criterioCompleto && !s.enviando, cargando = s.enviando)
    }
}

/** Fila del prototipo: ícono del criterio, nombre y detalle a la izquierda; – [valor] + a la derecha ("-" mientras no hay nota). */
@Composable
private fun Fila(c: CriterioLimpieza, titulo: String, detalle: String, valor: Int?, onCambio: (Int) -> Unit) {
    ResiTarjeta {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                ResiIcono(c.icono(), null, tint = MaterialTheme.colorScheme.primary, tamano = 26.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(detalle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Paso("−", "Restar a ${c.nombre()}", (valor ?: 1) > 0) { onCambio(-1) }
            Box(
                Modifier.widthIn(min = 44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(vertical = 10.dp)
                    .semantics { contentDescription = "${c.nombre()}: ${valor ?: "sin nota"} de ${c.maximo}" },
                contentAlignment = Alignment.Center,
            ) { Text(valor?.toString() ?: "-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            Paso("+", "Sumar a ${c.nombre()}", (valor ?: 0) < c.maximo) { onCambio(1) }
        }
    }
}

@Composable
private fun Paso(texto: String, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled = habilitado, role = Role.Button, onClick = onClick).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) { Text(texto, style = MaterialTheme.typography.headlineMedium, color = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) }
}
