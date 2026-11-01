package pe.upeu.resi.resiup.presentation.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.data.local.AvisosDelSistema
import pe.upeu.resi.resiup.domain.model.Notificacion
import pe.upeu.resi.resiup.domain.repository.NotificacionesRepository
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.formatoLima

/**
 * Revisa los avisos del servidor: mantiene el número de no leídos de la campana y lanza las notificaciones del sistema de
 * los nuevos. Mientras la app está abierta lo hace cada 30 s ([vigilar]); con la app cerrada lo repite un trabajo periódico.
 */
class GestorAvisos(private val repo: NotificacionesRepository, private val sistema: AvisosDelSistema) {
    private val _noLeidas = MutableStateFlow(0)
    val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    suspend fun vigilar() {
        while (true) {
            revisar()
            delay(INTERVALO_MS)
        }
    }

    suspend fun revisar() {
        repo.cantidadNoLeidas().onSuccess { _noLeidas.value = it }
        if (_noLeidas.value > 0) {
            repo.recientes().onSuccess { lista -> sistema.mostrarNuevos(lista.filter { !it.leida }) }
        }
    }

    fun restar(cantidad: Int = 1) {
        _noLeidas.value = (_noLeidas.value - cantidad).coerceAtLeast(0)
    }

    fun limpiar() {
        _noLeidas.value = 0
    }

    private companion object {
        const val INTERVALO_MS = 30_000L
    }
}

class BandejaViewModel(private val repo: NotificacionesRepository, private val avisos: GestorAvisos) : ViewModel() {
    private val _lista = MutableStateFlow<Carga<List<Notificacion>>>(Carga.Cargando)
    val lista: StateFlow<Carga<List<Notificacion>>> = _lista.asStateFlow()

    fun cargar() {
        _lista.value = Carga.Cargando
        viewModelScope.launch { _lista.value = repo.recientes().aCarga() }
    }

    fun abrir(n: Notificacion) {
        if (n.leida) return
        marcar { lista -> lista.map { if (it.id == n.id) it.copy(leida = true) else it } }
        avisos.restar()
        viewModelScope.launch { repo.marcarLeida(n.id) }
    }

    fun marcarTodas() {
        marcar { lista -> lista.map { it.copy(leida = true) } }
        avisos.limpiar()
        viewModelScope.launch { repo.marcarTodasLeidas() }
    }

    private fun marcar(cambio: (List<Notificacion>) -> List<Notificacion>) {
        (_lista.value as? Carga.Exito)?.let { _lista.value = Carga.Exito(cambio(it.datos)) }
    }
}

/** Campana de la parte superior con el número de avisos sin leer. */
@Composable
fun CampanaAvisos(noLeidas: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val descripcion = if (noLeidas > 0) "Notificaciones, $noLeidas sin leer" else "Notificaciones"
    Box(
        modifier.size(44.dp).semantics { contentDescription = descripcion }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ResiIcono(ResiIconos.Campana, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (noLeidas > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 2.dp).background(MaterialTheme.colorScheme.error, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(if (noLeidas > 9) "9+" else noLeidas.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun BandejaScreen(onVolver: () -> Unit, vm: BandejaViewModel = koinViewModel()) {
    val estado by vm.lista.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    BackHandler(onBack = onVolver)
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l)) {
        ResiBoton("Volver", onVolver, Modifier.padding(top = Espacio.l), variante = VarianteBoton.Terciario)
        TituloPantalla("Notificaciones", subtitulo = "Permisos, garita y avisos de la residencia")
        ContenidoCarga(estado, vm::cargar, Modifier.weight(1f)) { lista ->
            if (lista.isEmpty()) {
                EstadoVacio("Sin notificaciones", "Aquí aparecerán los avisos sobre tus permisos y la residencia.")
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    if (lista.any { !it.leida }) {
                        ResiBoton("Marcar todas como leídas", vm::marcarTodas, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.s), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Espacio.m)) {
                        items(lista, key = { it.id }) { n -> FilaAviso(n) { vm.abrir(n) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaAviso(n: Notificacion, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Espacio.s),
        horizontalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Box(
            Modifier.padding(top = 6.dp).size(10.dp)
                .background(if (n.leida) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary, CircleShape),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                n.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = if (n.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(n.mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(n.creadaEn.formatoLima(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
