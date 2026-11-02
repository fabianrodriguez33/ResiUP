package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable
import pe.upeu.resi.resiup.domain.model.ImagenElegida

/**
 * Devuelve una función que abre la galería del teléfono para elegir una foto. La imagen llega ya reducida (máx. 1600 px,
 * JPEG) para no gastar datos ni superar el límite de 5 MB del servidor; null si la persona cancela o no se pudo leer.
 */
@Composable
expect fun rememberSelectorImagen(onElegida: (ImagenElegida?) -> Unit): () -> Unit
