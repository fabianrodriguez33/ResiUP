package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiEstilos
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

/** Un acceso de la cuadrícula de inicio: ícono sobre fondo de color y una etiqueta corta debajo. */
data class ModuloUi(val titulo: String, val icono: ImageVector, val acento: Acento, val onClick: () -> Unit, val insignia: String? = null)

@Composable
fun TileModulo(modulo: ModuloUi, modifier: Modifier = Modifier) {
    val descripcion = modulo.titulo + (modulo.insignia?.let { ", $it" } ?: "")
    val forma = RoundedCornerShape(28.dp)
    Column(
        modifier
            .sombraSuave(forma)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = modulo.onClick)
            .semantics { contentDescription = descripcion }
            .padding(Espacio.l),
        verticalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(Modifier.size(56.dp).sombraSuave(RoundedCornerShape(20.dp), modulo.acento.color()).background(modulo.acento.suave(), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                ResiIcono(modulo.icono, null, tint = modulo.acento.color(), tamano = 28.dp)
                if (modulo.insignia != null) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(2.dp).heightIn(min = 20.dp).background(MaterialTheme.colorScheme.error, CircleShape).padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(modulo.insignia, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) {
                ResiIcono(ResiIconos.FlechaDiagonal, null, tint = modulo.acento.color(), tamano = 16.dp)
            }
        }
        Text(
            modulo.titulo, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2,
        )
    }
}

/** Cuadrícula de accesos sin desplazamiento propio (va dentro de una pantalla que ya se desplaza). */
@Composable
fun CuadriculaModulos(modulos: List<ModuloUi>, modifier: Modifier = Modifier, columnas: Int = 2) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        modulos.chunked(columnas).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                fila.forEach { TileModulo(it, Modifier.weight(1f)) }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Dato destacado (racha, asistencias del mes, promedio...): fondo del color del módulo y cifra grande. */
@Composable
fun TarjetaDato(valor: String, etiqueta: String, icono: ImageVector, acento: Acento, modifier: Modifier = Modifier, detalle: String? = null) {
    val descripcion = "$etiqueta: $valor" + (detalle?.let { ", $it" } ?: "")
    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(acento.suave()).semantics(mergeDescendants = true) { contentDescription = descripcion }.padding(Espacio.m),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        Box(Modifier.size(32.dp).background(acento.color(), CircleShape), contentAlignment = Alignment.Center) {
            ResiIcono(icono, null, tint = Color.White, tamano = 18.dp)
        }
        Text(valor, style = ResiEstilos.Numero, color = acento.color())
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
        if (detalle != null) Text(detalle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

/** Tarjeta principal de la pantalla de inicio, con degradado de marca y una acción. */
@Composable
fun TarjetaHero(titulo: String, subtitulo: String, textoBoton: String, onClick: () -> Unit, icono: ImageVector, modifier: Modifier = Modifier) {
    val c = ResiTheme.colores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.marca).padding(Espacio.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            ResiIcono(icono, null, tint = Color.White, tamano = 28.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(Color.White).clickable(role = Role.Button, onClick = onClick).padding(horizontal = Espacio.l),
            contentAlignment = Alignment.Center,
        ) { Text(textoBoton, style = MaterialTheme.typography.labelLarge, color = c.marca) }
    }
}

/** Círculo con la inicial del nombre. */
@Composable
fun AvatarInicial(nombre: String, acento: Acento, modifier: Modifier = Modifier, tamano: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(modifier.size(tamano).background(acento.color(), CircleShape).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Text(nombre.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

@Composable
fun EncabezadoSeccion(titulo: String, modifier: Modifier = Modifier) {
    Text(titulo, modifier.padding(top = Espacio.s).semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun EspacioVertical(alto: androidx.compose.ui.unit.Dp = Espacio.s) = Spacer(Modifier.height(alto))
