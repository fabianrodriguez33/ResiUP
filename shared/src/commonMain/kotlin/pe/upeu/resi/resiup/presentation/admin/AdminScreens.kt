package pe.upeu.resi.resiup.presentation.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import pe.upeu.resi.resiup.data.remote.dto.PaginaDto
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CampoDesplegable
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.CuadriculaModulos
import pe.upeu.resi.resiup.presentation.components.DialogoMotivo
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EncabezadoSeccion
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.LocalVolver
import pe.upeu.resi.resiup.presentation.components.Mensajes
import pe.upeu.resi.resiup.presentation.components.ModuloUi
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TarjetaHola
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.aErrorApi

// ---------------------------------------------------------------- datos (solo lo que usa esta pantalla)

@Serializable data class HabAdmin(val id: Long, val numero: String, val capacidad: Int, val activa: Boolean = true, val ocupantes: Long = 0, val disponibles: Long = 0)
@Serializable data class HabResumenAdmin(val id: Long, val numero: String, val cama: String? = null)
@Serializable data class ResidenteAdmin(val id: Long, val codigo: String, val nombreCompleto: String, val telefono: String? = null, val activo: Boolean = true, val habitacion: HabResumenAdmin? = null)
@Serializable data class AutorizacionAdmin(val id: Long, val tipo: String, val inicio: String, val fin: String? = null, val referencia: String, val revocada: Boolean = false, val motivoRevocacion: String? = null, val vigente: Boolean = false)
@Serializable data class NuevoResidenteAdmin(val username: String, val password: String, val nombreCompleto: String, val codigo: String, val telefono: String, val email: String, val carrera: String? = null, val ciclo: Int? = null)
@Serializable data class AsignacionAdmin(val habitacionId: Long, val cama: String?)
@Serializable data class NuevaAutorizacionAdmin(val tipo: String, val referencia: String)
@Serializable data class RevocacionAdmin(val motivo: String)
@Serializable data class SemestreAdmin(val id: Long, val nombre: String, val inicio: String, val fin: String, val activo: Boolean = false)
@Serializable data class OcupanteAdmin(val residenteId: Long, val codigo: String, val nombreCompleto: String, val cama: String? = null)
@Serializable data class NuevoSemestreAdmin(val nombre: String, val inicio: String, val fin: String)

private const val B = "api/v1/"

/** Llamadas de administración (todas con conexión; el servidor valida cada dato). */
class AdminApi(private val c: HttpClient) {
    suspend fun residentes(): List<ResidenteAdmin> = c.get("${B}residentes") { parameter("pagina", 0); parameter("tamanio", 100) }.body<PaginaDto<ResidenteAdmin>>().contenido
    suspend fun habitaciones(): List<HabAdmin> = c.get("${B}habitaciones").body()
    suspend fun ocupantes(habitacionId: Long): List<OcupanteAdmin> = c.get("${B}habitaciones/$habitacionId/ocupantes").body()
    suspend fun semestres(): List<SemestreAdmin> = c.get("${B}semestres").body()
    suspend fun semestreActual(): SemestreAdmin = c.get("${B}semestres/actual").body()
    suspend fun terminarSemestre() { c.post("${B}semestres/terminar") }
    suspend fun crearSemestre(s: NuevoSemestreAdmin): SemestreAdmin = c.post("${B}semestres") { setBody(s) }.body()
    suspend fun liberar(id: Long) { c.post("${B}residentes/$id/liberar-habitacion") }
    suspend fun crear(r: NuevoResidenteAdmin): ResidenteAdmin = c.post("${B}residentes") { setBody(r) }.body()
    suspend fun asignar(id: Long, a: AsignacionAdmin) { c.post("${B}residentes/$id/asignacion") { setBody(a) } }
    suspend fun autorizaciones(id: Long): List<AutorizacionAdmin> = c.get("${B}residentes/$id/autorizaciones").body()
    suspend fun autorizar(id: Long, a: NuevaAutorizacionAdmin) { c.post("${B}residentes/$id/autorizaciones") { setBody(a) } }
    suspend fun revocar(id: Long, motivo: String) { c.post("${B}autorizaciones/$id/revocar") { setBody(RevocacionAdmin(motivo)) } }
}

private sealed interface RutaAdmin {
    data object Inicio : RutaAdmin
    data object Residentes : RutaAdmin
    data object Nuevo : RutaAdmin
    data class Detalle(val residente: ResidenteAdmin) : RutaAdmin
    data object Habitaciones : RutaAdmin
    data object Semestres : RutaAdmin
}

private fun String.tipoLegible() = when (this) { "CORTO" -> "Corto"; "LARGO" -> "Largo"; else -> "Pernoctar" }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AdministradorScreen(nombre: String, onIr: (String) -> Unit, onSubpantalla: (Boolean) -> Unit = {}) {
    val api = koinInject<AdminApi>()
    var ruta by remember { mutableStateOf<RutaAdmin>(RutaAdmin.Inicio) }
    val atras: () -> Unit = {
        ruta = when (ruta) { is RutaAdmin.Nuevo, is RutaAdmin.Detalle -> RutaAdmin.Residentes; else -> RutaAdmin.Inicio }
    }
    BackHandler(enabled = ruta != RutaAdmin.Inicio, onBack = atras)
    androidx.compose.runtime.LaunchedEffect(ruta) { onSubpantalla(ruta != RutaAdmin.Inicio) }
    val alInicio = LocalVolver.current
    CompositionLocalProvider(LocalVolver provides (if (ruta == RutaAdmin.Inicio) alInicio else atras)) {
        when (val r = ruta) {
            RutaAdmin.Inicio -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
                TarjetaHola(nombre, "Administrador", Acento.Turquesa, { onIr("mas") }, Modifier.padding(top = 40.dp))
                EncabezadoSeccion("Accesos")
                CuadriculaModulos(
                    listOf(
                        ModuloUi("Residentes", ResiIconos.Perfil, Acento.Azul, { ruta = RutaAdmin.Residentes }),
                        ModuloUi("Habitaciones", ResiIconos.Cama, Acento.Azul, { ruta = RutaAdmin.Habitaciones }),
                        ModuloUi("Semestres", ResiIconos.Calendario, Acento.Azul, { ruta = RutaAdmin.Semestres }),
                    ),
                )
            }
            RutaAdmin.Residentes -> Residentes(api, onNuevo = { ruta = RutaAdmin.Nuevo }, onAbrir = { ruta = RutaAdmin.Detalle(it) })
            RutaAdmin.Nuevo -> NuevoResidente(api, onListo = { ruta = RutaAdmin.Residentes })
            is RutaAdmin.Detalle -> DetalleResidente(api, r.residente, onSinHabitacion = { ruta = RutaAdmin.Residentes })
            RutaAdmin.Habitaciones -> Habitaciones(api)
            RutaAdmin.Semestres -> Semestres(api)
        }
    }
}

@Composable
private fun Residentes(api: AdminApi, onNuevo: () -> Unit, onAbrir: (ResidenteAdmin) -> Unit) {
    var estado by remember { mutableStateOf<Carga<List<ResidenteAdmin>>>(Carga.Cargando) }
    var reintento by remember { mutableStateOf(0) }
    LaunchedEffect(reintento) { estado = ejecutarLlamada { api.residentes() }.aCarga() }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Residentes", Modifier.padding(top = Espacio.l), subtitulo = "Lista de residentes")
        ResiBoton("Nuevo residente", onNuevo, Modifier.fillMaxWidth())
        ContenidoCarga(estado, { reintento++ }, Modifier.weight(1f)) { lista ->
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                if (lista.isEmpty()) Text("No hay residentes", style = MaterialTheme.typography.titleMedium)
                lista.forEach { r ->
                    ResiTarjeta(onClick = { onAbrir(r) }) {
                        ElementoLista(
                            r.nombreCompleto, subtitulo = "${r.codigo} · ${r.habitacion?.let { "Hab. ${it.numero} ${it.cama ?: ""}".trim() } ?: "Sin habitación"}",
                            icono = ResiIconos.Perfil, acento = Acento.Azul,
                            final = { if (!r.activo) ChipEstado("Inactivo", EstadoVisual.Neutro) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NuevoResidente(api: AdminApi, onListo: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("") }
    var ciclo by remember { mutableStateOf<Int?>(null) }
    var celular by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<ErrorApi?>(null) }
    var enviando by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Nuevo residente", subtitulo = "Llene los datos")
        ResiCampoTexto(nombre, { nombre = it.take(150) }, "Nombre completo", error = errores["nombreCompleto"], habilitado = !enviando)
        ResiCampoTexto(codigo, { codigo = it.filter(Char::isDigit).take(9) }, "Código universitario (9 dígitos)", error = errores["codigo"], tipoTeclado = KeyboardType.Number, habilitado = !enviando)
        ResiCampoTexto(correo, { correo = it.take(150) }, "Correo electrónico", error = errores["email"], tipoTeclado = KeyboardType.Email, habilitado = !enviando)
        ResiCampoTexto(celular, { celular = it.filter(Char::isDigit).take(9) }, "Celular (9 dígitos)", error = errores["telefono"], tipoTeclado = KeyboardType.Phone, habilitado = !enviando)
        ResiCampoTexto(carrera, { carrera = it.take(100) }, "Carrera (opcional)", error = errores["carrera"], habilitado = !enviando)
        CampoDesplegable("Ciclo (opcional)", (1..14).toList(), ciclo, { "Ciclo $it" }, { ciclo = it })
        ResiCampoTexto(usuario, { usuario = it.take(50) }, "Usuario", error = errores["username"], habilitado = !enviando)
        ResiCampoTexto(clave, { clave = it.take(72) }, "Contraseña (mínimo 8)", error = errores["password"], esClave = true, habilitado = !enviando)
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
        ResiBoton("Registrar residente", {
            enviando = true; errores = emptyMap(); error = null
            alcance.launch {
                ejecutarLlamada { api.crear(NuevoResidenteAdmin(usuario.trim(), clave, nombre.trim(), codigo, celular, correo.trim(), carrera.trim().ifBlank { null }, ciclo)) }
                    .onSuccess { Mensajes.exito("Residente registrado exitosamente"); onListo() }
                    .onFailure { e -> val x = e.aErrorApi(); if (x is ErrorApi.Validacion) errores = x.porCampo else error = x }
                enviando = false
            }
        }, Modifier.fillMaxWidth(), cargando = enviando)
    }
}

@Composable
private fun DetalleResidente(api: AdminApi, r: ResidenteAdmin, onSinHabitacion: () -> Unit) {
    val alcance = rememberCoroutineScope()
    var habitaciones by remember { mutableStateOf<List<HabAdmin>>(emptyList()) }
    var autorizaciones by remember { mutableStateOf<Carga<List<AutorizacionAdmin>>>(Carga.Cargando) }
    var reintento by remember { mutableStateOf(0) }
    var hab by remember { mutableStateOf<HabAdmin?>(null) }
    var cama by remember { mutableStateOf<String?>(null) }
    var tipo by remember { mutableStateOf("CORTO") }
    var referencia by remember { mutableStateOf("") }
    var revocando by remember { mutableStateOf<AutorizacionAdmin?>(null) }
    var quitando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ErrorApi?>(null) }
    LaunchedEffect(reintento) {
        autorizaciones = ejecutarLlamada { api.autorizaciones(r.id) }.aCarga()
        ejecutarLlamada { api.habitaciones() }.onSuccess { habitaciones = it.filter { h -> h.activa } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(r.nombreCompleto, subtitulo = "${r.codigo} · ${r.habitacion?.let { "Hab. ${it.numero} ${it.cama ?: ""}".trim() } ?: "Sin habitación"}")
        error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }

        EncabezadoSeccion("Habitación")
        CampoDesplegable("Elige la habitación", habitaciones, hab, { "${it.numero} (${it.ocupantes}/${it.capacidad})" }, { hab = it })
        CampoDesplegable("Cama", listOf("A", "B", "C", "D"), cama, { "Cama $it" }, { cama = it })
        if (r.habitacion != null) ResiBoton("Quitar de la habitación", { quitando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
        ResiBoton("Asignar habitación", {
            val h = hab ?: return@ResiBoton
            alcance.launch {
                ejecutarLlamada { api.asignar(r.id, AsignacionAdmin(h.id, cama)) }
                    .onSuccess { error = null; Mensajes.exito("Habitación asignada exitosamente"); reintento++ }.onFailure { error = it.aErrorApi() }
            }
        }, Modifier.fillMaxWidth(), habilitado = hab != null)

        EncabezadoSeccion("Autorizaciones de permiso")
        Text("Con una autorización vigente, el residente envía ese tipo de permiso directo al pastor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ContenidoCarga(autorizaciones, { reintento++ }, Modifier.fillMaxWidth()) { lista ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                if (lista.isEmpty()) Text("Sin autorizaciones registradas", style = MaterialTheme.typography.bodyMedium)
                lista.forEach { a ->
                    ResiTarjeta(onClick = if (a.vigente) ({ revocando = a }) else null) {
                        ElementoLista(
                            a.tipo.tipoLegible(), subtitulo = "Hoja ${a.referencia} · desde ${a.inicio}${a.fin?.let { " hasta $it" } ?: ""}", icono = ResiIconos.Permisos, acento = Acento.Azul,
                            final = { ChipEstado(if (a.revocada) "Revocada" else if (a.vigente) "Vigente" else "Vencida", if (a.vigente) EstadoVisual.Exito else EstadoVisual.Neutro) },
                        )
                    }
                }
            }
        }
        Segmentos(listOf("CORTO", "LARGO", "PERNOCTE"), tipo, { it.tipoLegible() }, { tipo = it })
        ResiCampoTexto(referencia, { referencia = it.take(100) }, "Referencia (número de la hoja firmada)")
        ResiBoton("Registrar autorización", {
            alcance.launch {
                ejecutarLlamada { api.autorizar(r.id, NuevaAutorizacionAdmin(tipo, referencia.trim())) }
                    .onSuccess { error = null; referencia = ""; Mensajes.exito("Autorización registrada exitosamente"); reintento++ }.onFailure { error = it.aErrorApi() }
            }
        }, Modifier.fillMaxWidth(), habilitado = referencia.isNotBlank())
        Spacer(Modifier.padding(bottom = Espacio.xl))
    }
    if (quitando) {
        pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion(
            "Quitar de la habitación", "${r.nombreCompleto} quedará sin habitación ni cama. Podrás asignarle otra después.", "Quitar",
            { quitando = false; alcance.launch { ejecutarLlamada { api.liberar(r.id) }.onSuccess { error = null; Mensajes.exito("Habitación liberada exitosamente"); onSinHabitacion() }.onFailure { error = it.aErrorApi() } } },
            { quitando = false }, destructivo = true,
        )
    }
    revocando?.let { a ->
        DialogoMotivo(
            "Revocar autorización", "El residente volverá a necesitar a su padre o madre para este tipo de permiso.", "Motivo", "Revocar",
            { m -> revocando = null; alcance.launch { ejecutarLlamada { api.revocar(a.id, m) }.onSuccess { Mensajes.exito("Autorización revocada exitosamente"); reintento++ }.onFailure { error = it.aErrorApi() } } },
            { revocando = null }, destructivo = true,
        )
    }
}

@Composable
private fun Habitaciones(api: AdminApi) {
    var estado by remember { mutableStateOf<Carga<List<HabAdmin>>>(Carga.Cargando) }
    var reintento by remember { mutableStateOf(0) }
    var elegida by remember { mutableStateOf<HabAdmin?>(null) }
    var ocupantes by remember { mutableStateOf<Carga<List<OcupanteAdmin>>>(Carga.Cargando) }
    LaunchedEffect(reintento) { estado = ejecutarLlamada { api.habitaciones() }.aCarga() }
    LaunchedEffect(elegida) { elegida?.let { ocupantes = Carga.Cargando; ocupantes = ejecutarLlamada { api.ocupantes(it.id) }.aCarga() } }
    elegida?.let { h ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { elegida = null }) {
            ResiTarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Text("Habitación ${h.numero}", style = MaterialTheme.typography.titleLarge)
                    when (val o = ocupantes) {
                        Carga.Cargando -> Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
                        is Carga.Fallo -> Text("No se pudo cargar.", style = MaterialTheme.typography.bodyMedium)
                        is Carga.Exito -> {
                            if (o.datos.isEmpty()) Text("Sin residentes asignados", style = MaterialTheme.typography.bodyMedium)
                            o.datos.forEach { x -> ElementoLista(x.nombreCompleto, subtitulo = "${x.codigo} · Cama ${x.cama ?: "-"}", icono = ResiIconos.Perfil, acento = Acento.Azul) }
                        }
                    }
                    ResiBoton("Cerrar", { elegida = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Habitaciones", Modifier.padding(top = Espacio.l), subtitulo = "Ocupación de la residencia")
        ContenidoCarga(estado, { reintento++ }, Modifier.weight(1f)) { lista ->
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                lista.forEach { h ->
                    ResiTarjeta(onClick = { elegida = h }) {
                        ElementoLista(
                            h.numero, subtitulo = "${h.ocupantes} de ${h.capacidad} camas ocupadas", icono = ResiIconos.Cama, acento = if (h.disponibles > 0) Acento.Verde else Acento.Ambar,
                            final = { ChipEstado(if (h.disponibles > 0) "${h.disponibles} libres" else "Llena", if (h.disponibles > 0) EstadoVisual.Exito else EstadoVisual.Advertencia) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Semestres(api: AdminApi) {
    val alcance = rememberCoroutineScope()
    var estado by remember { mutableStateOf<Carga<List<SemestreAdmin>>>(Carga.Cargando) }
    var reintento by remember { mutableStateOf(0) }
    var creando by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf("") }
    var fin by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf(false) }
    var terminando by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<ErrorApi?>(null) }
    LaunchedEffect(reintento) { estado = ejecutarLlamada { api.semestres() }.aCarga() }
    if (terminando) {
        pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion(
            "Terminar semestre", "El semestre actual se cierra hoy. Las habitaciones no se vacían hasta que crees el siguiente. ¿Continuar?", "Terminar",
            { terminando = false; alcance.launch { ejecutarLlamada { api.terminarSemestre() }.onSuccess { Mensajes.exito("Semestre terminado exitosamente"); reintento++ }.onFailure { error = it.aErrorApi() } } },
            { terminando = false }, destructivo = true,
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Semestres", subtitulo = "Ciclos de la residencia")
        ContenidoCarga(estado, { reintento++ }, Modifier.fillMaxWidth()) { lista ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                lista.forEach { sm ->
                    ResiTarjeta {
                        ElementoLista(
                            "Semestre ${sm.nombre}", subtitulo = "${sm.inicio} al ${sm.fin}", icono = ResiIconos.Calendario, acento = Acento.Azul,
                            final = { ChipEstado(if (sm.activo) "Actual" else "Cerrado", if (sm.activo) EstadoVisual.Exito else EstadoVisual.Neutro) },
                        )
                    }
                }
            }
        }
        if (!creando) {
            ResiBoton("Crear nuevo semestre", { creando = true }, Modifier.fillMaxWidth())
            if ((estado as? Carga.Exito)?.datos?.any { it.activo } == true) ResiBoton("Terminar semestre actual", { terminando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
        } else {
            EncabezadoSeccion("Nuevo semestre")
            Text("Al crearlo, el semestre actual se cierra y todas las habitaciones quedan vacías. Los residentes conservan su cuenta y su historial.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ResiCampoTexto(nombre, { nombre = it.take(10) }, "Nombre (por ejemplo 2027-1)", error = errores["nombre"])
            pe.upeu.resi.resiup.presentation.components.CampoFecha("Inicio", inicio, { inicio = it }, error = errores["inicio"])
            pe.upeu.resi.resiup.presentation.components.CampoFecha("Fin", fin, { fin = it }, error = errores["fin"])
            error?.let { BannerMensaje(if (it is ErrorApi.Conflicto) it.mensaje else it.aMensaje(), EstadoVisual.Error) }
            ResiBoton("Crear semestre", { confirmar = true }, Modifier.fillMaxWidth(), habilitado = nombre.isNotBlank() && inicio.isNotBlank() && fin.isNotBlank())
            ResiBoton("Cancelar", { creando = false; errores = emptyMap(); error = null }, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario)
        }
    }
    if (confirmar) {
        pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion(
            "Crear semestre $nombre", "Se cerrará el semestre actual y se vaciarán todas las habitaciones. ¿Continuar?", "Crear",
            {
                confirmar = false
                alcance.launch {
                    ejecutarLlamada { api.crearSemestre(NuevoSemestreAdmin(nombre.trim(), inicio, fin)) }
                        .onSuccess { Mensajes.exito("Semestre ${nombre.trim()} creado exitosamente"); creando = false; nombre = ""; inicio = ""; fin = ""; errores = emptyMap(); error = null; reintento++ }
                        .onFailure { e -> val x = e.aErrorApi(); if (x is ErrorApi.Validacion) errores = x.porCampo else error = x }
                }
            },
            { confirmar = false }, destructivo = true,
        )
    }
}
