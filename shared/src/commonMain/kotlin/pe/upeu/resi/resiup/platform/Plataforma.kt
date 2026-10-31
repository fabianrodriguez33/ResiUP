package pe.upeu.resi.resiup.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Matriz de módulos del QR (true = oscuro), o null si la plataforma no puede generarlo. */
expect fun crearMatrizQr(contenido: String): List<List<Boolean>>?

/**
 * Vista de cámara que lee códigos QR. Pide el permiso de cámara en el momento de usarla, explica su finalidad y
 * libera la cámara cuando [activo] es falso o la pantalla sale de composición.
 * [onCodigo] puede dispararse varias veces con el mismo código; quien la usa debe filtrar repeticiones.
 */
@Composable
expect fun EscanerCamara(activo: Boolean, onCodigo: (String) -> Unit, modifier: Modifier = Modifier)
