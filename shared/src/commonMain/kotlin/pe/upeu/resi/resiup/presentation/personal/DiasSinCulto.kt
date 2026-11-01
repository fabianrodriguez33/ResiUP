package pe.upeu.resi.resiup.presentation.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.DiaSinCulto
import pe.upeu.resi.resiup.domain.repository.CalendarioRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CampoFecha
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.perfil.textoVisible
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.hoyLima

data class DiasSinCultoState(
    val lista: Carga<List<DiaSinCulto>> = Carga.Cargando,
    val trabajando: Boolean = false,
    val error: ErrorApi? = null,
    val mensaje: String? = null,
)

/** Feriados y días sin cultos: los marca el pastor (o la administración) y todos los residentes los usan para su racha. */
class DiasSinCultoViewModel(private val repo: CalendarioRepository) : ViewModel() {
    private val _estado = MutableStateFlow(DiasSinCultoState())
    val estado: StateFlow<DiasSinCultoState> = _estado.asStateFlow()

    /** Primero los próximos (del más cercano al más lejano) y después los pasados (del más reciente al más antiguo). */
    private fun ordenar(lista: List<DiaSinCulto>): List<DiaSinCulto> {
        val hoy = hoyLima().toString()
        return lista.filter { it.fecha >= hoy }.sortedBy { it.fecha } + lista.filter { it.fecha < hoy }.sortedByDescending { it.fecha }
    }

    fun cargar() {
        _estado.value = DiasSinCultoState()
        viewModelScope.launch { _estado.value = DiasSinCultoState(lista = repo.diasSinCulto().map { ordenar(it) }.aCarga()) }
    }

    private fun recargarLista(mensaje: String) {
        viewModelScope.launch {
            repo.diasSinCulto().map { ordenar(it) }
                .onSuccess { _estado.value = DiasSinCultoState(lista = Carga.Exito(it), mensaje = mensaje) }
                .onFailure { _estado.value = DiasSinCultoState(lista = _estado.value.lista, mensaje = mensaje) }
        }
    }

    fun marcar(fecha: String, motivo: String, alTerminar: () -> Unit) {
        if (_estado.value.trabajando) return
        _estado.value = _estado.value.copy(trabajando = true, error = null, mensaje = null)
        viewModelScope.launch {
            repo.marcar(fecha, motivo)
                .onSuccess { alTerminar(); recargarLista("Día marcado sin culto") }
                .onFailure { _estado.value = _estado.value.copy(trabajando = false, error = it.aErrorApi()) }
        }
    }

    fun quitar(fecha: String) {
        if (_estado.value.trabajando) return
        _estado.value = _estado.value.copy(trabajando = true, error = null, mensaje = null)
        viewModelScope.launch {
            repo.quitar(fecha)
                .onSuccess { recargarLista("Día quitado: vuelve a haber culto") }
                .onFailure { _estado.value = _estado.value.copy(trabajando = false, error = it.aErrorApi()) }
        }
    }
}

@Composable
fun DiasSinCultoScreen(onVolver: () -> Unit, vm: DiasSinCultoViewModel = koinViewModel()) {
    val s by vm.estado.collectAsState()
    var agregando by remember { mutableStateOf(false) }
    var aQuitar by remember { mutableStateOf<DiaSinCulto?>(null) }
    LaunchedEffect(Unit) { vm.cargar() }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        ResiBoton("Volver", onVolver, Modifier.padding(top = Espacio.l), variante = VarianteBoton.Terciario)
        TituloPantalla("Días sin culto", subtitulo = "Feriados y vacaciones: no cortan la racha de los residentes")
        ResiBoton("Marcar un día sin culto", { agregando = true }, Modifier.fillMaxWidth())
        s.mensaje?.let { BannerMensaje(it, EstadoVisual.Exito) }
        s.error?.let { BannerMensaje(it.textoVisible(), EstadoVisual.Error) }
        ContenidoCarga(s.lista, vm::cargar, Modifier.weight(1f)) { lista ->
            if (lista.isEmpty()) {
                EstadoVacio("Sin días marcados", "Todos los días de lunes a viernes tienen culto. Marca los feriados y las vacaciones para que no corten la racha.")
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    items(lista, key = { it.fecha }) { d ->
                        ResiTarjeta {
                            ElementoLista(
                                d.fecha.fechaLegible(), subtitulo = d.motivo, icono = ResiIconos.Calendario, acento = Acento.Turquesa,
                                final = { TextButton({ aQuitar = d }, enabled = !s.trabajando) { Text("Quitar") } },
                            )
                        }
                    }
                }
            }
        }
    }
    if (agregando) {
        DialogoNuevoDia(
            trabajando = s.trabajando,
            error = s.error?.textoVisible(),
            onGuardar = { fecha, motivo -> vm.marcar(fecha, motivo) { agregando = false } },
            onCancelar = { agregando = false },
        )
    }
    aQuitar?.let { d ->
        DialogoConfirmacion(
            "Quitar día sin culto", "${d.fecha.fechaLegible()} (${d.motivo}) volverá a contar como día con culto.", "Quitar",
            { aQuitar = null; vm.quitar(d.fecha) }, { aQuitar = null }, destructivo = true,
        )
    }
}

@Composable
private fun DialogoNuevoDia(trabajando: Boolean, error: String?, onGuardar: (String, String) -> Unit, onCancelar: () -> Unit) {
    var fecha by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var errorFecha by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Marcar día sin culto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                CampoFecha("Día", fecha, { fecha = it; errorFecha = null }, error = errorFecha, habilitado = !trabajando)
                ResiCampoTexto(motivo, { motivo = it.take(120) }, "Motivo", ayuda = "Por ejemplo: Feriado nacional", habilitado = !trabajando)
                error?.let { BannerMensaje(it, EstadoVisual.Error) }
            }
        },
        confirmButton = {
            TextButton(
                {
                    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull()
                    if (dia == null) errorFecha = "Elige el día"
                    else if (dia.dayOfWeek == DayOfWeek.SATURDAY || dia.dayOfWeek == DayOfWeek.SUNDAY) errorFecha = "Elige un día de lunes a viernes"
                    else onGuardar(fecha, motivo.trim())
                },
                enabled = fecha.isNotBlank() && motivo.isNotBlank() && !trabajando,
            ) { Text(if (trabajando) "Guardando…" else "Guardar") }
        },
        dismissButton = { TextButton(onCancelar, enabled = !trabajando) { Text("Cancelar") } },
    )
}
