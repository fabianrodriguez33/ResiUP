package pe.upeu.resi.resiup.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import pe.upeu.resi.resiup.domain.model.ImagenElegida

private const val LADO_MAXIMO = 1600
private const val CALIDAD_JPEG = 82

@Composable
actual fun rememberSelectorImagen(onElegida: (ImagenElegida?) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onElegida(uri?.let { reducir(contexto, it) })
    }
    return { lanzador.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Lee la foto, la achica a [LADO_MAXIMO] px y la guarda como JPEG; null si no se puede decodificar. */
private fun reducir(contexto: Context, uri: Uri): ImagenElegida? = runCatching {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contexto.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
    var muestra = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / muestra > LADO_MAXIMO * 2) muestra *= 2
    val bitmap = contexto.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
    } ?: return@runCatching null
    val escala = LADO_MAXIMO.toFloat() / maxOf(bitmap.width, bitmap.height)
    val final = if (escala < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true) else bitmap
    val salida = ByteArrayOutputStream()
    final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
    ImagenElegida(salida.toByteArray(), "foto.jpg", "image/jpeg")
}.getOrNull()
