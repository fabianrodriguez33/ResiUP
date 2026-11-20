package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.NOMBRES_MES

private val DIAS_CORTOS = listOf("DOM", "LUN", "MAR", "MIE", "JUE", "VIE", "SAB")

/** Domingo de la semana que contiene [fecha]; las semanas del prototipo van de domingo a sábado. */
fun domingoDe(fecha: LocalDate): LocalDate = fecha.minus(fecha.dayOfWeek.ordinal.plus(1) % 7, DateTimeUnit.DAY)

/** Franja de la semana (domingo a sábado) con el mes arriba; el día elegido lleva un recuadro. */
@Composable
fun FranjaSemana(seleccion: LocalDate, onSeleccion: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val inicio = domingoDe(seleccion)
    ResiTarjeta(modifier) {
        Text(
            "${NOMBRES_MES[seleccion.month.ordinal].replaceFirstChar { it.uppercase() }} ${seleccion.year}",
            Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth().padding(top = Espacio.s), horizontalArrangement = Arrangement.SpaceEvenly) {
            (0 until 7).forEach { i ->
                val dia = inicio.plus(i, DateTimeUnit.DAY)
                val elegido = dia == seleccion
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .then(if (elegido) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp)) else Modifier)
                        .clickable(role = Role.Button) { onSeleccion(dia) }
                        .semantics { contentDescription = "${DIAS_CORTOS[i]} ${dia.day}"; selected = elegido }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(DIAS_CORTOS[i], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("${dia.day}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Botón de ícono de la cabecera (calendario, etc.): ícono grande del color de marca. */
@Composable
fun IconoAccion(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = descripcion, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) { ResiIcono(icono, null, tint = MaterialTheme.colorScheme.primary, tamano = 30.dp) }
}
