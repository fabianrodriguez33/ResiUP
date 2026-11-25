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
import pe.upeu.resi.resiup.domain.repository.ReservasRepository
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.decodeToImageBitmap
import pe.upeu.resi.resiup.domain.model.ImagenElegida
import pe.upeu.resi.resiup.domain.repository.TramitesRepository
import pe.upeu.resi.resiup.presentation.components.rememberSelectorImagen
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.tramites.EvidenciaImagen
import pe.upeu.resi.resiup.presentation.util.fechaConDia
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
    val motivo: String = "", val curso: String = "", val turno: Boolean? = null, val foto: ImagenElegida? = null,
    val errores: Map<String, String> = emptyMap(), val enviando: Boolean = false, val error: ErrorApi? = null, val creada: Boolean = false,
)

class ReservaFormViewModel(private val repo: ReservasRepository, private val tramites: TramitesRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ReservaFormState())
    val estado: StateFlow<ReservaFormState> = _estado.asStateFlow()

    fun motivo(v: String) = _estado.update { it.copy(motivo = v.take(300), errores = it.errores - "motivo", error = null) }
    fun curso(v: String) = _estado.update { it.copy(curso = v.take(100), error = null) }
    fun turno(v: Boolean) = _estado.update { it.copy(turno = v, errores = it.errores - "turno", error = null) }
    /** Al volver a abrir el formulario: la reserva anterior ya se atendió y, si no se limpia, lo cierra al instante. */
    fun descartarEnviado() { if (_estado.value.creada) _estado.value = ReservaFormState() }

    fun foto(v: ImagenElegida?) { if (v != null) _estado.update { it.copy(foto = v, errores = it.errores - "foto", error = null) } }

    fun enviar(comida: ComidaOfertada, ofertaCena: List<ComidaOfertada>) {
        val s = _estado.value
        if (s.enviando) return
        // Las dos cenas (táper y segundo turno) son opciones de la misma comida. Motivo y foto son obligatorios.
        val faltas = buildMap {
            if (comida.tipo.esCena && s.turno == null) put("turno", "Elige turno o táper")
            if (s.motivo.isBlank()) put("motivo", "Indica el motivo")
            if (s.curso.isBlank()) put("curso", "Indica el curso o actividad")
            if (s.foto == null) put("foto", "Adjunta una foto")
        }
        if (faltas.isNotEmpty()) { _estado.update { it.copy(errores = faltas) }; return }
        val tipo = if (comida.tipo.esCena) (if (s.turno == true) TipoReserva.CenaSegundoTurno else TipoReserva.CenaTaper) else comida.tipo
        val fecha = (ofertaCena.firstOrNull { it.tipo == tipo } ?: comida).fecha
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val evidencia = tramites.subirEvidencia(s.foto!!).getOrElse { e ->
                val err = e.aErrorApi()
                _estado.update { it.copy(enviando = false, error = err) }
                return@launch
            }
            repo.reservar(NuevaReserva(tipo, fecha, s.motivo.trim(), s.curso.trim(), evidencia))
                .onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Reserva enviada exitosamente"); _estado.update { it.copy(enviando = false, creada = true) } }
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
                        ElementoLista(r.tipo.etiqueta, subtitulo = r.fecha.fechaConDia(), icono = r.tipo.icono(), acento = Acento.Azul, final = { ChipEstado(r.estado.etiqueta, r.estado.aVisual()) })
                    }
                }
            }
        }
    }
}

private fun TipoReserva.icono() = when (this) {
    TipoReserva.Almuerzo -> ResiIconos.Almuerzo
    TipoReserva.Desayuno -> ResiIconos.Desayuno
    else -> ResiIconos.Cena
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
    val s by remember(vm) { vm.descartarEnviado(); vm.estado }.collectAsState()
    LaunchedEffect(s.creada) { if (s.creada) onListo() }
    val abrirGaleria = rememberSelectorImagen(vm::foto)
    val nombre = when {
        comida.tipo.esCena -> "Cena"
        else -> comida.tipo.etiqueta
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Reserva de $nombre", subtitulo = "Llene los datos")
        if (comida.tipo.esCena) pe.upeu.resi.resiup.presentation.components.CampoDesplegable("Turno o Táper", listOf(true, false), s.turno, { if (it) "Turno" else "Táper" }, vm::turno)
        ResiCampoTexto(s.motivo, vm::motivo, "Motivo", error = s.errores["motivo"], habilitado = !s.enviando)
        ResiCampoTexto(s.curso, vm::curso, "Curso", error = s.errores["curso"], habilitado = !s.enviando)
        CampoFoto(s.foto, s.errores["foto"], !s.enviando, abrirGaleria)
        s.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Enviar reserva", { vm.enviar(comida, oferta) }, Modifier.fillMaxWidth(), cargando = s.enviando)
    }
}

/** Campo «Adjuntar foto» del formulario: la foto es obligatoria; al elegirla se muestra una vista previa. */
@Composable
private fun CampoFoto(foto: ImagenElegida?, error: String?, habilitado: Boolean, onElegir: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = habilitado, role = Role.Button, onClickLabel = "Adjuntar foto", onClick = onElegir).padding(vertical = Espacio.m),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                ResiIcono(ResiIconos.Adjuntar, null, tint = MaterialTheme.colorScheme.onSurface, tamano = 44.dp)
                Text(if (foto == null) "Adjuntar foto" else "Foto adjunta · toca para cambiarla", style = MaterialTheme.typography.bodyMedium)
            }
        }
        foto?.let { f ->
            val bitmap = remember(f) { runCatching { f.bytes.decodeToImageBitmap() }.getOrNull() }
            if (bitmap != null) Image(bitmap, "Foto elegida", Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Fit)
        }
        error?.let { Text(it, Modifier.padding(start = Espacio.l), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun DetalleReserva(r: Reserva, onListo: () -> Unit, onVolver: () -> Unit) {
    val repo = org.koin.compose.koinInject<ReservasRepository>()
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    var confirmar by remember { mutableStateOf(false) }
    var verFoto by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ErrorApi?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        // Como en el prototipo: flecha arriba y el estado debajo, en una píldora centrada.
        Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            BotonAtras(onVolver)
            ChipEstado(r.estado.etiqueta, r.estado.aVisual(), Modifier.width(128.dp), centrado = true)
        }
        ResiTarjeta {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Dato("Tipo", r.tipo.etiqueta)
                    Dato("Motivo", r.motivo)
                    Dato("Curso", r.curso ?: "-")
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Dato("Registrado", r.creadaEn.formatoLima())
                    if (r.evidenciaId != null) {
                        Column {
                            Text("Archivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Box(
                                Modifier.padding(top = Espacio.xs).size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                                    .clickable(role = Role.Button, onClickLabel = "Ver la foto adjunta") { verFoto = true },
                                contentAlignment = Alignment.Center,
                            ) { ResiIcono(ResiIconos.Adjuntar, null, tint = Color.White, tamano = 28.dp) }
                        }
                    }
                    r.comentario?.let { Dato("Comentario del pastor", it) }
                }
            }
        }
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        // Se puede cancelar mientras esté pendiente o reservada (el servidor la rechaza si ya terminó el plazo).
        if (r.estado.activa) ResiBoton("Cancelar reserva", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
    }
    if (verFoto && r.evidenciaId != null) {
        Dialog(onDismissRequest = { verFoto = false }) {
            ResiTarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    EvidenciaImagen(r.evidenciaId)
                    ResiBoton("Cerrar", { verFoto = false }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                }
            }
        }
    }
    if (confirmar) {
        DialogoConfirmacion("Cancelar reserva", "¿Seguro que deseas cancelar esta reserva?", "Cancelar reserva", {
            confirmar = false
            alcance.launch { repo.cancelar(r.id).onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Reserva cancelada exitosamente"); onListo() }.onFailure { error = it.aErrorApi() } }
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
