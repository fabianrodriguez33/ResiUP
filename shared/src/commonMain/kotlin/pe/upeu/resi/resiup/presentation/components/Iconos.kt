package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Iconos de trazo propios (24x24), uniformes en toda la app. */
private fun trazo(nombre: String, datos: String): ImageVector =
    ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(datos).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

object ResiIconos {
    val Inicio = trazo("inicio", "M4 11 L12 4 L20 11 M6 10 V20 H18 V10 M10 20 V14 H14 V20")
    val Permisos = trazo("permisos", "M7 3 H14 L18 7 V21 H7 Z M14 3 V7 H18 M10 12 H15 M10 16 H15")
    val Asistencias = trazo("asistencias", "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 M8 12.5 L11 15.5 L16.5 9")
    val Limpieza = trazo("limpieza", "M12 3 L13.8 9.2 L20 11 L13.8 12.8 L12 19 L10.2 12.8 L4 11 L10.2 9.2 Z")
    val Perfil = trazo("perfil", "M12 4 A4 4 0 1 0 12 12 A4 4 0 1 0 12 4 M4 21 C4 16 8 14 12 14 C16 14 20 16 20 21")
    val Escanear = trazo("escanear", "M4 8 V4 H8 M16 4 H20 V8 M20 16 V20 H16 M8 20 H4 V16 M4 12 H20")
    val Mas = trazo("mas", "M5 12 H5.01 M12 12 H12.01 M19 12 H19.01")
    val Solicitudes = trazo("solicitudes", "M8 6 H20 M8 12 H20 M8 18 H20 M4 6 H4.01 M4 12 H4.01 M4 18 H4.01")
    val Historial = trazo("historial", "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 M12 7 V12 L15 14")
    val Salir = trazo("salir", "M14 4 H19 V20 H14 M4 12 H15 M11 8 L15 12 L11 16")
    val Alerta = trazo("alerta", "M12 3 L22 20 H2 Z M12 10 V14 M12 17 H12.01")
    val SinConexion = trazo("sin_conexion", "M3 3 L21 21 M5 10 C7 8.5 9.5 7.5 12 7.5 M8.5 14 C9.5 13 10.7 12.5 12 12.5 M12 17 H12.01")
    val Racha = trazo("racha", "M12 3 C12.5 6 17 8 17 13 A5 5 0 0 1 7 13 C7 10.5 8.5 9 10 7.5 C10.5 9 11 10 12 10 C12.5 7.5 12 5 12 3 Z")
    val Editar = trazo("editar", "M4 20 L4 16 L15 5 L19 9 L8 20 Z M13 7 L17 11")
    val Calendario = trazo("calendario", "M4 6 H20 V20 H4 Z M4 10 H20 M8 3 V7 M16 3 V7")
    val Qr = trazo("qr", "M4 4 H10 V10 H4 Z M14 4 H20 V10 H14 Z M4 14 H10 V20 H4 Z M14 14 H17 V17 M20 14 V14.01 M14 20 H14.01 M17 20 H20 V17")
    val Campana = trazo("campana", "M6 16 V11 A6 6 0 0 1 18 11 V16 L20 18 H4 Z M10 21 H14")
    val Correo = trazo("correo", "M4 6 H20 V18 H4 Z M4 7 L12 13 L20 7")
    val Telefono = trazo("telefono", "M8 3 H16 V21 H8 Z M11 18 H13")
    val Vacio = trazo("vacio", "M4 7 H20 V20 H4 Z M4 7 L7 3 H17 L20 7 M9 12 H15")
}

@Composable
fun ResiIcono(
    icono: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    tamano: Dp = 24.dp,
) {
    val color = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint
    Image(
        imageVector = icono,
        contentDescription = contentDescription,
        modifier = modifier.size(tamano),
        colorFilter = ColorFilter.tint(color),
    )
}

/** Isotipo propio de ResiUP (casa con puerta). No es el escudo oficial de UPeU. */
val IsotipoResi: ImageVector =
    ImageVector.Builder("isotipo", 96.dp, 96.dp, 96f, 96f).apply {
        path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
            moveTo(48f, 14f)
            lineTo(84f, 46f)
            lineTo(74f, 46f)
            lineTo(74f, 80f)
            lineTo(22f, 80f)
            lineTo(22f, 46f)
            lineTo(12f, 46f)
            close()
            moveTo(40f, 80f)
            lineTo(40f, 58f)
            lineTo(56f, 58f)
            lineTo(56f, 80f)
            close()
        }
    }.build()
