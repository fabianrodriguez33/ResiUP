package pe.upeu.resi.resiup.presentation.residente

import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.perfil.VerificarCorreoDialogo
import pe.upeu.resi.resiup.presentation.components.ResiIcono
import pe.upeu.resi.resiup.presentation.components.TarjetaHero
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
import pe.upeu.resi.resiup.presentation.util.NotasMes
import pe.upeu.resi.resiup.presentation.util.NOMBRES_MES
import androidx.compose.foundation.layout.Spacer

@Composable
fun InicioResidenteScreen(onIr: (String) -> Unit, vm: InicioResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    var verQr by remember { mutableStateOf(false) }
    ContenidoConsulta(estado, vm::reintentar) { datos ->
        val ui = datos ?: return@ContenidoConsulta IndicadorCarga()
        val e = ui.estadisticas
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            Row(Modifier.padding(end = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                AvatarInicial(ui.perfil.nombreCompleto, Acento.Rosa)
                Column(Modifier.weight(1f)) {
                    Text("Hola, ${ui.perfil.nombreCompleto.substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        ui.perfil.habitacion?.let { listOfNotNull("Habitación ${it.numero}", it.etiquetaCama).joinToString(" · ") } ?: "Sin habitación asignada",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TarjetaHero("Mi código QR", "Para culto y garita", "Mostrar", { verQr = true }, ResiIconos.Qr)
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                TarjetaDato(e.racha.toString(), "Racha de cultos", ResiIconos.Racha, Acento.Naranja, Modifier.weight(1f), detalle = if (e.racha == 1) "día seguido" else "días seguidos")
                TarjetaDato("${e.diasAsistidosMes}/${e.diasHabilesMes}", if (e.delMesAnterior) "Mes pasado" else "Este mes", ResiIconos.Asistencias, Acento.Verde, Modifier.weight(1f), detalle = "días con culto")
                TarjetaDato(ui.limpieza?.promedio?.let { it.unDecimal() } ?: "—", "Limpieza", ResiIconos.Limpieza, Acento.Violeta, Modifier.weight(1f), detalle = "promedio sobre 20")
            }
            EncabezadoSeccion("Mis notas")
            ResiTarjeta {
                NotasDelMes(ui.notasMes, enCurso = true)
                Spacer(Modifier.height(Espacio.m))
                NotasDelMes(ui.notasMesAnterior, enCurso = false)
            }
            EncabezadoSeccion("Accesos")
            CuadriculaModulos(
                listOf(
                    ModuloUi("Mi QR", ResiIconos.Qr, Acento.Violeta, { verQr = true }),
                    ModuloUi("Permisos", ResiIconos.Permisos, Acento.Azul, { onIr(Destinos.Permisos) }),
                    ModuloUi("Asistencias", ResiIconos.Calendario, Acento.Verde, { onIr(Destinos.Asistencias) }),
                    ModuloUi("Limpieza", ResiIconos.Limpieza, Acento.Naranja, { onIr(Destinos.Limpieza) }),
                    ModuloUi("Mi perfil", ResiIconos.Perfil, Acento.Rosa, { onIr(Destinos.Perfil) }),
                    ModuloUi("Salidas y retornos", ResiIconos.Salir, Acento.Turquesa, { onIr(Destinos.Permisos) }),
                ),
            )
            EncabezadoSeccion("Lo último")
            ResiTarjeta(onClick = { onIr(Destinos.Permisos) }) {
                val p = ui.permisoReciente
                if (p == null) {
                    ElementoLista("Permisos", subtitulo = "Aún no has solicitado permisos", icono = ResiIconos.Permisos, acento = Acento.Azul)
                } else {
                    ElementoLista(
                        p.destino, subtitulo = "${p.tipo.etiqueta} · ${p.salida.formatoLima()}", icono = ResiIconos.Permisos, acento = Acento.Azul,
                        final = { ChipEstado(p.estado.etiqueta, p.estado.aEstadoVisual()) },
                    )
                }
            }
            ResiTarjeta(onClick = { onIr(Destinos.Asistencias) }) {
                val a = ui.ultimaAsistencia
                ElementoLista(
                    if (a == null) "Cultos" else "Culto ${a.tipo.etiqueta.lowercase()}",
                    subtitulo = a?.fecha?.fechaLegible() ?: "Sin registros todavía", icono = ResiIconos.Asistencias, acento = Acento.Verde,
                    final = { if (a != null) ChipEstado(if (a.despuesDelCorte) "Tarde" else "Puntual", if (a.despuesDelCorte) EstadoVisual.Advertencia else EstadoVisual.Exito) },
                )
            }
        }
        if (verQr) QrFlotante({ verQr = false })
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PerfilResidenteScreen(sesion: Sesion?, onCerrarSesion: () -> Unit, vm: PerfilViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    var confirmar by remember { mutableStateOf(false) }
    var cambiandoClave by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf(false) }
    var verQr by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(false) }
    BackHandler(enabled = editando) { editando = false }
    ContenidoConsulta(estado, vm::reintentar) { datos ->
        val perfil = datos ?: return@ContenidoConsulta IndicadorCarga()
        if (editando) {
            EditarPerfilScreen(perfil, onTerminar = { editando = false })
            return@ContenidoConsulta
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            Row(Modifier.padding(end = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                AvatarInicial(perfil.nombreCompleto, Acento.Rosa, tamano = 60.dp)
                Column(Modifier.weight(1f)) {
                    Text(perfil.nombreCompleto, style = MaterialTheme.typography.headlineMedium)
                    Text("Código ${perfil.codigo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChipEstado(Rol.Residente.etiqueta, EstadoVisual.Info, Modifier.padding(top = Espacio.xs))
                }
            }
            ResiBoton("Mostrar mi código QR", { verQr = true }, Modifier.fillMaxWidth())
            ResiTarjeta {
                DatoPerfil("DNI", perfil.dni, ResiIconos.Perfil, Acento.Azul)
                DatoPerfil("Fecha de nacimiento", perfil.fechaNacimiento?.fechaLegible(), ResiIconos.Calendario, Acento.Rosa)
                DatoPerfil("Carrera", perfil.carrera, ResiIconos.Perfil, Acento.Violeta)
                DatoPerfil("Ciclo", perfil.ciclo?.let { "${it}.º ciclo" }, ResiIconos.Calendario, Acento.Turquesa)
                DatoPerfil(
                    "Correo electrónico", perfil.email, ResiIconos.Correo, Acento.Azul,
                    final = if (perfil.email.isNullOrBlank()) null else { { ChipEstado(if (perfil.emailVerificado) "Verificado" else "Sin verificar", if (perfil.emailVerificado) EstadoVisual.Exito else EstadoVisual.Advertencia) } },
                )
                DatoPerfil("Teléfono", perfil.telefono, ResiIconos.Telefono, Acento.Verde)
                DatoPerfil("Habitación", perfil.habitacion?.let { listOfNotNull(it.numero, it.etiquetaCama).joinToString(" · ") }, ResiIconos.Inicio, Acento.Naranja)
            }
            if (!perfil.email.isNullOrBlank() && !perfil.emailVerificado) {
                ResiBoton("Verificar mi correo", { verificando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            }
            ResiBoton("Editar perfil", { editando = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            ResiBoton("Cambiar contraseña", { cambiandoClave = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            Text(
                "El nombre, el DNI y el código universitario los gestiona la administración de la residencia.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sesion?.let { Text("Sesión: ${it.usuario}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            ResiBoton("Cerrar sesión", { confirmar = true }, Modifier.fillMaxWidth(), variante = VarianteBoton.Destructivo)
        }
        if (verQr) QrFlotante({ verQr = false })
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
    Column(Modifier.fillMaxSize()) {
        TituloPantalla("Asistencias", Modifier.padding(horizontal = Espacio.l, vertical = Espacio.l), subtitulo = "Cultos de lunes a viernes")
        ContenidoConsulta(estado, vm::reintentar, Modifier.weight(1f)) { lista ->
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = Espacio.l), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                item { CalendarioAsistencias(lista, hoyLima()) }
                if (lista.isEmpty()) {
                    item { Text("Cuando el preceptor registre tu asistencia a un culto aparecerá en el calendario.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    item { Text("Historial reciente", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground) }
                    items(lista) { a -> ResiTarjeta { AsistenciaItem(a) } }
                }
            }
        }
    }
}

@Composable
private fun AsistenciaItem(a: AsistenciaCulto) {
    ElementoLista(
        "Culto ${a.tipo.etiqueta.lowercase()}", subtitulo = "${a.fecha.fechaLegible()} · registrado ${a.registradoEn.formatoLima()}", icono = ResiIconos.Asistencias, acento = Acento.Verde,
        final = { if (a.despuesDelCorte) ChipEstado("Después del corte", EstadoVisual.Advertencia) else ChipEstado("Registrada", EstadoVisual.Exito) },
    )
}

@Composable
fun LimpiezaResidenteScreen(vm: LimpiezaResidenteViewModel = koinViewModel()) {
    val estado by vm.estado.collectAsState()
    ContenidoConsulta(estado, vm::reintentar) { ui ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            TituloPantalla("Limpieza", subtitulo = "Evaluación individual sobre 20 puntos")
            if (ui.evaluaciones.isEmpty()) {
                ResiTarjeta { Text("Todavía no tienes evaluaciones. Aparecerán aquí cuando el preceptor las registre.", style = MaterialTheme.typography.bodyLarge) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    TarjetaEstadistica("Promedio", ui.resumen.promedio?.unDecimal() ?: "—", Modifier.weight(1f), "de 20")
                    TarjetaEstadistica("Evaluaciones", ui.resumen.evaluaciones.toString(), Modifier.weight(1f), ui.resumen.ultimaFecha?.let { "última ${it.fechaLegible()}" })
                }
                if (ui.evaluaciones.size >= 2) {
                    ResiTarjeta {
                        Text("Evolución", style = MaterialTheme.typography.titleMedium)
                        BarrasEvolucion(ui.evaluaciones.take(10).reversed().map { it.total }, maximo = ui.evaluaciones.first().maximo)
                    }
                }
                ui.evaluaciones.forEach { e ->
                    ResiTarjeta {
                        ElementoLista("${e.total}/${e.maximo}", subtitulo = e.fecha.fechaLegible(), icono = ResiIconos.Limpieza)
                        Text(
                            e.puntajes.entries.joinToString(" · ") { "${it.key.etiqueta} ${it.value}/${it.key.maximo}" },
                            Modifier.padding(top = Espacio.s), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        e.observacion?.let { Text("“$it”", Modifier.padding(top = Espacio.xs), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
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

private fun Nota.estado() = when (this) {
    Nota.AD -> EstadoVisual.Exito
    Nota.A -> EstadoVisual.Info
    Nota.B -> EstadoVisual.Advertencia
    Nota.C -> EstadoVisual.Error
}

/** Resumen de un mes: cuántos cultos y cómo le fue en limpieza, cada uno con su nota (AD, A, B o C). */
@Composable
private fun NotasDelMes(n: NotasMes, enCurso: Boolean) {
    val mes = NOMBRES_MES[n.mes - 1].replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text("$mes ${n.anio}" + if (enCurso) " · en curso" else "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            ChipEstado("Cultos ${n.cultos} · ${n.notaCultos.name}", n.notaCultos.estado())
            if (n.notaLimpieza == null || n.promedioLimpieza == null) ChipEstado("Limpieza · sin evaluar", EstadoVisual.Neutro)
            else ChipEstado("Limpieza ${n.promedioLimpieza.unDecimal()} · ${n.notaLimpieza.name}", n.notaLimpieza.estado())
        }
    }
}
