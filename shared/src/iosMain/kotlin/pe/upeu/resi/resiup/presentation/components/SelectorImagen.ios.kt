package pe.upeu.resi.resiup.presentation.components

import androidx.compose.runtime.Composable
import pe.upeu.resi.resiup.domain.model.ImagenElegida

/** Pendiente en iOS (PHPicker): requiere una Mac para escribirlo y probarlo. Por ahora no abre nada. */
@Composable
actual fun rememberSelectorImagen(onElegida: (ImagenElegida?) -> Unit): () -> Unit = { onElegida(null) }
