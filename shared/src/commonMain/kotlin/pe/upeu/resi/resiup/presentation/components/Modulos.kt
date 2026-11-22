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
import androidx.compose.ui.unit.sp
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiEstilos
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

/** Un acceso de la cuadrícula de inicio: ícono sobre fondo de color y una etiqueta corta debajo. */
data class ModuloUi(val titulo: String, val icono: ImageVector, val acento: Acento, val onClick: () -> Unit, val insignia: String? = null, val deshabilitado: Boolean = false)

@Composable
fun TileModulo(modulo: ModuloUi, modifier: Modifier = Modifier) {
    val descripcion = modulo.titulo + (modulo.insignia?.let { ", $it" } ?: "")
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier
            .sombraSuave(forma)
            .clip(forma)
            .background(if (modulo.deshabilitado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable(enabled = !modulo.deshabilitado, role = Role.Button, onClick = modulo.onClick)
            .semantics { contentDescription = descripcion }
            .padding(horizontal = Espacio.m, vertical = Espacio.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Box(Modifier.size(57.dp).background(if (modulo.deshabilitado) MaterialTheme.colorScheme.outlineVariant else modulo.acento.suave(), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            ResiIcono(modulo.icono, null, tint = if (modulo.deshabilitado) MaterialTheme.colorScheme.onSurfaceVariant else modulo.acento.color(), tamano = 30.dp)
            if (modulo.insignia != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(2.dp).heightIn(min = 20.dp).background(MaterialTheme.colorScheme.error, CircleShape).padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(modulo.insignia, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            modulo.titulo, Modifier.clearAndSetSemantics { }, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (modulo.deshabilitado) MaterialTheme.colorScheme.onSurfaceVariant else modulo.acento.color(),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Cuadrícula de accesos sin desplazamiento propio (va dentro de una pantalla que ya se desplaza). */
@Composable
fun CuadriculaModulos(modulos: List<ModuloUi>, modifier: Modifier = Modifier, columnas: Int = 2) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        modulos.chunked(columnas).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                // Un acceso solo en su fila va centrado, como en los prototipos.
                if (fila.size == 1 && columnas == 2) Spacer(Modifier.weight(0.5f))
                fila.forEach { TileModulo(it, Modifier.weight(1f)) }
                if (fila.size == 1 && columnas == 2) Spacer(Modifier.weight(0.5f)) else repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
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

/** Tarjeta de cifra del inicio: ícono y título arriba, número grande al centro y una frase debajo (rankings, cultos de hoy, permisos aprobados). */
@Composable
fun TarjetaCifra(titulo: String, valor: String, detalle: String, icono: ImageVector, acento: Acento, descripcion: String, modifier: Modifier = Modifier) {
    Column(
        modifier.heightIn(min = 134.dp).clip(RoundedCornerShape(22.dp)).background(acento.suave()).semantics(mergeDescendants = true) { contentDescription = descripcion }.padding(horizontal = Espacio.s, vertical = Espacio.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).background(acento.color(), CircleShape), contentAlignment = Alignment.Center) {
                ResiIcono(icono, null, tint = Color.White, tamano = 18.dp)
            }
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = acento.color(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(valor, style = ResiEstilos.NumeroGrande.copy(fontSize = 52.sp, lineHeight = 58.sp), color = acento.color())
        Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Tarjeta de ranking del inicio: puesto grande ("1°") y "de N residentes"; sin datos muestra un guion. */
@Composable
fun TarjetaRanking(titulo: String, posicion: Int?, total: Int, icono: ImageVector, acento: Acento, modifier: Modifier = Modifier) {
    val descripcion = if (posicion == null) "$titulo: sin datos todavía" else "$titulo: puesto $posicion de $total residentes"
    Column(
        modifier.heightIn(min = 134.dp).clip(RoundedCornerShape(22.dp)).background(acento.suave()).semantics(mergeDescendants = true) { contentDescription = descripcion }.padding(horizontal = Espacio.s, vertical = Espacio.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.xs, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).background(acento.color(), CircleShape), contentAlignment = Alignment.Center) {
                ResiIcono(icono, null, tint = Color.White, tamano = 18.dp)
            }
            Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = acento.color(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(if (posicion == null) "—" else "$posicion°", style = ResiEstilos.NumeroGrande.copy(fontSize = 52.sp, lineHeight = 58.sp), color = acento.color())
        Text(if (posicion == null) "sin datos aún" else "de $total residentes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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

/** Tarjeta de bienvenida del inicio: al tocarla se abren los datos personales; [onQr] muestra el código QR (solo residentes). */
@Composable
fun TarjetaHola(nombre: String, subtitulo: String, acento: Acento, onPerfil: () -> Unit, modifier: Modifier = Modifier, onQr: (() -> Unit)? = null, foto: androidx.compose.ui.graphics.ImageBitmap? = null) {
    val c = ResiTheme.colores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.marca)
            .clickable(onClickLabel = "Ver mis datos", role = Role.Button, onClick = onPerfil).padding(Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        if (foto != null) AvatarInicial(nombre, acento, tamano = 50.dp, foto = foto) else AvatarSilueta(50.dp)
        Column(Modifier.weight(1f)) {
            Text("Hola, ${nombre.substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onQr != null) {
            Box(
                Modifier.size(55.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClickLabel = "Mostrar mi código QR", role = Role.Button, onClick = onQr),
                contentAlignment = Alignment.Center,
            ) { ResiIcono(ResiIconos.Qr, null, tint = Color.White, tamano = 30.dp) }
        }
    }
}

/** Silueta gris de persona (foto por defecto, como en los prototipos). */
@Composable
fun AvatarSilueta(tamano: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.size(tamano).clip(CircleShape).clearAndSetSemantics { }) {
        val w = size.width
        drawRect(Color(0xFFEEF1F6))
        val gris = Color(0xFF7D8696)
        drawCircle(gris, radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(w / 2, w * 0.4f))
        drawOval(gris, topLeft = androidx.compose.ui.geometry.Offset(w * 0.12f, w * 0.66f), size = androidx.compose.ui.geometry.Size(w * 0.76f, w * 0.7f))
    }
}

/** Círculo con la inicial del nombre. */
@Composable
fun AvatarInicial(nombre: String, acento: Acento, modifier: Modifier = Modifier, tamano: androidx.compose.ui.unit.Dp = 48.dp, foto: androidx.compose.ui.graphics.ImageBitmap? = null) {
    Box(modifier.size(tamano).clip(CircleShape).background(acento.color(), CircleShape).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (foto != null) {
            androidx.compose.foundation.Image(foto, null, Modifier.size(tamano), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            Text(nombre.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

@Composable
fun EncabezadoSeccion(titulo: String, modifier: Modifier = Modifier) {
    Text(titulo, modifier.padding(top = Espacio.s).semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = Acento.Azul.color())
}

@Composable
fun EspacioVertical(alto: androidx.compose.ui.unit.Dp = Espacio.s) = Spacer(Modifier.height(alto))
