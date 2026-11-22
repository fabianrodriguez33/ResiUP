package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Aviso breve al pie de la pantalla («Reserva enviada exitosamente»): verde si salió bien, rojo si no. Cualquier pantalla puede lanzarlo. */
object Mensajes {
    data class Aviso(val texto: String, val exito: Boolean, val id: Long)

    private val _aviso = MutableStateFlow<Aviso?>(null)
    val aviso: StateFlow<Aviso?> = _aviso.asStateFlow()
    private var contador = 0L

    fun exito(texto: String) { _aviso.value = Aviso(texto, true, ++contador) }
    fun error(texto: String) { _aviso.value = Aviso(texto, false, ++contador) }
    fun limpiar(id: Long) { if (_aviso.value?.id == id) _aviso.value = null }
}

/** Barra del aviso: se muestra unos segundos y se oculta sola. */
@Composable
fun AvisoAlPie(modifier: Modifier = Modifier) {
    val a by Mensajes.aviso.collectAsState()
    val actual = a ?: return
    LaunchedEffect(actual.id) { delay(3500); Mensajes.limpiar(actual.id) }
    Box(modifier.fillMaxWidth().background(if (actual.exito) Color(0xFF0F9D58) else MaterialTheme.colorScheme.error).padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(actual.texto, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}
