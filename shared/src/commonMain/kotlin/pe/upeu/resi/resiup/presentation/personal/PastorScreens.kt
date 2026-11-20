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
import pe.upeu.resi.resiup.presentation.residente.ReservasRepository
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

    fun crear(tipo: TipoCulto, alListo: () -> Unit) = ejecutar(alListo) { repo.abrirSesion(tipo).map { } }
    fun terminar(id: Long, alListo: () -> Unit) = ejecutar(alListo) { repo.cerrarSesion(id).map { } }
    fun reabrir(id: Long, alListo: () -> Unit) = ejecutar(alListo) { repo.reabrirSesion(id).map { } }

    suspend fun presentes(id: Long) = repo.presentes(id)
    suspend fun exportar(id: Long) = repo.exportarPresentes(id)

    private fun ejecutar(alListo: () -> Unit, bloque: suspend () -> Result<Unit>) {
        if (_accion.value.trabajando) return
        _accion.value = AccionCulto(trabajando = true)
        viewModelScope.launch {
            bloque()
                .onSuccess { _accion.value = AccionCulto(); _fecha.value = hoyLima(); cargar(); alListo() }
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
                    val puedeCrear = fecha == hoy && s.datos.none { !it.cerrada } && s.datos.size < TipoCulto.entries.size
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

class ReservasPastorViewModel(
    private val api: ReservasApi,
    private val residentes: pe.upeu.resi.resiup.domain.repository.ResidenteRepository,
) : ViewModel() {
    private val _fecha = MutableStateFlow(hoyLima())
    val fecha: StateFlow<LocalDate> = _fecha.asStateFlow()
    private val _lista = MutableStateFlow<Carga<List<ReservaPastor>>>(Carga.Cargando)
    val lista: StateFlow<Carga<List<ReservaPastor>>> = _lista.asStateFlow()
    private val _cerradas = MutableStateFlow<Set<String>>(emptySet())
    val cerradas: StateFlow<Set<String>> = _cerradas.asStateFlow()
    private val _error = MutableStateFlow<ErrorApi?>(null)
    val error: StateFlow<ErrorApi?> = _error.asStateFlow()
    private val _residentes = MutableStateFlow<List<pe.upeu.resi.resiup.domain.model.ResidenteResumen>>(emptyList())
    val residentesActivos: StateFlow<List<pe.upeu.resi.resiup.domain.model.ResidenteResumen>> = _residentes.asStateFlow()
    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando.asStateFlow()

    init { cargar(); viewModelScope.launch { residentes.listarResidentes().onSuccess { _residentes.value = it } } }

    fun elegir(f: LocalDate) { _fecha.value = f; cargar() }

    fun cargar() {
        val f = _fecha.value
        viewModelScope.launch {
            _lista.value = ejecutarLlamada { api.delDia(f.toString(), null).map { ReservaPastor(it.id, it.residenteNombre, it.codigo, it.tipo.aTipoReserva(), it.estado.aEstadoReserva(), it.motivo, it.curso) } }.aCarga()
            ejecutarLlamada { api.cierres(f.toString()) }.onSuccess { _cerradas.value = it.toSet() }
        }
    }

    fun decidir(id: Long, aprobar: Boolean, comentario: String?) {
        viewModelScope.launch {
            ejecutarLlamada { api.decidir(id, aprobar, comentario) }.onSuccess { _error.value = null; cargar() }.onFailure { _error.value = it.aErrorApi() }
        }
    }

    fun cerrar(grupo: String) {
        viewModelScope.launch {
            ejecutarLlamada { api.cerrar(_fecha.value.toString(), grupo) }.onSuccess { _error.value = null; cargar() }.onFailure { _error.value = it.aErrorApi() }
        }
    }

    suspend fun exportar(grupo: String) = ejecutarLlamada { api.exportar(_fecha.value.toString(), grupo) }

    fun crear(tipo: TipoReserva, residenteId: Long, motivo: String, curso: String?, alListo: () -> Unit) {
        if (_enviando.value) return
        _enviando.value = true
        viewModelScope.launch {
            ejecutarLlamada { api.crearComoPastor(pe.upeu.resi.resiup.data.remote.ReservaPastorRequestDto(residenteId, tipo.aApi(), _fecha.value.toString(), motivo, curso)) }
                .onSuccess { _error.value = null; _enviando.value = false; cargar(); alListo() }
                .onFailure { _error.value = it.aErrorApi(); _enviando.value = false }
        }
    }

    fun limpiarError() { _error.value = null }
}

private sealed interface RutaReservasPastor {
    data object Inicio : RutaReservasPastor
    data class Fecha(val grupo: String) : RutaReservasPastor
    data class Lista(val grupo: String) : RutaReservasPastor
    data class Nueva(val grupo: String) : RutaReservasPastor
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
        ruta = when (val r = ruta) { is RutaReservasPastor.Lista -> RutaReservasPastor.Fecha(r.grupo); is RutaReservasPastor.Nueva -> RutaReservasPastor.Fecha(r.grupo); else -> RutaReservasPastor.Inicio }
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
            is RutaReservasPastor.Fecha -> FechaReservas(vm, r.grupo, onLista = { ruta = RutaReservasPastor.Lista(r.grupo) }, onNueva = { ruta = RutaReservasPastor.Nueva(r.grupo) })
            is RutaReservasPastor.Lista -> ListaReservasPastor(vm, r.grupo)
            is RutaReservasPastor.Nueva -> NuevaReservaPastor(vm, r.grupo, onListo = { ruta = RutaReservasPastor.Fecha(r.grupo) })
        }
    }
}

/** «Almuerzo / Elija la fecha»: calendario, franja de la semana y el botón para crear una reserva. */
@Composable
private fun FechaReservas(vm: ReservasPastorViewModel, grupo: String, onLista: () -> Unit, onNueva: () -> Unit) {
    val fecha by vm.fecha.collectAsState()
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
        ResiBoton("Crear nueva reserva", onNueva, Modifier.fillMaxWidth())
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
                        ResiTarjeta(onClick = if (r.estado == EstadoReserva.Pendiente && !cerrada) ({ elegida = r }) else null) {
                            ElementoLista(
                                r.nombre, subtitulo = r.codigo, icono = if (grupo == "almuerzo") ResiIconos.Almuerzo else if (grupo == "cena") ResiIconos.Cena else ResiIconos.Desayuno, acento = Acento.Azul,
                                final = { ChipEstado(r.estado.etiqueta, when (r.estado) { EstadoReserva.Pendiente -> EstadoVisual.Advertencia; EstadoReserva.Reservado -> EstadoVisual.Exito; EstadoReserva.Rechazada -> EstadoVisual.Error; else -> EstadoVisual.Neutro }) },
                            )
                        }
                    }
                }
            }
        }
        // Como en los prototipos: «Cerrar reservas»; ya cerradas, queda en gris y aparece «Exportar».
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
                    ResiBoton("Aprobar", { vm.decidir(r.id, true, null); elegida = null }, Modifier.fillMaxWidth())
                    ResiBoton("Rechazar", { rechazando = r; elegida = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
                }
            }
        }
    }
    rechazando?.let { r ->
        DialogoMotivo("Rechazar reserva", "El residente verá el motivo.", "Motivo del rechazo", "Rechazar", { vm.decidir(r.id, false, it); rechazando = null }, { rechazando = null }, destructivo = true)
    }
}

/** El pastor crea la reserva de un residente: almuerzo hasta la 1 p. m., cena y desayuno hasta las 6 p. m. (lo valida el servidor). */
@Composable
private fun NuevaReservaPastor(vm: ReservasPastorViewModel, grupo: String, onListo: () -> Unit) {
    val residentes by vm.residentesActivos.collectAsState()
    val error by vm.error.collectAsState()
    val enviando by vm.enviando.collectAsState()
    var residente by remember { mutableStateOf<pe.upeu.resi.resiup.domain.model.ResidenteResumen?>(null) }
    var turno by remember { mutableStateOf<Boolean?>(null) }
    var motivo by remember { mutableStateOf("") }
    var curso by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Reserva de ${nombreGrupo(grupo)}", subtitulo = "Llene los datos")
        CampoDesplegable("Residente", residentes, residente, { "${it.nombreCompleto} (${it.codigo})" }, { residente = it })
        if (grupo == "cena") CampoDesplegable("Turno o Táper", listOf(true, false), turno, { if (it) "Turno" else "Táper" }, { turno = it })
        pe.upeu.resi.resiup.presentation.components.ResiCampoTexto(motivo, { motivo = it.take(300) }, "Motivo", habilitado = !enviando)
        if (grupo != "desayuno") pe.upeu.resi.resiup.presentation.components.ResiCampoTexto(curso, { curso = it.take(100) }, "Curso", habilitado = !enviando)
        (aviso ?: error?.let { if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje() })?.let { BannerMensaje(it, EstadoVisual.Error) }
        ResiBoton("Enviar reserva", {
            val r = residente
            when {
                r == null -> aviso = "Elige al residente"
                grupo == "cena" && turno == null -> aviso = "Elige turno o táper"
                motivo.isBlank() -> aviso = "Indica el motivo"
                else -> {
                    aviso = null
                    val tipo = when (grupo) { "almuerzo" -> TipoReserva.Almuerzo; "desayuno" -> TipoReserva.Desayuno; else -> if (turno == true) TipoReserva.CenaSegundoTurno else TipoReserva.CenaTaper }
                    vm.crear(tipo, r.id, motivo.trim(), curso.trim().ifBlank { null }, onListo)
                }
            }
        }, Modifier.fillMaxWidth(), cargando = enviando)
    }
}
