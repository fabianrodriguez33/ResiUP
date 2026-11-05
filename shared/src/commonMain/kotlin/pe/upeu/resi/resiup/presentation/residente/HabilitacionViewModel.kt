package pe.upeu.resi.resiup.presentation.residente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.model.Habilitacion
import pe.upeu.resi.resiup.domain.repository.TramitesRepository

/**
 * Si el residente puede pedir permisos esta semana (domingo a sábado). Es una consulta en línea: sin conexión no se muestra
 * ningún aviso (null) y el servidor sigue validando al enviar.
 */
class HabilitacionViewModel(private val repo: TramitesRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Habilitacion?>(null)
    val estado: StateFlow<Habilitacion?> = _estado.asStateFlow()

    fun cargar() {
        viewModelScope.launch { repo.miHabilitacion().onSuccess { _estado.value = it } }
    }
}
