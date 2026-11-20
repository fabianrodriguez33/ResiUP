package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberExportador(): (String, String) -> Unit = { _, contenido ->
    // En iPhone se comparte el contenido como texto; el menú permite guardarlo en Archivos.
    val raiz = UIApplication.sharedApplication.keyWindow?.rootViewController
    raiz?.presentViewController(UIActivityViewController(activityItems = listOf(contenido), applicationActivities = null), animated = true, completion = null)
}
