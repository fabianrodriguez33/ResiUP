package pe.upeu.resi.resiup.presentation.personal

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.data.remote.ReservasApi
import pe.upeu.resi.resiup.data.remote.aDominio
import pe.upeu.resi.resiup.data.remote.aEstadoReserva
import pe.upeu.resi.resiup.data.remote.aTipoReserva
import pe.upeu.resi.resiup.data.remote.aApi
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.EstadoReserva
import pe.upeu.resi.resiup.domain.model.SesionCulto
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.domain.model.TipoReserva
import pe.upeu.resi.resiup.domain.repository.CultoRepository
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.BotonAtras
import pe.upeu.resi.resiup.presentation.components.CalendarioAsistencias
import pe.upeu.resi.resiup.presentation.components.CampoDesplegable
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.CuadriculaModulos
import pe.upeu.resi.resiup.presentation.components.DialogoMotivo
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.FranjaSemana
import pe.upeu.resi.resiup.presentation.components.IconoAccion
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.components.LocalVolver
import pe.upeu.resi.resiup.presentation.components.ModuloUi
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.domain.repository.ReservasRepository
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.hoyLima
import pe.upeu.resi.resiup.presentation.util.soloHoraLima

// ================================================================= cultos (pastor / monitor)

data class AccionCulto(val trabajando: Boolean = false, val error: ErrorApi? = null)

class CultosViewModel(private val repo: CultoRepository) : ViewModel() {
    private val _fecha = MutableStateFlow(hoyLima())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()
    private val _sesiones = MutableStateFlow<Carga<List<SesionCulto>>>(Carga.Cargando)
    val sesiones: StateFlow<Carga<List<SesionCulto>>> = _sesiones.asStateFlow()
    private val _accion = MutableStateFlow(AccionCulto())
    val accion: StateFlow<AccionCulto> = _accion.asStateFlow()

    init { cargar() }

    fun elegir(f: LocalDate) { _fecha.value = f; cargar() }

    fun cargar() {
        val f = _fecha.value
        viewModelScope.launch { _sesiones.value = repo.sesionesDeFecha(f.toString()).aCarga() }
    }

    fun crear(tipo: TipoCulto, alListo: () -> Unit) = ejecutar(alListo, "Culto ${tipo.etiqueta.lowercase()} creado exitosamente") { repo.abrirSesion(tipo).map { } }
    fun terminar(id: Long, alListo: () -> Unit) = ejecutar(alListo, "Culto terminado exitosamente") { repo.cerrarSesion(id).map { } }
    fun reabrir(id: Long, alListo: () -> Unit) = ejecutar(alListo, "Culto abierto nuevamente") { repo.reabrirSesion(id).map { } }

    suspend fun presentes(id: Long) = repo.presentes(id)
    suspend fun exportar(id: Long) = repo.exportarPresentes(id)

    private fun ejecutar(alListo: () -> Unit, mensajeOk: String, bloque: suspend () -> Result<Unit>) {
        if (_accion.value.trabajando) return
        _accion.value = AccionCulto(trabajando = true)
        viewModelScope.launch {
            bloque()
                .onSuccess { _accion.value = AccionCulto(); _fecha.value = hoyLima(); cargar(); pe.upeu.resi.resiup.presentation.components.Mensajes.exito(mensajeOk); alListo() }
                .onFailure { _accion.value = AccionCulto(error = it.aErrorApi()) }
        }
    }
}

private sealed interface RutaCultos {
    data object Lista : RutaCultos
    data object Nuevo : RutaCultos
    data class Detalle(val sesion: SesionCulto) : RutaCultos
}

private fun TipoCulto.titulo() = "Culto ${etiqueta.lowercase()}"

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CultosPastorScreen() {
    val vm = koinViewModel<CultosViewModel>()
    var ruta by remember { mutableStateOf<RutaCultos>(RutaCultos.Lista) }
    BackHandler(enabled = ruta != RutaCultos.Lista) { ruta = RutaCultos.Lista }
    val alInicio = LocalVolver.current
    CompositionLocalProvider(LocalVolver provides (if (ruta == RutaCultos.Lista) alInicio else ({ ruta = RutaCultos.Lista }))) {
        when (val r = ruta) {
            RutaCultos.Lista -> ListaCultos(vm, onNuevo = { ruta = RutaCultos.Nuevo }, onDetalle = { ruta = RutaCultos.Detalle(it) })
            RutaCultos.Nuevo -> NuevoCulto(vm, onListo = { ruta = RutaCultos.Lista })
            is RutaCultos.Detalle -> DetalleCulto(vm, r.sesion, onListo = { ruta = RutaCultos.Lista })
        }
    }
}

@Composable
private fun ListaCultos(vm: CultosViewModel, onNuevo: () -> Unit, onDetalle: (SesionCulto) -> Unit) {
    val fecha by vm.fecha.collectAsState()
    val sesiones by vm.sesiones.collectAsState()
    val hoy = remember { hoyLima() }
    var calendario by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            "Cultos", Modifier.padding(top = Espacio.l), subtitulo = "Cultos de lunes a viernes",
            accion = { IconoAccion(ResiIconos.Calendario, "Abrir calendario", { calendario = true }) },
        )
        FranjaSemana(fecha, vm::elegir)
        Text("Cultos del ${fecha.toString().fechaLegible()}", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            when (val s = sesiones) {
                Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
                is Carga.Fallo -> { BannerMensaje(s.error.aMensaje(), EstadoVisual.Error); ResiBoton("Reintentar", vm::cargar, variante = VarianteBoton.Secundario) }
                is Carga.Exito -> {
                    s.datos.forEach { c ->
                        ResiTarjeta(onClick = { onDetalle(c) }) {
                            ElementoLista(
                                c.tipo.titulo(), subtitulo = "${c.fecha.fechaLegible()} - ${(c.inicio ?: c.corte).soloHoraLima()}",
                                icono = if (c.tipo == TipoCulto.Matutino) ResiIconos.Sol else ResiIconos.Luna, acento = Acento.Naranja,
                                final = { ChipEstado(if (c.cerrada) "Terminado" else "Abierto", if (c.cerrada) EstadoVisual.Info else EstadoVisual.Exito) },
                            )
                        }
                    }
                    if (s.datos.isEmpty()) Text("No hay cultos registrados", Modifier.fillMaxWidth().padding(top = Espacio.m), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    // Solo se crea el culto de hoy, y mientras no haya otro abierto ni estén los dos.
                    val puedeCrear = fecha == hoy && fecha.dayOfWeek.ordinal < 5 && s.datos.none { !it.cerrada } && s.datos.size < TipoCulto.entries.size
                    ResiBoton("Crear nuevo culto", onNuevo, Modifier.fillMaxWidth(), habilitado = puedeCrear)
                }
            }
        }
    }
    if (calendario) Dialog(onDismissRequest = { calendario = false }) { CalendarioAsistencias(emptyList(), hoy, onElegir = { vm.elegir(it); calendario = false }, conMarcas = false) }
}

@Composable
private fun NuevoCulto(vm: CultosViewModel, onListo: () -> Unit) {
    val sesiones by vm.sesiones.collectAsState()
    val accion by vm.accion.collectAsState()
    val existentes = ((sesiones as? Carga.Exito)?.datos ?: emptyList()).map { it.tipo }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Nuevo culto", subtitulo = "Elije el horario")
        CuadriculaModulos(
            listOf(
                ModuloUi("Matutino", ResiIconos.Sol, Acento.Naranja, { vm.crear(TipoCulto.Matutino, onListo) }, deshabilitado = TipoCulto.Matutino in existentes || accion.trabajando),
                ModuloUi("Vespertino", ResiIconos.Luna, Acento.Azul, { vm.crear(TipoCulto.Vespertino, onListo) }, deshabilitado = TipoCulto.Vespertino in existentes || accion.trabajando),
            ),
        )
        accion.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
    }
}

@Composable
private fun DetalleCulto(vm: CultosViewModel, sesion: SesionCulto, onListo: () -> Unit) {
    val accion by vm.accion.collectAsState()
    val presentes by produceState<Carga<List<pe.upeu.resi.resiup.domain.repository.AsistenciaPresente>>>(Carga.Cargando, sesion.id) { value = vm.presentes(sesion.id).aCarga() }
    Column(Modifier.fillMaxSize().padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(sesion.tipo.titulo(), subtitulo = "Residentes presentes")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            when (val p = presentes) {
                Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
                is Carga.Fallo -> BannerMensaje(p.error.aMensaje(), EstadoVisual.Error)
                is Carga.Exito -> {
                    if (p.datos.isEmpty()) Text("Todavía no hay residentes presentes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    p.datos.forEach { a ->
                        ResiTarjeta {
                            ElementoLista(
                                a.nombreCompleto, subtitulo = a.codigo, icono = ResiIconos.Perfil,
                                final = { if (a.despuesDelCorte) ChipEstado("Tardanza", EstadoVisual.Advertencia) },
                            )
                        }
                    }
                }
            }
        }
        accion.error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        val sinPresentes = (presentes as? Carga.Exito)?.datos?.isEmpty() == true
        if (sinPresentes) {
            val acciones = org.koin.compose.koinInject<pe.upeu.resi.resiup.domain.repository.AccionesPastorRepository>()
            val alcanceCulto = androidx.compose.runtime.rememberCoroutineScope()
            ResiBoton("Eliminar culto", {
                alcanceCulto.launch { acciones.eliminarCulto(sesion.id).onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Culto eliminado exitosamente"); onListo() } }
            }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
        }
        if (!sesion.cerrada) {
            ResiBoton("Terminar culto", { vm.terminar(sesion.id, onListo) }, Modifier.fillMaxWidth(), cargando = accion.trabajando)
        } else {
            val exportar = pe.upeu.resi.resiup.presentation.components.rememberExportador()
            val alcance = androidx.compose.runtime.rememberCoroutineScope()
            Row(Modifier.fillMaxWidth().padding(bottom = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                if (sesion.fecha == hoyLima().toString()) ResiBoton("Abrir nuevamente", { vm.reabrir(sesion.id, onListo) }, Modifier.weight(1f), variante = VarianteBoton.Secundario, cargando = accion.trabajando)
                ResiBoton("Exportar", { alcance.launch { vm.exportar(sesion.id).onSuccess { exportar("culto-${sesion.tipo.etiqueta.lowercase()}-${sesion.fecha}.csv", it) } } }, Modifier.weight(1f))
            }
        }
    }
}

// ================================================================= reservas (pastor / monitor)

data class ReservaPastor(val id: Long, val nombre: String, val codigo: String, val tipo: TipoReserva, val estado: EstadoReserva, val motivo: String, val curso: String?)

class ReservasPastorViewModel(private val api: ReservasApi) : ViewModel() {
    private val _fecha = MutableStateFlow(hoyLima())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()
    private val _lista = MutableStateFlow<Carga<List<ReservaPastor>>>(Carga.Cargando)
    val lista: StateFlow<Carga<List<ReservaPastor>>> = _lista.asStateFlow()
    private val _cerradas = MutableStateFlow<Set<String>>(emptySet())
    val cerradas: StateFlow<Set<String>> = _cerradas.asStateFlow()
    private val _abiertas = MutableStateFlow<Set<String>>(emptySet())
    val abiertas: StateFlow<Set<String>> = _abiertas.asStateFlow()
    private val _error = MutableStateFlow<ErrorApi?>(null)
    val error: StateFlow<ErrorApi?> = _error.asStateFlow()

    init { cargar() }

    fun elegir(f: LocalDate) { _fecha.value = f; cargar() }

    fun cargar() {
        val f = _fecha.value
        viewModelScope.launch {
            _lista.value = ejecutarLlamada { api.delDia(f.toString(), null).map { ReservaPastor(it.id, it.residenteNombre, it.codigo, it.tipo.aTipoReserva(), it.estado.aEstadoReserva(), it.motivo, it.curso) } }.aCarga()
            ejecutarLlamada { api.cierres(f.toString()) }.onSuccess { _cerradas.value = it.toSet() }
            ejecutarLlamada { api.aperturas(f.toString()) }.onSuccess { _abiertas.value = it.toSet() }
        }
    }

    fun decidir(id: Long, aprobar: Boolean, comentario: String?) {
        viewModelScope.launch {
            ejecutarLlamada { api.decidir(id, aprobar, comentario) }.onSuccess { _error.value = null; pe.upeu.resi.resiup.presentation.components.Mensajes.exito(if (aprobar) "Reserva aprobada exitosamente" else "Reserva rechazada exitosamente"); cargar() }.onFailure { _error.value = it.aErrorApi() }
        }
    }

    /** Las reservas de una comida no abren solas: el pastor o monitor las abre para la fecha elegida. */
    fun abrir(grupo: String, alListo: () -> Unit = {}) {
        viewModelScope.launch {
            ejecutarLlamada { api.abrir(_fecha.value.toString(), grupo) }.onSuccess { _error.value = null; pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Reservas abiertas exitosamente"); cargar(); alListo() }.onFailure { _error.value = it.aErrorApi() }
        }
    }

    fun cerrar(grupo: String) {
        viewModelScope.launch {
            ejecutarLlamada { api.cerrar(_fecha.value.toString(), grupo) }.onSuccess { _error.value = null; pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Reservas cerradas exitosamente"); cargar() }.onFailure { _error.value = it.aErrorApi() }
        }
    }

    suspend fun exportar(grupo: String) = ejecutarLlamada { api.exportar(_fecha.value.toString(), grupo) }

    fun limpiarError() { _error.value = null }
}

private sealed interface RutaReservasPastor {
    data object Inicio : RutaReservasPastor
    data class Fecha(val grupo: String) : RutaReservasPastor
    data class Lista(val grupo: String) : RutaReservasPastor
}

private fun nombreGrupo(grupo: String) = when (grupo) { "almuerzo" -> "Almuerzo"; "cena" -> "Cena"; else -> "Desayuno" }
private fun claveGrupo(grupo: String) = grupo.uppercase()

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ReservasPastorScreen() {
    val vm = koinViewModel<ReservasPastorViewModel>()
    var ruta by remember { mutableStateOf<RutaReservasPastor>(RutaReservasPastor.Inicio) }
    val atras: () -> Unit = {
        vm.limpiarError()
        ruta = when (val r = ruta) { is RutaReservasPastor.Lista -> RutaReservasPastor.Fecha(r.grupo); else -> RutaReservasPastor.Inicio }
    }
    BackHandler(enabled = ruta != RutaReservasPastor.Inicio, onBack = atras)
    val alInicio = LocalVolver.current
    CompositionLocalProvider(LocalVolver provides (if (ruta == RutaReservasPastor.Inicio) alInicio else atras)) {
        when (val r = ruta) {
            RutaReservasPastor.Inicio -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TituloPantalla("Reservas", subtitulo = "Elije las reservas")
                CuadriculaModulos(
                    listOf(
                        ModuloUi("Almuerzo", ResiIconos.Almuerzo, Acento.Azul, { ruta = RutaReservasPastor.Fecha("almuerzo") }),
                        ModuloUi("Cena", ResiIconos.Cena, Acento.Azul, { ruta = RutaReservasPastor.Fecha("cena") }),
                        ModuloUi("Desayuno", ResiIconos.Desayuno, Acento.Azul, { ruta = RutaReservasPastor.Fecha("desayuno") }),
                    ),
                )
            }
            is RutaReservasPastor.Fecha -> FechaReservas(vm, r.grupo, onLista = { ruta = RutaReservasPastor.Lista(r.grupo) })
            is RutaReservasPastor.Lista -> ListaReservasPastor(vm, r.grupo)
        }
    }
}

/** «Almuerzo / Elija la fecha»: calendario, franja de la semana y el botón para crear una reserva. */
@Composable
private fun FechaReservas(vm: ReservasPastorViewModel, grupo: String, onLista: () -> Unit) {
    val fecha by vm.fecha.collectAsState()
    val abiertas by vm.abiertas.collectAsState()
    val cerradas by vm.cerradas.collectAsState()
    val error by vm.error.collectAsState()
    val yaCreada = claveGrupo(grupo) in abiertas || claveGrupo(grupo) in cerradas
    // Almuerzo y desayuno se sirven de lunes a viernes; la cena, de lunes a jueves (no hay fines de semana ni cena el viernes).
    val sirve = if (grupo == "cena") fecha.dayOfWeek.ordinal < 4 else fecha.dayOfWeek.ordinal < 5
    val hoy = remember { hoyLima() }
    var calendario by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            nombreGrupo(grupo), Modifier.padding(top = Espacio.l), subtitulo = "Elija la fecha",
            accion = { IconoAccion(ResiIconos.Calendario, "Abrir calendario", { calendario = true }) },
        )
        FranjaSemana(fecha, vm::elegir)
        Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Ver la lista de reservas", role = androidx.compose.ui.semantics.Role.Button, onClick = onLista), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reservas del ${fecha.toString().fechaLegible()}", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            ResiIcono(ResiIconos.ChevronDerecha, null, tint = MaterialTheme.colorScheme.primary, tamano = 22.dp)
        }
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        // Crear una nueva reserva abre las reservas de esa comida en la fecha elegida (solo de hoy en adelante); luego se ve la lista.
        if (yaCreada) {
            val cerradaHoy = claveGrupo(grupo) in cerradas
            ResiTarjeta(onClick = onLista) {
                ElementoLista(
                    "Reservas de ${nombreGrupo(grupo).lowercase()}", subtitulo = fecha.toString().fechaLegible(),
                    icono = if (grupo == "almuerzo") ResiIconos.Almuerzo else if (grupo == "cena") ResiIconos.Cena else ResiIconos.Desayuno, acento = Acento.Azul,
                    final = { ChipEstado(if (cerradaHoy) "Cerradas" else "Abiertas", if (cerradaHoy) EstadoVisual.Info else EstadoVisual.Exito) },
                )
            }
        }
        else if (!sirve) Text("No hay reservas de ${nombreGrupo(grupo).lowercase()} este día", Modifier.fillMaxWidth().padding(top = Espacio.m), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        else ResiBoton("Crear nueva reserva", { vm.abrir(claveGrupo(grupo), onLista) }, Modifier.fillMaxWidth(), habilitado = fecha >= hoy)
    }
    if (calendario) Dialog(onDismissRequest = { calendario = false }) { CalendarioAsistencias(emptyList(), hoy, onElegir = { vm.elegir(it); calendario = false }, conMarcas = false) }
}

@Composable
private fun ListaReservasPastor(vm: ReservasPastorViewModel, grupo: String) {
    val lista by vm.lista.collectAsState()
    val cerradas by vm.cerradas.collectAsState()
    val error by vm.error.collectAsState()
    val fecha by vm.fecha.collectAsState()
    val exportar = pe.upeu.resi.resiup.presentation.components.rememberExportador()
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    var turno by remember { mutableStateOf(true) }
    var elegida by remember { mutableStateOf<ReservaPastor?>(null) }
    var rechazando by remember { mutableStateOf<ReservaPastor?>(null) }
    var cancelando by remember { mutableStateOf<ReservaPastor?>(null) }
    val acciones = org.koin.compose.koinInject<pe.upeu.resi.resiup.domain.repository.AccionesPastorRepository>()
    val titulo = when (grupo) { "almuerzo" -> "Reservas de almuerzo"; "cena" -> "Reservas de cenas"; else -> "Reservas de desayunos" }
    val cerrada = claveGrupo(grupo) in cerradas
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(titulo, Modifier.padding(top = Espacio.l), subtitulo = "Lista de reservas de los residentes")
        if (grupo == "cena") Segmentos(listOf(true, false), turno, { if (it) "Turno" else "Táper" }, { turno = it })
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            when (val l = lista) {
                Carga.Cargando -> IndicadorCarga(Modifier.padding(Espacio.xl))
                is Carga.Fallo -> { BannerMensaje(l.error.aMensaje(), EstadoVisual.Error); ResiBoton("Reintentar", vm::cargar, variante = VarianteBoton.Secundario) }
                is Carga.Exito -> {
                    val visibles = l.datos.filter {
                        when (grupo) {
                            "almuerzo" -> it.tipo == TipoReserva.Almuerzo
                            "desayuno" -> it.tipo == TipoReserva.Desayuno
                            else -> it.tipo == (if (turno) TipoReserva.CenaSegundoTurno else TipoReserva.CenaTaper)
                        }
                    }
                    if (visibles.isEmpty()) Text("No hay reservas", Modifier.fillMaxWidth().padding(top = Espacio.l), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    visibles.forEach { r ->
                        ResiTarjeta(onClick = if ((r.estado == EstadoReserva.Pendiente && !cerrada) || r.estado == EstadoReserva.Reservado) ({ elegida = r }) else null) {
                            ElementoLista(
                                r.nombre, subtitulo = r.codigo, icono = if (grupo == "almuerzo") ResiIconos.Almuerzo else if (grupo == "cena") ResiIconos.Cena else ResiIconos.Desayuno, acento = Acento.Azul,
                                final = { ChipEstado(r.estado.etiqueta, when (r.estado) { EstadoReserva.Pendiente -> EstadoVisual.Advertencia; EstadoReserva.Reservado -> EstadoVisual.Exito; EstadoReserva.Rechazada -> EstadoVisual.Error; else -> EstadoVisual.Neutro }) },
                            )
                        }
                    }
                }
            }
        }
        // «Cerrar reservas»; ya cerradas, queda en gris y aparece «Exportar».
        Row(Modifier.fillMaxWidth().padding(bottom = Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            ResiBoton("Cerrar reservas", { vm.cerrar(claveGrupo(grupo)) }, Modifier.weight(1f), habilitado = !cerrada)
            if (cerrada) ResiBoton("Exportar", {
                alcance.launch { vm.exportar(claveGrupo(grupo)).onSuccess { exportar("reservas-$grupo-$fecha.csv", it) } }
            }, Modifier.weight(1f))
        }
    }
    elegida?.let { r ->
        Dialog(onDismissRequest = { elegida = null }) {
            ResiTarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Text(r.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Motivo: ${r.motivo}" + (r.curso?.let { " · Curso: $it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                    if (r.estado == EstadoReserva.Pendiente) {
                        ResiBoton("Aprobar", { vm.decidir(r.id, true, null); elegida = null }, Modifier.fillMaxWidth())
                        ResiBoton("Rechazar", { rechazando = r; elegida = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
                    } else {
                        // Reservada: el pastor puede cancelarla con motivo; el residente puede volver a reservar si el plazo sigue abierto.
                        ResiBoton("Cancelar reserva", { cancelando = r; elegida = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
                    }
                }
            }
        }
    }
    cancelando?.let { r ->
        DialogoMotivo("Cancelar reserva", "El residente verá el motivo y podrá volver a reservar.", "Motivo", "Cancelar reserva",
            { m -> cancelando = null; alcance.launch { acciones.cancelarReserva(r.id, m).onSuccess { pe.upeu.resi.resiup.presentation.components.Mensajes.exito("Reserva cancelada exitosamente"); vm.cargar() } } },
            { cancelando = null }, destructivo = true)
    }
    rechazando?.let { r ->
        DialogoMotivo("Rechazar reserva", "El residente verá el motivo.", "Motivo del rechazo", "Rechazar", { vm.decidir(r.id, false, it); rechazando = null }, { rechazando = null }, destructivo = true)
    }
}
