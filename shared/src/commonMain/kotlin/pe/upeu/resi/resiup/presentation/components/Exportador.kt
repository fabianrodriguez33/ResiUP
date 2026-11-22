package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que guarda un archivo de texto (CSV) y abre el menú de compartir del teléfono.
 * Parámetros: nombre del archivo y su contenido.
 */
@Composable
expect fun rememberExportador(): (nombre: String, contenido: String) -> Unit
