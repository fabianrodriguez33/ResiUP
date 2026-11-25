package pe.upeu.resi.resiup.presentation.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExportadorImagen(): (String, String, List<String>) -> Unit {
    val contexto = LocalContext.current
    return { nombre, titulo, lineas -> exportarImagen(contexto, nombre, titulo, lineas) }
}

/** Dibuja título y lista sobre fondo blanco, la guarda en Imágenes/ResiUP (Android 10 o más) y ofrece compartirla. */
private fun exportarImagen(contexto: Context, nombre: String, titulo: String, lineas: List<String>) {
    runCatching {
        val margen = 48f
        val alto = 44f
        val ancho = 1080
        val total = (margen * 2 + 90f + alto * (lineas.size + 1)).toInt().coerceAtLeast(300)
        val mapa = Bitmap.createBitmap(ancho, total, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)
        val pTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#012E68"); textSize = 46f; typeface = Typeface.DEFAULT_BOLD }
        val pTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 34f }
        val pLinea = Paint().apply { color = Color.parseColor("#D9DEE8"); strokeWidth = 2f }
        var y = margen + 46f
        lienzo.drawText(titulo, margen, y, pTitulo)
        y += 28f
        lienzo.drawLine(margen, y, ancho - margen, y, pLinea)
        y += alto
        if (lineas.isEmpty()) lienzo.drawText("Sin reservas aprobadas", margen, y, pTexto)
        lineas.forEachIndexed { i, l ->
            lienzo.drawText("${i + 1}.  $l", margen, y, pTexto)
            y += alto
        }
        val envio = Intent(Intent.ACTION_SEND).apply { type = "image/png" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, nombre)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ResiUP")
            }
            val uri = contexto.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores) ?: error("sin destino")
            contexto.contentResolver.openOutputStream(uri)?.use { mapa.compress(Bitmap.CompressFormat.PNG, 100, it) }
            envio.putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Toast.makeText(contexto, "Guardado en Imágenes/ResiUP: $nombre", Toast.LENGTH_LONG).show()
        } else {
            envio.type = "text/plain"
            envio.putExtra(Intent.EXTRA_TEXT, titulo + "\n" + lineas.joinToString("\n"))
        }
        contexto.startActivity(Intent.createChooser(envio, nombre).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(contexto, "No se pudo exportar la imagen", Toast.LENGTH_LONG).show() }
}
