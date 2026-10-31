package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

@Composable
fun IndicadorCarga(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    icono: ImageVector = ResiIconos.Vacio,
    accion: (@Composable () -> Unit)? = null,
) = EstadoCentrado(icono, titulo, mensaje, MaterialTheme.colorScheme.onSurfaceVariant, modifier, accion)

@Composable
fun EstadoError(
    mensaje: String,
    onReintentar: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titulo: String = "No se pudo completar la acción",
) = EstadoCentrado(
    ResiIconos.Alerta, titulo, mensaje, MaterialTheme.colorScheme.error, modifier,
    if (onReintentar != null) {
        { ResiBoton("Reintentar", onReintentar, variante = VarianteBoton.Secundario) }
    } else null,
)

@Composable
private fun EstadoCentrado(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    colorIcono: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    accion: (@Composable () -> Unit)?,
) {
    Box(modifier.fillMaxSize().padding(Espacio.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            ResiIcono(icono, null, tint = colorIcono, tamano = 40.dp)
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (accion != null) accion()
        }
    }
}

/** Aviso persistente: sin conexión, sólo consulta de datos locales. */
@Composable
fun BannerSinConexion(modifier: Modifier = Modifier, mensaje: String = "Sin conexión. Se muestran los últimos datos guardados.") {
    val c = ResiTheme.colores
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = c.advertenciaContenedor) {
        Row(Modifier.padding(Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            ResiIcono(ResiIconos.SinConexion, null, tint = c.advertencia, tamano = 20.dp)
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = c.advertencia)
        }
    }
}

@Composable
fun BannerMensaje(mensaje: String, estado: EstadoVisual, modifier: Modifier = Modifier) {
    val c = ResiTheme.colores
    val e = MaterialTheme.colorScheme
    val (fondo, texto) = when (estado) {
        EstadoVisual.Exito -> c.exitoContenedor to c.exito
        EstadoVisual.Advertencia -> c.advertenciaContenedor to c.advertencia
        EstadoVisual.Error -> e.errorContainer to e.onErrorContainer
        else -> c.infoContenedor to c.info
    }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = fondo) {
        Text(mensaje, Modifier.padding(Espacio.m), style = MaterialTheme.typography.bodyMedium, color = texto)
    }
}
