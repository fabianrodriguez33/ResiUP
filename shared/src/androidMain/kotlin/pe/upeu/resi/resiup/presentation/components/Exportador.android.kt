package pe.upeu.resi.resiup.presentation.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExportador(): (String, String) -> Unit {
    val contexto = LocalContext.current
    return { nombre, contenido -> exportar(contexto, nombre, contenido) }
}

/** Guarda el CSV en Descargas (Android 10 o más) y ofrece compartirlo; en versiones anteriores solo lo comparte como texto. */
private fun exportar(contexto: Context, nombre: String, contenido: String) {
    runCatching {
        val envio = Intent(Intent.ACTION_SEND).apply { type = "text/csv" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val valores = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombre)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/ResiUP")
            }
            val uri = contexto.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: error("sin destino")
            contexto.contentResolver.openOutputStream(uri)?.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
            envio.putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Toast.makeText(contexto, "Guardado en Descargas/ResiUP: $nombre", Toast.LENGTH_LONG).show()
        } else {
            envio.putExtra(Intent.EXTRA_TEXT, contenido)
        }
        contexto.startActivity(Intent.createChooser(envio, nombre).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(contexto, "No se pudo exportar el archivo", Toast.LENGTH_LONG).show() }
}
