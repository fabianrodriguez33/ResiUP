package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberExportadorImagen(): (String, String, List<String>) -> Unit = { _, titulo, lineas ->
    // En iPhone (sin verificar: no hay Mac) se comparte la lista como texto; el menú permite guardarla.
    val raiz = UIApplication.sharedApplication.keyWindow?.rootViewController
    raiz?.presentViewController(UIActivityViewController(activityItems = listOf(titulo + "\n" + lineas.joinToString("\n")), applicationActivities = null), animated = true, completion = null)
}
