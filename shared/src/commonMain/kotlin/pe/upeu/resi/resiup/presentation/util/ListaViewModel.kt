package pe.upeu.resi.resiup.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.Pagina

data class ListaUiState<T>(
    val items: List<T> = emptyList(),
    val pagina: Int = -1,
    val cargandoInicial: Boolean = true,
    val cargandoMas: Boolean = false,
    val error: ErrorApi? = null,
    val ultima: Boolean = false,
)

/** Lista paginada con «ver más». Las pantallas observan [estado]; los datos siempre vienen del servidor. */
open class ListaViewModel<T>(private val cargar: suspend (pagina: Int) -> Result<Pagina<T>>) : ViewModel() {
    private val _estado = MutableStateFlow(ListaUiState<T>())
    val estado: StateFlow<ListaUiState<T>> = _estado.asStateFlow()

    init {
        refrescar()
    }

    fun refrescar() {
        _estado.value = ListaUiState()
        pedir(0)
    }

    fun cargarMas() {
        val e = _estado.value
        if (e.cargandoInicial || e.cargandoMas || e.ultima) return
        _estado.update { it.copy(cargandoMas = true, error = null) }
        pedir(e.pagina + 1)
    }

    private fun pedir(pagina: Int) {
        viewModelScope.launch {
            cargar(pagina)
                .onSuccess { p ->
                    _estado.update {
                        it.copy(items = if (pagina == 0) p.contenido else it.items + p.contenido, pagina = pagina, cargandoInicial = false,
                            cargandoMas = false, error = null, ultima = p.ultima)
                    }
                }
                .onFailure { e -> _estado.update { it.copy(cargandoInicial = false, cargandoMas = false, error = e.aErrorApi()) } }
        }
    }
}
