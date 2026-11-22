package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.combinarFechaHora
import pe.upeu.resi.resiup.presentation.util.conHoraLima
import pe.upeu.resi.resiup.presentation.util.soloHoraLima
import pe.upeu.resi.resiup.presentation.util.formatoLima

/** Dibuja la matriz de un QR en negro sobre blanco, con zona de silencio, para que sea legible por cualquier lector. */
@Composable
fun QrVista(matriz: List<List<Boolean>>, modifier: Modifier = Modifier, descripcion: String = "Código QR personal") {
    val n = matriz.size
    Canvas(
        modifier.aspectRatio(1f).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)
            .semantics { contentDescription = descripcion },
    ) {
        val lado = size.minDimension / n
        for (y in 0 until n) for (x in 0 until n) if (matriz[y][x]) {
            drawRect(Color.Black, Offset(x * lado, y * lado), Size(lado + 0.5f, lado + 0.5f))
        }
    }
}

/**
 * Selector segmentado (2-3 opciones): el fondo blanco de la opción elegida se desliza al cambiar y también se puede
 * arrastrar con el dedo de una opción a otra.
 */
@Composable
fun <T> Segmentos(opciones: List<T>, seleccionada: T, etiqueta: (T) -> String, onSeleccion: (T) -> Unit, modifier: Modifier = Modifier) {
    val indice = opciones.indexOf(seleccionada).coerceAtLeast(0)
    val posicion by androidx.compose.animation.core.animateFloatAsState(indice.toFloat(), androidx.compose.animation.core.tween(220), label = "segmento")
    val alElegir by androidx.compose.runtime.rememberUpdatedState(onSeleccion)
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(4.dp)
            .pointerInput(opciones) {
                detectHorizontalDragGestures { cambio, _ ->
                    val destino = (cambio.position.x / size.width * opciones.size).toInt().coerceIn(0, opciones.size - 1)
                    alElegir(opciones[destino])
                }
            },
    ) {
        val ancho = maxWidth / opciones.size
        Box(Modifier.offset(x = ancho * posicion).width(ancho).height(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(11.dp)))
        Row(Modifier.fillMaxWidth()) {
            opciones.forEach { o ->
                val activa = o == seleccionada
                Box(Modifier.weight(1f).height(44.dp).clickable(role = Role.Tab) { onSeleccion(o) }, contentAlignment = Alignment.Center) {
                    Text(
                        etiqueta(o),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Botón que abre selector de fecha y luego de hora; entrega un instante en hora de Lima. */
@Composable
fun CampoFechaHora(etiqueta: String, valor: Instant?, error: String?, onElegido: (Instant) -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true, desde: Instant? = null) {
    var paso by remember { mutableStateOf(0) }
    var fechaMillis by remember { mutableLongStateOf(0L) }
    androidx.compose.foundation.layout.Column(modifier) {
        PildoraSelector(valor?.formatoLima() ?: etiqueta, { paso = 1 }, Modifier.fillMaxWidth(), habilitado = habilitado)
        if (error != null) Text(error, Modifier.padding(start = Espacio.l, top = Espacio.xs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
    // No se eligen días anteriores a hoy (hora de Lima) ni, en un ingreso, anteriores a la salida.
    val primerDia = remember(desde) {
        val d = (desde ?: pe.upeu.resi.resiup.presentation.util.ahora()).toLocalDateTime(pe.upeu.resi.resiup.presentation.util.ZonaResidencia).date
        d.atStartOfDayIn(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
    }
    if (paso == 1) SelectorFecha({ fechaMillis = it; paso = 2 }, { paso = 0 }, desdeMillis = primerDia)
    if (paso == 2) SelectorHora({ h, m -> onElegido(combinarFechaHora(fechaMillis, h, m)); paso = 0 }, { paso = 0 })
}

/** Botón que abre el selector de hora; la fecha es la de [base] (la salida), en hora de Lima. */
@Composable
fun CampoHora(etiqueta: String, valor: Instant?, base: Instant?, error: String?, onElegido: (Instant) -> Unit, modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(modifier) {
        PildoraSelector(valor?.soloHoraLima() ?: etiqueta, { abierto = true }, Modifier.fillMaxWidth(), habilitado = base != null)
        if (error != null) Text(error, Modifier.padding(start = Espacio.l, top = Espacio.xs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
    if (abierto && base != null) SelectorHora({ h, m -> onElegido(base.conHoraLima(h, m)); abierto = false }, { abierto = false })
}

/** Cómo ve el residente su permiso: En revisión, En garita, Salida, Entrada... */
fun pe.upeu.resi.resiup.domain.model.SolicitudPermiso.estadoParaResidente(): Pair<String, EstadoVisual> = when (estado) {
    EstadoSolicitud.PendienteApoderado -> "Pendiente" to EstadoVisual.Advertencia
    EstadoSolicitud.PendientePreceptor, EstadoSolicitud.Borrador -> "En revisión" to EstadoVisual.Revision
    EstadoSolicitud.Aprobado -> when {
        garitaEntrada != null -> "Entrada" to EstadoVisual.Exito
        garitaSalida != null -> "Salida" to EstadoVisual.Info
        else -> "En garita" to EstadoVisual.Neutro
    }
    EstadoSolicitud.Rechazado -> "Rechazado" to EstadoVisual.Error
    EstadoSolicitud.Anulado -> "Anulado" to EstadoVisual.Error
    EstadoSolicitud.Cancelado -> "Cancelado" to EstadoVisual.Neutro
    EstadoSolicitud.Vencido -> "Vencido" to EstadoVisual.Neutro
}

/** Selector en forma de píldora (fecha, hora): azul suave; gris si todavía no se puede usar. */
@Composable
fun PildoraSelector(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Box(
        modifier.heightIn(min = 52.dp).background(if (habilitado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable(enabled = habilitado, role = Role.Button, onClick = onClick).padding(horizontal = Espacio.m, vertical = Espacio.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

fun EstadoSolicitud.aEstadoVisual(): EstadoVisual = when (this) {
    EstadoSolicitud.Aprobado -> EstadoVisual.Exito
    EstadoSolicitud.PendienteApoderado, EstadoSolicitud.PendientePreceptor, EstadoSolicitud.Borrador -> EstadoVisual.Advertencia
    EstadoSolicitud.Rechazado -> EstadoVisual.Error
    EstadoSolicitud.Anulado -> EstadoVisual.Error
    EstadoSolicitud.Cancelado, EstadoSolicitud.Vencido -> EstadoVisual.Neutro
}
