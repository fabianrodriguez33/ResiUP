package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable

/**
 * Devuelve una función que dibuja una lista como imagen (PNG), la guarda y abre el menú de compartir del teléfono.
 * Parámetros: nombre del archivo, título y las líneas de la lista.
 */
@Composable
expect fun rememberExportadorImagen(): (nombre: String, titulo: String, lineas: List<String>) -> Unit
