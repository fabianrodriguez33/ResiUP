package pe.upeu.resi.resiup.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// iOS: sin implementar ni verificar (no hay Mac). Se declara explícitamente para no simular funcionalidad.
actual fun crearMatrizQr(contenido: String): List<List<Boolean>>? = null

@Composable
actual fun EscanerCamara(activo: Boolean, onCodigo: (String) -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("El escaneo con cámara todavía no está disponible en iOS.")
    }
}
