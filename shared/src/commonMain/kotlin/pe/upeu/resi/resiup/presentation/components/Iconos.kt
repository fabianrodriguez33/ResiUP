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

/** Íconos rellenos (siluetas), como los criterios de limpieza del prototipo; las partes huecas se cortan con relleno par-impar. */
private fun relleno(nombre: String, datos: String): ImageVector =
    ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString(datos).toNodes(), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd)
    }.build()

object ResiIconos {
    val Inicio = trazo("inicio", "M4 11 L12 4 L20 11 M6 10 V20 H18 V10 M10 20 V14 H14 V20")
    val Permisos = trazo("permisos", "M7 3 H14 L18 7 V21 H7 Z M14 3 V7 H18 M10 12 H15 M10 16 H15")
    val Asistencias = trazo("asistencias", "M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 M8 12.5 L11 15.5 L16.5 9")
    /** Escoba con polvo, como en los prototipos. */
    val Limpieza = trazo("limpieza", "M15.5 2.5 L12 12 M8.5 12.5 L14.5 14.5 L12.5 20.5 L4 21 C3.5 17.5 5.5 14.5 8.5 12.5 Z M7.5 16 L6.8 20.7 M10 16.6 L9.6 20.6 M19 8.5 L17.8 12.5 M21 10.5 L20 13.5 M5 7.2 A1.5 1.5 0 1 0 5 4.2 A1.5 1.5 0 1 0 5 7.2 M3.2 10 A1.3 1.3 0 1 0 3.2 7.4 A1.3 1.3 0 1 0 3.2 10")
    /** Manos en oración (cultos). */
    val Cultos = trazo("cultos", "M12 3 C10.3 5.2 9.3 8 9.3 11 L6.2 14.2 L9.4 17.6 L12 20 L14.6 17.6 L17.8 14.2 L14.7 11 C14.7 8 13.7 5.2 12 3 Z M12 3 V20 M9.3 11 L12 14 L14.7 11")
    /** Plato con tenedor y cuchara (reservas). */
    val Reservas = trazo("reservas", "M12 6.6 A5.4 5.4 0 1 0 12 17.4 A5.4 5.4 0 1 0 12 6.6 Z M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9 Z M3.6 3.5 V8 M5.2 3.5 V8 M6.8 3.5 V8 M3.6 8 Q5.2 10.6 6.8 8 M5.2 10 V20.5 M19.2 3.5 C17.6 3.5 17 6.5 17.5 8.5 C17.8 9.8 18.5 10 19.2 10 C19.9 10 20.6 9.8 20.9 8.5 C21.4 6.5 20.8 3.5 19.2 3.5 Z M19.2 10 V20.5")
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
    val FlechaDiagonal = trazo("flecha_diagonal", "M7 17 L17 7 M9 7 H17 V15")
    val Vacio = trazo("vacio", "M4 7 H20 V20 H4 Z M4 7 L7 3 H17 L20 7 M9 12 H15")
    val Notas = trazo("notas", "M5 20 V12 M12 20 V5 M19 20 V9 M3 20 H21")
    val Almuerzo = trazo("almuerzo", "M8 3 V10 M5 3 V8 A3 3 0 0 0 11 8 V3 M8 10 V21 M17 3 C14 5 14 11 17 13 V21 M17 3 V13")
    val Cena = trazo("cena", "M3 18 H21 M5 18 A7 7 0 0 1 19 18 M12 8 V6 M10 6 H14")
    val Desayuno = trazo("desayuno", "M4 10 H17 V16 A4 4 0 0 1 13 20 H8 A4 4 0 0 1 4 16 Z M17 12 H19 A2 2 0 0 1 19 16 H17 M8 3 C7 5 9 6 8 8 M12 3 C11 5 13 6 12 8")
    val Sol = trazo("sol", "M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8 M12 2 V4 M12 20 V22 M2 12 H4 M20 12 H22 M5 5 L6.5 6.5 M17.5 17.5 L19 19 M5 19 L6.5 17.5 M17.5 6.5 L19 5")
    val Luna = trazo("luna", "M20 14 A8 8 0 1 1 10 4 A6 6 0 0 0 20 14 Z")
    val ChevronAbajo = trazo("chevron_abajo", "M6 9 L12 15 L18 9")
    val ChevronArriba = trazo("chevron_arriba", "M6 15 L12 9 L18 15")
    val Teclado = trazo("teclado", "M4 5 H20 V19 H4 Z M8 9 H8.01 M12 9 H12.01 M16 9 H16.01 M8 13 H8.01 M12 13 H12.01 M16 13 H16.01 M8 16.5 H16")
    val ChevronDerecha = trazo("chevron_derecha", "M9 6 L15 12 L9 18")
    val Cama = relleno("cama", "M2 4 H4 V15.5 H22 V19.5 H20 V18 H4 V19.5 H2 Z M10 10.5 H19 A3 3 0 0 1 22 13.5 V15 H10 Z M6.5 9.5 A2.2 2.2 0 1 0 6.5 13.9 A2.2 2.2 0 1 0 6.5 9.5 Z")
    val Ropero = relleno("ropero", "M5 2 H19 V20 H5 Z M11.4 3.5 H12.6 V18.5 H11.4 Z M9.4 10.8 H10.6 V13.2 H9.4 Z M13.4 10.8 H14.6 V13.2 H13.4 Z M6.5 20.2 H9 V22 H6.5 Z M15 20.2 H17.5 V22 H15 Z")
    val Escritorio = relleno("escritorio", "M3 9 H21 V11 H3 Z M4.5 11 H6.5 V20.5 H4.5 Z M17.5 11 H19.5 V20.5 H17.5 Z M8.5 11 H15.5 V13 H8.5 Z M9.5 3 H14.5 A1 1 0 0 1 15.5 4 V7.2 H8.5 V4 A1 1 0 0 1 9.5 3 Z M11.3 13 H12.7 V17.6 H11.3 Z M8.3 18.6 H15.7 V20.2 H8.3 Z")
    val Basura = relleno("basura", "M4 5.2 H20 V7.2 H4 Z M9 3 H15 V5.2 H9 Z M5.5 8.2 H18.5 L17.5 21 H6.5 Z M9 10.6 H10.3 V18.6 H9 Z M11.4 10.6 H12.7 V18.6 H11.4 Z M13.7 10.6 H15 V18.6 H13.7 Z")
    val Olores = trazo("olores", "M8 4 C6 7 10 9 8 12 C6 15 10 17 8 20 M15 4 C13 7 17 9 15 12 C13 15 17 17 15 20")
    val Piso = relleno("piso", "M6.00 8.5 L7.71 8.5 L6.78 11.2 L4.70 11.2 Z M9.43 8.5 L11.14 8.5 L10.96 11.2 L8.87 11.2 Z M12.86 8.5 L14.57 8.5 L15.13 11.2 L13.04 11.2 Z M16.29 8.5 L18.00 8.5 L19.30 11.2 L17.22 11.2 Z M6.78 11.2 L8.87 11.2 L8.29 14.0 L5.82 14.0 Z M10.96 11.2 L13.04 11.2 L13.24 14.0 L10.76 14.0 Z M15.13 11.2 L17.22 11.2 L18.18 14.0 L15.71 14.0 Z M3.35 14.0 L5.82 14.0 L4.86 16.8 L2.00 16.8 Z M8.29 14.0 L10.76 14.0 L10.57 16.8 L7.71 16.8 Z M13.24 14.0 L15.71 14.0 L16.29 16.8 L13.43 16.8 Z M18.18 14.0 L20.65 14.0 L22.00 16.8 L19.14 16.8 Z")
    val Atras = trazo("atras", "M19 12 H5 M11 6 L5 12 L11 18")
    val Libro = trazo("libro", "M12 6.5 C10 5 7 4.5 3.5 5 V18 C7 17.5 10 18 12 19.5 C14 18 17 17.5 20.5 18 V5 C17 4.5 14 5 12 6.5 Z M12 6.5 V19.5")
    val Cruz = trazo("cruz", "M12 5.5 V18.5 M7 10 H17")
    private const val OJO = "M12 4.5 C7 4.5 3.2 8 1.5 12 C3.2 16 7 19.5 12 19.5 C17 19.5 20.8 16 22.5 12 C20.8 8 17 4.5 12 4.5 Z " +
        "M12 8.2 A3.8 3.8 0 1 0 12 15.8 A3.8 3.8 0 1 0 12 8.2 Z M12 10.1 A1.9 1.9 0 1 1 12 13.9 A1.9 1.9 0 1 1 12 10.1 Z"
    /** Ojo relleno: mostrar la contraseña. */
    val Ojo: ImageVector = ImageVector.Builder("ojo", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString(OJO).toNodes(), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd)
    }.build()
    /** Ojo tachado: ocultar la contraseña. */
    val OjoTachado: ImageVector = ImageVector.Builder("ojo_tachado", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(pathData = PathParser().parsePathString(OJO).toNodes(), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd)
        addPath(pathData = PathParser().parsePathString("M4 4 L20 20").toNodes(), stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round)
    }.build()
    val Adjuntar = trazo("adjuntar", "M10 14 A4 4 0 0 0 15.7 14 L18.5 11.2 A4 4 0 0 0 12.8 5.5 L11.5 6.8 M14 10 A4 4 0 0 0 8.3 10 L5.5 12.8 A4 4 0 0 0 11.2 18.5 L12.5 17.2")
    /** Campana rellena del inicio (como en los prototipos). */
    val CampanaLlena: ImageVector = ImageVector.Builder("campana_llena", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M12 2.5 A1.5 1.5 0 0 1 13.5 4 V4.6 C16.6 5.4 18 8 18 11 V15 L20 17.5 V18.5 H4 V17.5 L6 15 V11 C6 8 7.4 5.4 10.5 4.6 V4 A1.5 1.5 0 0 1 12 2.5 Z M9.8 19.8 H14.2 A2.2 2.2 0 0 1 9.8 19.8 Z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
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
