package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.platform.crearMatrizQr
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ListaPaginada
import pe.upeu.resi.resiup.presentation.components.QrVista
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TarjetaEstadistica
import pe.upeu.resi.resiup.presentation.components.TarjetaVidrio
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.aEstadoVisual
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.navigation.Destinos
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiEstilos
import pe.upeu.resi.resiup.presentation.theme.ResiTheme
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.formatoLima

@Composable
fun InicioResidenteScreen(onIr: (String) -> Unit, vm: InicioResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    ContenidoCarga(estado, vm::cargar) { ui ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TituloPantalla(
                "Hola, ${ui.perfil.nombreCompleto.substringBefore(' ')}",
                subtitulo = ui.perfil.habitacion?.let { "Habitación ${it.numero}" } ?: "Sin habitación asignada",
            )
            Box(Modifier.fillMaxWidth().background(ResiTheme.colores.marca, RoundedCornerShape(24.dp)).padding(Espacio.m)) {
                TarjetaVidrio(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(Espacio.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                        Column(Modifier.weight(1f)) {
                            Text("Mi código QR", style = MaterialTheme.typography.titleMedium)
                            Text("Para culto y garita", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ResiBoton("Mostrar", { onIr(Destinos.Perfil) })
                    }
                }
            }
            ResiTarjeta(onClick = { onIr(Destinos.Permisos) }) {
                Text("Permiso reciente", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val p = ui.permisoReciente
                if (p == null) {
                    Text("Aún no has solicitado permisos", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = Espacio.xs))
                } else {
                    ElementoLista(p.destino, subtitulo = "${p.salida.formatoLima()} → ${p.retornoAutorizado.formatoLima()}", final = { ChipEstado(p.estado.etiqueta, p.estado.aEstadoVisual()) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Box(Modifier.weight(1f)) {
                    ResiTarjeta(onClick = { onIr(Destinos.Limpieza) }) {
                        Text("Limpieza", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ui.limpieza?.promedio?.let { "$it/20" } ?: "—", style = ResiEstilos.Numero)
                        Text(if (ui.limpieza?.evaluaciones == 0L || ui.limpieza == null) "Sin evaluaciones" else "promedio", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.weight(1f)) {
                    ResiTarjeta(onClick = { onIr(Destinos.Asistencias) }) {
                        Text("Último culto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ui.ultimaAsistencia?.tipo?.etiqueta ?: "—", style = MaterialTheme.typography.titleLarge)
                        Text(ui.ultimaAsistencia?.fecha?.fechaLegible() ?: "Sin registros", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PerfilResidenteScreen(sesion: Sesion?, onCerrarSesion: () -> Unit, vm: PerfilViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    var confirmar by remember { mutableStateOf(false) }
    ContenidoCarga(estado, vm::cargar) { ui ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TituloPantalla("Perfil")
            ResiTarjeta {
                ElementoLista(ui.perfil.nombreCompleto, subtitulo = "Código ${ui.perfil.codigo}", icono = ResiIconos.Perfil, final = { ChipEstado(Rol.Residente.etiqueta, EstadoVisual.Info) })
                Text(
                    ui.perfil.habitacion?.let { "Habitación ${it.numero}" } ?: "Sin habitación asignada",
                    Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.bodyLarge,
                )
                ui.perfil.telefono?.let { Text("Teléfono $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            ResiTarjeta {
                Text("Mi código QR", style = MaterialTheme.typography.titleMedium)
                val matriz = ui.qr?.let { crearMatrizQr(it.contenido) }
                when {
                    matriz != null -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = Espacio.m), contentAlignment = Alignment.Center) { QrVista(matriz, Modifier.widthIn(max = 280.dp).fillMaxWidth()) }
                        Text(
                            "Preséntalo al preceptor o en garita. Es personal: no lo compartas ni lo publiques. Si lo perdiste, pide que lo regeneren.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ui.errorQr != null -> BannerMensaje(ui.errorQr.aMensaje(), EstadoVisual.Error, Modifier.padding(top = Espacio.s))
                    else -> BannerMensaje("No se pudo generar el QR en este dispositivo.", EstadoVisual.Advertencia, Modifier.padding(top = Espacio.s))
                }
            }
            sesion?.let { Text("Sesión: ${it.usuario}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            ResiBoton("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
        }
    }
    if (confirmar) {
        DialogoConfirmacion("Cerrar sesión", "¿Deseas salir de ResiUP en este dispositivo?", "Cerrar sesión", { confirmar = false; onCerrarSesion() }, { confirmar = false }, destructivo = true)
    }
}

@Composable
fun AsistenciasScreen(vm: AsistenciasViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l)) {
        TituloPantalla("Asistencias", Modifier.padding(vertical = Espacio.l), subtitulo = "Cultos de lunes a viernes")
        ListaPaginada(estado, vm::refrescar, vm::cargarMas, "Sin asistencias", "Cuando el preceptor registre tu asistencia a un culto aparecerá aquí.") { a ->
            ResiTarjeta { AsistenciaItem(a) }
        }
    }
}

@Composable
private fun AsistenciaItem(a: AsistenciaCulto) {
    ElementoLista(
        "Culto ${a.tipo.etiqueta.lowercase()}", subtitulo = "${a.fecha.fechaLegible()} · registrado ${a.registradoEn.formatoLima()}", icono = ResiIconos.Asistencias,
        final = { if (a.despuesDelCorte) ChipEstado("Después del corte", EstadoVisual.Advertencia) else ChipEstado("Registrada", EstadoVisual.Exito) },
    )
}

@Composable
fun LimpiezaResidenteScreen(vm: LimpiezaResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    ContenidoCarga(estado, vm::cargar) { ui ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TituloPantalla("Limpieza", subtitulo = "Evaluación individual sobre 20 puntos")
            if (ui.evaluaciones.isEmpty()) {
                ResiTarjeta { Text("Todavía no tienes evaluaciones. Aparecerán aquí cuando el preceptor las registre.", style = MaterialTheme.typography.bodyLarge) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    TarjetaEstadistica("Promedio", ui.resumen.promedio?.toString() ?: "—", Modifier.weight(1f), "de 20")
                    TarjetaEstadistica("Evaluaciones", ui.resumen.evaluaciones.toString(), Modifier.weight(1f), ui.resumen.ultimaFecha?.let { "última ${it.fechaLegible()}" })
                }
                if (ui.evaluaciones.size >= 2) {
                    ResiTarjeta {
                        Text("Evolución", style = MaterialTheme.typography.titleMedium)
                        BarrasEvolucion(ui.evaluaciones.take(10).reversed().map { it.total }, maximo = ui.evaluaciones.first().maximo)
                    }
                }
                ui.evaluaciones.forEach { e ->
                    ResiTarjeta {
                        ElementoLista("${e.total}/${e.maximo}", subtitulo = e.fecha.fechaLegible(), icono = ResiIconos.Limpieza)
                        Text(
                            e.puntajes.entries.joinToString(" · ") { "${it.key.etiqueta} ${it.value}/${it.key.maximo}" },
                            Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        e.observacion?.let { Text("“$it”", Modifier.padding(top = Espacio.xs), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarrasEvolucion(valores: List<Int>, maximo: Int) {
    Row(Modifier.fillMaxWidth().height(110.dp).padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        valores.forEach { v ->
            Box(
                Modifier.weight(1f).height((100 * v / maximo.toFloat()).coerceAtLeast(4f).dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
            )
        }
    }
}
