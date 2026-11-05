package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.ResumenNotas
import pe.upeu.resi.resiup.presentation.components.BotonAtras
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.NOMBRES_MES
import pe.upeu.resi.resiup.presentation.util.Nota
import pe.upeu.resi.resiup.presentation.util.unDecimal

/** Notas del residente (mes en curso y mes anterior). Se abre desde los accesos del inicio; la flecha y el botón atrás regresan. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NotasResidenteScreen(onVolver: () -> Unit, vm: NotasResidenteViewModel = koinViewModel()) {
    val notas by vm.notas.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    BackHandler(onBack = onVolver)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
        Row(Modifier.padding(top = Espacio.s, end = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            BotonAtras(onVolver)
            Text("Notas", style = MaterialTheme.typography.headlineMedium)
        }
        when (val n = notas) {
            is Carga.Cargando -> IndicadorCarga()
            is Carga.Fallo -> ResiTarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Text(
                        if (n.error == ErrorApi.SinConexion) "Necesitas conexión para ver tus notas" else "No se pudieron cargar tus notas",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ResiBoton("Reintentar", vm::cargar, variante = VarianteBoton.Secundario)
                }
            }
            is Carga.Exito -> ResiTarjeta {
                NotasDelMes(n.datos.actual)
                Spacer(Modifier.height(Espacio.m))
                NotasDelMes(n.datos.anterior)
            }
        }
    }
}

private fun Nota.estado() = when (this) {
    Nota.AD -> EstadoVisual.Exito
    Nota.A -> EstadoVisual.Info
    Nota.B -> EstadoVisual.Advertencia
    Nota.C -> EstadoVisual.Error
}

private fun String.aNota(): Nota? = Nota.entries.firstOrNull { it.name == this }

/**
 * Resumen de un mes con los datos del servidor. Desde noviembre de 2026 los cultos van por días asistidos y porcentaje; mientras el
 * mes no termina la nota es «Nota a la fecha». Los meses anteriores muestran la escala antigua (cantidad de cultos).
 */
@Composable
private fun NotasDelMes(n: ResumenNotas) {
    val mes = NOMBRES_MES[n.mes - 1].replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text("$mes ${n.anio}" + if (n.provisional) " · en curso" else "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalAlignment = Alignment.CenterVertically) {
            val nota = n.notaCultos?.aNota()
            when {
                n.sinCultos || nota == null -> ChipEstado("Sin cultos en el mes", EstadoVisual.Neutro)
                n.porDias -> {
                    val pct = n.porcentajeAsistencia?.unDecimal() ?: "—"
                    ChipEstado("Cultos ${n.diasAsistidos}/${n.diasConCulto} días · $pct % · ${nota.name}", nota.estado())
                }
                else -> ChipEstado("Cultos ${n.cultos} · ${nota.name}", nota.estado())
            }
            val limpieza = n.notaLimpieza?.aNota()
            if (limpieza == null || n.promedioLimpieza == null) ChipEstado("Limpieza · sin evaluar", EstadoVisual.Neutro)
            else ChipEstado("Limpieza ${n.promedioLimpieza.unDecimal()} · ${limpieza.name}", limpieza.estado())
        }
        if (n.provisional && n.porDias && !n.sinCultos) {
            Text("Nota a la fecha: puede cambiar hasta que termine el mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
