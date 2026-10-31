package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiTheme
import pe.upeu.resi.resiup.presentation.util.DiaCalendario
import pe.upeu.resi.resiup.presentation.util.NOMBRES_MES
import pe.upeu.resi.resiup.presentation.util.armarMes
import pe.upeu.resi.resiup.presentation.util.fechaMasAntigua
import pe.upeu.resi.resiup.presentation.util.formatoLima
import pe.upeu.resi.resiup.presentation.util.legible
import pe.upeu.resi.resiup.presentation.util.mesDesplazado

private val INICIALES = listOf("L", "M", "M", "J", "V", "S", "D")
private val NOMBRES_DIA = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

/**
 * Calendario mensual de asistencias a cultos. Cada día se distingue por una marca además del color
 * («✓» asistió, «!» después del corte, «–» sin registro) y tiene descripción para lectores de pantalla.
 */
@Composable
fun CalendarioAsistencias(asistencias: List<AsistenciaCulto>, hoy: LocalDate, modifier: Modifier = Modifier) {
    var anio by remember { mutableStateOf(hoy.year) }
    var mes by remember { mutableStateOf(hoy.month.ordinal + 1) }
    var seleccion by remember { mutableStateOf<LocalDate?>(null) }
    val desde = remember(asistencias) { asistencias.fechaMasAntigua() }
    val semanas = remember(anio, mes, asistencias, hoy, desde) { armarMes(anio, mes, asistencias, hoy, desde) }
    val esMesActual = anio == hoy.year && mes == hoy.month.ordinal + 1

    ResiTarjeta(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton({ mesDesplazado(anio, mes, -1).let { anio = it.first; mes = it.second; seleccion = null } }, Modifier.semantics { contentDescription = "Mes anterior" }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            Text(
                "${NOMBRES_MES[mes - 1].replaceFirstChar { it.uppercase() }} $anio",
                Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                { mesDesplazado(anio, mes, 1).let { anio = it.first; mes = it.second; seleccion = null } },
                Modifier.semantics { contentDescription = "Mes siguiente" }, enabled = !esMesActual,
            ) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
        Row(Modifier.fillMaxWidth().padding(top = Espacio.xs).clearAndSetSemantics { }) {
            INICIALES.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        semanas.forEach { semana ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                semana.forEach { dia ->
                    CeldaDia(dia, dia.fecha == seleccion, Modifier.weight(1f)) { seleccion = if (dia.fecha == seleccion) null else dia.fecha }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Leyenda("✓", "Asistió")
            Leyenda("!", "Tras el corte")
            Leyenda("–", "Sin registro")
        }
        seleccion?.let { fecha ->
            val dia = semanas.flatten().firstOrNull { it.fecha == fecha }
            if (dia != null) DetalleDia(dia)
        }
    }
}

@Composable
private fun Leyenda(marca: String, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(marca, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DiaCalendario.descripcion(): String {
    val base = "${NOMBRES_DIA[fecha.dayOfWeek.ordinal]} ${fecha.legible()}"
    return when {
        asistencias.isNotEmpty() -> base + ": asistió a " + asistencias.joinToString(" y ") { "culto ${it.tipo.etiqueta.lowercase()}" + if (it.despuesDelCorte) " después del corte" else "" }
        sinRegistro -> "$base: sin registro de asistencia"
        !habil -> "$base: sin culto"
        else -> base
    }
}

@Composable
private fun CeldaDia(dia: DiaCalendario, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colores = ResiTheme.colores
    val tarde = dia.asistencias.any { it.despuesDelCorte }
    val fondo = when {
        !dia.delMes -> androidx.compose.ui.graphics.Color.Transparent
        dia.asistencias.isEmpty() -> androidx.compose.ui.graphics.Color.Transparent
        tarde -> colores.advertenciaContenedor
        else -> colores.exitoContenedor
    }
    val marca = when {
        !dia.delMes -> ""
        dia.asistencias.isNotEmpty() -> if (tarde) "!" else "✓"
        dia.sinRegistro -> "–"
        else -> ""
    }
    val borde = when {
        seleccionado -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        dia.esHoy -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else -> null
    }
    val forma = RoundedCornerShape(12.dp)
    var celda = modifier.aspectRatio(1f).background(fondo, forma)
    if (borde != null) celda = celda.border(borde, forma)
    if (dia.delMes) {
        celda = celda.clickable(onClickLabel = "Ver detalle del día") { onClick() }
            .semantics { contentDescription = dia.descripcion(); role = Role.Button; selected = seleccionado }
    } else {
        celda = celda.clearAndSetSemantics { }
    }
    Box(celda, contentAlignment = Alignment.Center) {
        if (dia.delMes) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${dia.fecha.day}", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (dia.esHoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (dia.habil) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(marca, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun DetalleDia(dia: DiaCalendario) {
    Column(Modifier.fillMaxWidth().padding(top = Espacio.m), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${NOMBRES_DIA[dia.fecha.dayOfWeek.ordinal].replaceFirstChar { it.uppercase() }} ${dia.fecha.legible()}", style = MaterialTheme.typography.titleSmall)
        when {
            dia.asistencias.isNotEmpty() -> dia.asistencias.forEach {
                Text(
                    "Culto ${it.tipo.etiqueta.lowercase()} · registrado ${it.registradoEn.formatoLima()}" + if (it.despuesDelCorte) " (después del corte)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            !dia.habil -> Text("No hay culto en fin de semana.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            dia.sinRegistro -> Text("Sin registro de asistencia. Puede deberse a un permiso, feriado o a que no se escaneó el QR.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Text("Todavía no hay registro para este día.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
