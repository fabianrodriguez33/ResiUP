package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.ListaUiState

/** Lista con estados de carga, vacío, error con reintento y botón «Ver más». */
@Composable
fun <T> ListaPaginada(
    estado: ListaUiState<T>,
    onReintentar: () -> Unit,
    onVerMas: () -> Unit,
    tituloVacio: String,
    mensajeVacio: String,
    modifier: Modifier = Modifier,
    contenido: @Composable (T) -> Unit,
) {
    when {
        estado.cargandoInicial -> IndicadorCarga(modifier)
        estado.error != null && estado.items.isEmpty() -> EstadoError(estado.error.aMensaje(), onReintentar, modifier)
        estado.items.isEmpty() -> EstadoVacio(tituloVacio, mensajeVacio, modifier)
        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            items(estado.items) { contenido(it) }
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = Espacio.s), contentAlignment = Alignment.Center) {
                    when {
                        estado.cargandoMas -> ResiBoton("Cargando…", {}, cargando = true, habilitado = false, variante = VarianteBoton.Terciario)
                        estado.error != null -> ResiBoton("Reintentar", onVerMas, variante = VarianteBoton.Secundario)
                        !estado.ultima -> ResiBoton("Ver más", onVerMas, variante = VarianteBoton.Secundario)
                    }
                }
            }
        }
    }
}
