package pe.upeu.resi.resiup.presentation.residente

import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.perfil.VerificarCorreoDialogo
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.TarjetaHero
import pe.upeu.resi.resiup.presentation.components.TarjetaHola
import pe.upeu.resi.resiup.presentation.components.TarjetaRanking
import pe.upeu.resi.resiup.presentation.components.TarjetaDato
import pe.upeu.resi.resiup.presentation.components.ModuloUi
import pe.upeu.resi.resiup.presentation.components.EncabezadoSeccion
import pe.upeu.resi.resiup.presentation.components.CuadriculaModulos
import pe.upeu.resi.resiup.presentation.components.AvatarInicial
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.domain.model.Sesion
import pe.upeu.resi.resiup.platform.crearMatrizQr
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.CalendarioAsistencias
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.util.hoyLima
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.components.QrVista
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.TarjetaEstadistica
import pe.upeu.resi.resiup.presentation.components.TarjetaVidrio
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.components.aEstadoVisual
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.navigation.Destinos
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiEstilos
import pe.upeu.resi.resiup.presentation.theme.ResiTheme
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoConsulta
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.formatoLima
import pe.upeu.resi.resiup.presentation.util.unDecimal
import pe.upeu.resi.resiup.presentation.util.Nota
import pe.upeu.resi.resiup.domain.model.ResumenNotas
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.NOMBRES_MES
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.size
import pe.upeu.resi.resiup.domain.model.CriterioLimpieza
import androidx.compose.ui.text.style.TextAlign
import kotlinx.datetime.LocalDate
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.presentation.components.FranjaSemana
import pe.upeu.resi.resiup.presentation.components.IconoAccion
import pe.upeu.resi.resiup.presentation.util.soloHoraLima

@Composable
fun InicioResidenteScreen(onIr: (String) -> Unit, vm: InicioResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    val notas by vm.notas.collectAsState()
    var verQr by remember { mutableStateOf(false) }
    ContenidoConsulta(estado, vm::reintentar) { datos ->
        val ui = datos ?: return@ContenidoConsulta IndicadorCarga()
        val e = ui.estadisticas
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TarjetaHola(
                ui.perfil.nombreCompleto,
                ui.perfil.habitacion?.let { listOfNotNull("Habitación ${it.numero}", it.etiquetaCama).joinToString(" · ") } ?: "Sin habitación asignada",
                Acento.Rosa, { onIr(Destinos.Perfil) }, Modifier.padding(top = 40.dp), onQr = { verQr = true },
                foto = koinViewModel<FotoPerfilViewModel>().foto.collectAsState().value,
            )
            val ranking = koinViewModel<RankingViewModel>().estado.collectAsState().value
            val r = (ranking as? Carga.Exito)?.datos
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                TarjetaRanking("Ranking en Cultos", r?.cultos?.posicion, r?.cultos?.total ?: 0, ResiIconos.Asistencias, Acento.Naranja, Modifier.weight(1f))
                TarjetaRanking("Ranking en Limpieza", r?.limpieza?.posicion, r?.limpieza?.total ?: 0, ResiIconos.Limpieza, Acento.Verde, Modifier.weight(1f))
            }
            EncabezadoSeccion("Accesos")
            CuadriculaModulos(
                listOf(
                    ModuloUi("Permisos", ResiIconos.Permisos, Acento.Azul, { onIr(Destinos.Permisos) }),
                    ModuloUi("Notas", ResiIconos.Notas, Acento.Violeta, { onIr(Destinos.Notas) }),
                    ModuloUi("Asistencias", ResiIconos.Calendario, Acento.Verde, { onIr(Destinos.Asistencias) }),
                    ModuloUi("Limpieza", ResiIconos.Limpieza, Acento.Naranja, { onIr(Destinos.Limpieza) }),
                ),
            )
        }
        if (verQr) QrFlotante({ verQr = false })
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PerfilResidenteScreen(sesion: Sesion?, onCerrarSesion: () -> Unit, vm: PerfilViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    val fotoVm = koinViewModel<FotoPerfilViewModel>()
    val foto by fotoVm.foto.collectAsState()
    val fotoEstado by fotoVm.estado.collectAsState()
    val elegirFoto = pe.upeu.resi.resiup.presentation.components.rememberSelectorImagen(fotoVm::elegida)
    var confirmar by remember { mutableStateOf(false) }
    var cambiandoClave by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(false) }
    BackHandler(enabled = editando) { editando = false }
    ContenidoConsulta(estado, vm::reintentar) { datos ->
        val perfil = datos ?: return@ContenidoConsulta IndicadorCarga()
        if (editando) {
            EditarPerfilScreen(perfil, onTerminar = { editando = false })
            return@ContenidoConsulta
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
            TituloPantalla("Mi Perfil", subtitulo = "Tus datos personales")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                AvatarInicial(perfil.nombreCompleto, Acento.Rosa, tamano = 112.dp, foto = foto)
                ResiBoton(if (fotoEstado.subiendo) "Subiendo…" else "Cambiar foto", elegirFoto, variante = VarianteBoton.Terciario, habilitado = !fotoEstado.subiendo)
                fotoEstado.error?.let { BannerMensaje(it.aMensaje(), EstadoVisual.Error) }
                Text(perfil.nombreCompleto, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            EncabezadoSeccion("Información personal")
            DatoPerfil("Nombre de usuario", sesion?.usuario, ResiIconos.Perfil, Acento.Azul)
            DatoPerfil("Código universitario", perfil.codigo, ResiIconos.Notas, Acento.Violeta)
            DatoPerfil(
                "Email", perfil.email, ResiIconos.Correo, Acento.Azul,
                final = if (perfil.email.isNullOrBlank()) null else { { ChipEstado(if (perfil.emailVerificado) "Verificado" else "Sin verificar", if (perfil.emailVerificado) EstadoVisual.Exito else EstadoVisual.Advertencia) } },
            )
            DatoPerfil("Celular", perfil.telefono, ResiIconos.Telefono, Acento.Verde)
            DatoPerfil("Carrera", perfil.carrera, ResiIconos.Perfil, Acento.Violeta)
            DatoPerfil("Ciclo", perfil.ciclo?.toString(), ResiIconos.Calendario, Acento.Turquesa)
            if (!perfil.email.isNullOrBlank() && !perfil.emailVerificado) {
                ResiBoton("Verificar mi correo", { verificando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            }
            ResiBoton("Editar correo y celular", { editando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            ResiBoton("Cambiar contraseña", { cambiandoClave = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            Text(
                "Tu nombre, usuario, código, carrera y ciclo los gestiona la administración de la residencia.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ResiBoton("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
        }
        if (verificando && perfil.email != null) {
            VerificarCorreoDialogo(perfil.email, { verificando = false }, { verificando = false; vm.reintentar() })
        }
    }
    if (cambiandoClave) pe.upeu.resi.resiup.presentation.clave.CambiarClaveDialogo(onCerrar = { cambiandoClave = false })
    if (confirmar) {
        DialogoConfirmacion("Cerrar sesión", "Se borrarán los datos guardados en este teléfono. ¿Deseas salir de ResiUP?", "Cerrar sesión", { confirmar = false; onCerrarSesion() }, { confirmar = false }, destructivo = true)
    }
}

/** Fila del perfil: ícono de color, etiqueta y valor; si falta el dato se invita a completarlo. [final] es un detalle al final de la fila. */
@Composable
internal fun DatoPerfil(
    etiqueta: String, valor: String?, icono: androidx.compose.ui.graphics.vector.ImageVector, acento: Acento,
    final: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = Espacio.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
        Box(Modifier.size(36.dp).background(acento.suave(), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            ResiIcono(icono, null, tint = acento.color(), tamano = 20.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (valor.isNullOrBlank()) Text("Sin completar", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Text(valor, style = MaterialTheme.typography.bodyLarge)
        }
        final?.invoke()
    }
}

@Composable
fun AsistenciasScreen(vm: AsistenciasViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    val hoy = remember { hoyLima() }
    var dia by remember { mutableStateOf(hoy) }
    var calendario by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            "Asistencias", Modifier.padding(top = Espacio.l), subtitulo = "Cultos de lunes a viernes",
            accion = { IconoAccion(ResiIconos.Calendario, "Abrir calendario", { calendario = true }) },
        )
        FranjaSemana(dia, { dia = it })
        Text("Historial del ${dia.toString().fechaLegible()}", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        ContenidoConsulta(estado, vm::reintentar, Modifier.weight(1f)) { lista ->
            if (dia > hoy || dia.dayOfWeek.ordinal >= 5) {
                Text("No hay cultos registrados", Modifier.fillMaxWidth().padding(top = Espacio.l), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    TipoCulto.entries.forEach { tipo -> CultoDelDia(tipo, dia, lista.firstOrNull { it.fecha == dia.toString() && it.tipo == tipo }) }
                }
            }
            if (calendario) {
                Dialog(onDismissRequest = { calendario = false }) {
                    CalendarioAsistencias(lista, hoy, onElegir = { dia = it; calendario = false })
                }
            }
        }
    }
}

/** Un culto del día: asistió, llegó tras el corte o no tiene registro. */
@Composable
private fun CultoDelDia(tipo: TipoCulto, dia: LocalDate, a: AsistenciaCulto?) {
    val fecha = dia.toString().fechaLegible()
    ResiTarjeta {
        ElementoLista(
            "Culto ${tipo.etiqueta.lowercase()}",
            subtitulo = if (a != null) "$fecha - ${a.registradoEn.soloHoraLima()}" else "$fecha - Sin registro",
            icono = if (a != null) ResiIconos.Asistencias else ResiIconos.Historial,
            acento = if (a != null) Acento.Verde else null,
            final = {
                when {
                    a == null -> ChipEstado("Sin registro", EstadoVisual.Neutro)
                    a.despuesDelCorte -> ChipEstado("Tras el corte", EstadoVisual.Advertencia)
                    else -> ChipEstado("Asistió", EstadoVisual.Exito)
                }
            },
        )
    }
}

@Composable
fun LimpiezaResidenteScreen(vm: LimpiezaResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    val hoy = remember { hoyLima() }
    var dia by remember { mutableStateOf(hoy) }
    var calendario by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla(
            "Limpieza", Modifier.padding(top = Espacio.l), subtitulo = "Evaluación individual sobre 20 puntos",
            accion = { IconoAccion(ResiIconos.Calendario, "Abrir calendario", { calendario = true }) },
        )
        FranjaSemana(dia, { dia = it })
        Text("Evaluaciones del ${dia.toString().fechaLegible()}", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        ContenidoConsulta(estado, vm::reintentar, Modifier.weight(1f)) { ui ->
            val evaluacion = ui.evaluaciones.firstOrNull { it.fecha == dia.toString() }
            if (evaluacion == null) {
                Text("Todavía no tienes evaluaciones", Modifier.fillMaxWidth().padding(top = Espacio.l), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    CriterioLimpieza.entries.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                            fila.forEach { c -> PuntajeCriterio(c, evaluacion.puntajes[c], Modifier.weight(1f)) }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Total: ${evaluacion.total}/${evaluacion.maximo}", Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    evaluacion.observacion?.let { Text("“$it”", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (calendario) {
                Dialog(onDismissRequest = { calendario = false }) {
                    CalendarioAsistencias(emptyList(), hoy, onElegir = { dia = it; calendario = false })
                }
            }
        }
    }
}

/** Un criterio con su nota: verde si es la máxima, rojo si es menor. */
@Composable
private fun PuntajeCriterio(c: CriterioLimpieza, puntaje: Int?, modifier: Modifier = Modifier) {
    val (nombre, icono) = when (c) {
        CriterioLimpieza.Cama -> "Cama" to ResiIconos.Cama
        CriterioLimpieza.Ropero -> "Ropero" to ResiIconos.Ropero
        CriterioLimpieza.Escritorio -> "Escritorio" to ResiIconos.Escritorio
        CriterioLimpieza.Basura -> "Basura" to ResiIconos.Basura
        CriterioLimpieza.Olores -> "Olores" to ResiIconos.Olores
        CriterioLimpieza.Piso -> "Piso" to ResiIconos.Piso
    }
    val colores = pe.upeu.resi.resiup.presentation.theme.ResiTheme.colores
    val bueno = puntaje != null && puntaje >= c.maximo
    ResiTarjeta(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                ResiIcono(icono, null, tint = MaterialTheme.colorScheme.primary, tamano = 22.dp)
            }
            Text(nombre, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Box(
                Modifier.background(if (bueno) colores.exitoContenedor else MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                    .semantics { contentDescription = "$nombre: ${puntaje ?: "sin nota"} de ${c.maximo}" },
                contentAlignment = Alignment.Center,
            ) {
                Text(puntaje?.toString() ?: "-", style = MaterialTheme.typography.titleMedium, color = if (bueno) colores.exito else MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun BarrasEvolucion(valores: List<Int>, maximo: Int) {
    Row(Modifier.fillMaxWidth().height(110.dp).padding(top = Espacio.m), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        valores.forEach { v ->
            Box(
                Modifier.weight(1f).height((100 * v / maximo.toFloat()).coerceAtLeast(4f).dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
            )
        }
    }
}
