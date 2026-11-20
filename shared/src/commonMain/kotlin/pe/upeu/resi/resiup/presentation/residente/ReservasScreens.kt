package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.data.remote.ReservaRequestDto
import pe.upeu.resi.resiup.data.remote.ReservasApi
import pe.upeu.resi.resiup.data.remote.aApi
import pe.upeu.resi.resiup.data.remote.aDominio
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.ComidaOfertada
import pe.upeu.resi.resiup.domain.model.EstadoReserva
import pe.upeu.resi.resiup.domain.model.NuevaReserva
import pe.upeu.resi.resiup.domain.model.Reserva
import pe.upeu.resi.resiup.domain.model.TipoReserva
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.BotonAtras
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.components.LocalVolver
import pe.upeu.resi.resiup.presentation.components.ModuloUi
import pe.upeu.resi.resiup.presentation.components.CuadriculaModulos
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.formatoLima

/** Reservas de alimentación del residente. Solo con conexión: el servidor valida horarios, días cerrados y duplicados. */
class ReservasRepository(private val api: ReservasApi) {
    suspend fun oferta() = ejecutarLlamada { api.oferta().comidas.map { it.aDominio() } }
    suspend fun mias() = ejecutarLlamada { api.mias(0).contenido.map { it.aDominio() } }
    suspend fun reservar(n: NuevaReserva) = ejecutarLlamada { api.reservar(ReservaRequestDto(n.tipo.aApi(), n.fecha, n.motivo, n.curso)).aDominio() }
    suspend fun cancelar(id: Long) = ejecutarLlamada { api.cancelar(id).aDominio() }
}

data class ReservasUi(val oferta: List<ComidaOfertada>, val historial: List<Reserva>)

class ReservasViewModel(private val repo: ReservasRepository) : ViewModel() {
    private val _estado = MutableStateFlow<Carga<ReservasUi>>(Carga.Cargando)
    val estado: StateFlow<Carga<ReservasUi>> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            val oferta = repo.oferta()
            val historial = repo.mias()
            _estado.value = oferta.fold(
                { o -> historial.fold({ h -> Carga.Exito(ReservasUi(o, h)) }, { Carga.Fallo(it.aErrorApi()) }) },
                { Carga.Fallo(it.aErrorApi()) },
            )
        }
    }
}

data class ReservaFormState(
    val motivo: String = "", val curso: String = "", val turno: Boolean? = null,
    val errores: Map<String, String> = emptyMap(), val enviando: Boolean = false, val error: ErrorApi? = null, val creada: Boolean = false,
)

class ReservaFormViewModel(private val repo: ReservasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ReservaFormState())
    val estado: StateFlow<ReservaFormState> = _estado.asStateFlow()

    fun motivo(v: String) = _estado.update { it.copy(motivo = v.take(300), errores = it.errores - "motivo", error = null) }
    fun curso(v: String) = _estado.update { it.copy(curso = v.take(100), error = null) }
    fun turno(v: Boolean) = _estado.update { it.copy(turno = v, errores = it.errores - "turno", error = null) }

    fun enviar(comida: ComidaOfertada, ofertaCena: List<ComidaOfertada>) {
        val s = _estado.value
        if (s.enviando) return
        if (s.motivo.isBlank()) { _estado.update { it.copy(errores = mapOf("motivo" to "Indica el motivo")) }; return }
        // Las dos cenas (táper y segundo turno) son opciones de la misma comida.
        if (comida.tipo.esCena && s.turno == null) { _estado.update { it.copy(errores = mapOf("turno" to "Elige turno o táper")) }; return }
        val tipo = if (comida.tipo.esCena) (if (s.turno == true) TipoReserva.CenaSegundoTurno else TipoReserva.CenaTaper) else comida.tipo
        val fecha = (ofertaCena.firstOrNull { it.tipo == tipo } ?: comida).fecha
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            repo.reservar(NuevaReserva(tipo, fecha, s.motivo.trim(), s.curso.trim().ifBlank { null }))
                .onSuccess { _estado.update { it.copy(enviando = false, creada = true) } }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _estado.update { if (err is ErrorApi.Validacion) it.copy(enviando = false, errores = err.porCampo) else it.copy(enviando = false, error = err) }
                }
        }
    }
}

private sealed interface RutaReservas {
    data object Inicio : RutaReservas
    data class Formulario(val comida: ComidaOfertada) : RutaReservas
    data class Detalle(val reserva: Reserva) : RutaReservas
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ReservasResidenteScreen() {
    val vm = koinViewModel<ReservasViewModel>()
    val estado by vm.estado.collectAsState()
    var ruta by remember { mutableStateOf<RutaReservas>(RutaReservas.Inicio) }
    BackHandler(enabled = ruta != RutaReservas.Inicio) { ruta = RutaReservas.Inicio }
    val alInicio = LocalVolver.current
    CompositionLocalProvider(LocalVolver provides (if (ruta == RutaReservas.Inicio) alInicio else ({ ruta = RutaReservas.Inicio }))) {
        when (val r = ruta) {
            RutaReservas.Inicio -> InicioReservas(estado, vm::cargar, onElegir = { ruta = RutaReservas.Formulario(it) }, onDetalle = { ruta = RutaReservas.Detalle(it) })
            is RutaReservas.Formulario -> FormularioReserva(r.comida, (estado as? Carga.Exito)?.datos?.oferta.orEmpty(), onListo = { vm.cargar(); ruta = RutaReservas.Inicio })
            is RutaReservas.Detalle -> DetalleReserva(r.reserva, onListo = { vm.cargar(); ruta = RutaReservas.Inicio }, onVolver = { ruta = RutaReservas.Inicio })
        }
    }
}

@Composable
private fun InicioReservas(estado: Carga<ReservasUi>, reintentar: () -> Unit, onElegir: (ComidaOfertada) -> Unit, onDetalle: (Reserva) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Reservas", subtitulo = "Elije tus reservas")
        when (estado) {
            Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
            is Carga.Fallo -> BannerMensaje(estado.error.aMensaje(), EstadoVisual.Error).also { ResiBoton("Reintentar", reintentar, variante = VarianteBoton.Secundario) }
            is Carga.Exito -> {
                val oferta = estado.datos.oferta
                fun comida(vararg tipos: TipoReserva) = oferta.filter { it.tipo in tipos }.let { l -> l.firstOrNull { it.puedeReservar } ?: l.firstOrNull() }
                fun mosaico(titulo: String, icono: androidx.compose.ui.graphics.vector.ImageVector, c: ComidaOfertada?) =
                    ModuloUi(titulo, icono, Acento.Azul, { if (c != null && c.puedeReservar) onElegir(c) }, deshabilitado = c == null || !c.puedeReservar)
                CuadriculaModulos(
                    listOf(
                        mosaico("Almuerzo", ResiIconos.Almuerzo, comida(TipoReserva.Almuerzo)),
                        mosaico("Cena", ResiIconos.Cena, comida(TipoReserva.CenaTaper, TipoReserva.CenaSegundoTurno)),
                        mosaico("Desayuno", ResiIconos.Desayuno, comida(TipoReserva.Desayuno)),
                    ),
                )
                oferta.firstOrNull { !it.puedeReservar && it.reserva == null && it.motivoNoDisponible != null }?.let {
                    Text(it.motivoNoDisponible!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Historial de Reservas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (estado.datos.historial.isEmpty()) Text("Aún no tienes reservas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                estado.datos.historial.forEach { r ->
                    ResiTarjeta(onClick = { onDetalle(r) }) {
                        ElementoLista(r.tipo.etiqueta, subtitulo = r.fecha.fechaLegible(), icono = ResiIconos.Cena, acento = Acento.Azul, final = { ChipEstado(r.estado.etiqueta, r.estado.aVisual()) })
                    }
                }
            }
        }
    }
}

private fun EstadoReserva.aVisual() = when (this) {
    EstadoReserva.Pendiente -> EstadoVisual.Advertencia
    EstadoReserva.Reservado -> EstadoVisual.Exito
    EstadoReserva.Rechazada -> EstadoVisual.Error
    else -> EstadoVisual.Neutro
}

@Composable
private fun FormularioReserva(comida: ComidaOfertada, oferta: List<ComidaOfertada>, onListo: () -> Unit) {
    val vm = koinViewModel<ReservaFormViewModel>()
    val s by vm.estado.collectAsState()
    LaunchedEffect(s.creada) { if (s.creada) onListo() }
    val nombre = when {
        comida.tipo.esCena -> "Cena"
        else -> comida.tipo.etiqueta
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Reserva de $nombre", subtitulo = "Llene los datos")
        if (comida.tipo.esCena) pe.upeu.resi.resiup.presentation.components.CampoDesplegable("Turno o Táper", listOf(true, false), s.turno, { if (it) "Turno" else "Táper" }, vm::turno)
        ResiCampoTexto(s.motivo, vm::motivo, "Motivo", error = s.errores["motivo"], habilitado = !s.enviando)
        if (comida.tipo != TipoReserva.Desayuno) ResiCampoTexto(s.curso, vm::curso, "Curso", error = s.errores["curso"], habilitado = !s.enviando)
        s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Enviar reserva", { vm.enviar(comida, oferta) }, Modifier.fillMaxWidth(), cargando = s.enviando)
    }
}

@Composable
private fun DetalleReserva(r: Reserva, onListo: () -> Unit, onVolver: () -> Unit) {
    val repo = org.koin.compose.koinInject<ReservasRepository>()
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    var confirmar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ErrorApi?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            BotonAtras(onVolver)
            ChipEstado(r.estado.etiqueta, r.estado.aVisual())
        }
        ResiTarjeta {
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Dato("Tipo", r.tipo.etiqueta, Modifier.weight(1f)); Dato("Registrado", r.creadaEn.formatoLima(), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Dato("Motivo", r.motivo, Modifier.weight(1f)); Dato("Curso", r.curso ?: "-", Modifier.weight(1f))
                }
                r.comentario?.let { Dato("Comentario del pastor", it) }
            }
        }
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        if (r.estado.activa) ResiBoton("Cancelar reserva", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
    }
    if (confirmar) {
        DialogoConfirmacion("Cancelar reserva", "¿Seguro que deseas cancelar esta reserva?", "Cancelar reserva", {
            confirmar = false
            alcance.launch { repo.cancelar(r.id).onSuccess { onListo() }.onFailure { error = it.aErrorApi() } }
        }, { confirmar = false }, destructivo = true)
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
