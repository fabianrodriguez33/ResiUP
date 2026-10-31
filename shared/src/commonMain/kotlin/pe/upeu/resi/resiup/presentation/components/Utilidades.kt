package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import pe.upeu.resi.resiup.domain.model.EstadoSolicitud
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.combinarFechaHora
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

/** Selector segmentado simple (2-3 opciones). */
@Composable
fun <T> Segmentos(opciones: List<T>, seleccionada: T, etiqueta: (T) -> String, onSeleccion: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        opciones.forEach { o ->
            val activa = o == seleccionada
            Text(
                etiqueta(o),
                Modifier.weight(1f).heightIn(min = 44.dp)
                    .background(if (activa) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(11.dp))
                    .clickable(role = Role.Tab) { onSeleccion(o) }
                    .padding(vertical = 11.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (activa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Botón que abre selector de fecha y luego de hora; entrega un instante en hora de Lima. */
@Composable
fun CampoFechaHora(etiqueta: String, valor: Instant?, error: String?, onElegido: (Instant) -> Unit, modifier: Modifier = Modifier) {
    var paso by remember { mutableStateOf(0) }
    var fechaMillis by remember { mutableLongStateOf(0L) }
    androidx.compose.foundation.layout.Column(modifier) {
        ResiBoton(
            texto = "$etiqueta: ${valor?.formatoLima() ?: "elegir fecha y hora"}",
            onClick = { paso = 1 },
            modifier = Modifier.fillMaxWidth(),
            variante = VarianteBoton.Secundario,
        )
        if (error != null) Text(error, Modifier.padding(start = Espacio.l, top = Espacio.xs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
    if (paso == 1) SelectorFecha({ fechaMillis = it; paso = 2 }, { paso = 0 })
    if (paso == 2) SelectorHora({ h, m -> onElegido(combinarFechaHora(fechaMillis, h, m)); paso = 0 }, { paso = 0 })
}

fun EstadoSolicitud.aEstadoVisual(): EstadoVisual = when (this) {
    EstadoSolicitud.Aprobado -> EstadoVisual.Exito
    EstadoSolicitud.PendienteApoderado, EstadoSolicitud.PendientePreceptor, EstadoSolicitud.Borrador -> EstadoVisual.Advertencia
    EstadoSolicitud.Rechazado -> EstadoVisual.Error
    EstadoSolicitud.Cancelado, EstadoSolicitud.Vencido -> EstadoVisual.Neutro
}
