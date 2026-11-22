package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.compose.koinInject
import pe.upeu.resi.resiup.domain.model.Ranking
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.components.FranjaSemana
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.components.TarjetaCifra
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.domingoDe
import pe.upeu.resi.resiup.presentation.theme.Acento
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga
import pe.upeu.resi.resiup.presentation.util.ContenidoCarga
import pe.upeu.resi.resiup.presentation.util.aCarga
import pe.upeu.resi.resiup.presentation.util.fechaLegible
import pe.upeu.resi.resiup.presentation.util.hoyLima
import pe.upeu.resi.resiup.presentation.util.unDecimal

/** Ranking y notas: del semestre en curso o de cualquier semana (domingo a sábado). Se calcula en el servidor con los registros originales. */
@Composable
fun RankingSemanaScreen() {
    val api = koinInject<ResidenteRepository>()
    val hoy = remember { hoyLima() }
    var porSemana by remember { mutableStateOf(true) }
    var dia by remember { mutableStateOf(hoy) }
    var estado by remember { mutableStateOf<Carga<Ranking>>(Carga.Cargando) }
    LaunchedEffect(porSemana, dia) {
        estado = Carga.Cargando
        val domingo = domingoDe(dia)
        estado = (if (porSemana) api.rankingDe(domingo.toString(), domingo.plus(6, DateTimeUnit.DAY).toString()) else api.ranking()).aCarga()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        TituloPantalla("Ranking y notas", subtitulo = if (porSemana) "Elige una semana" else "Semestre en curso")
        Segmentos(listOf(true, false), porSemana, { if (it) "Semana" else "Semestre" }, { porSemana = it })
        if (porSemana) FranjaSemana(dia, { dia = it })
        ContenidoCarga(estado, { dia = dia }, Modifier.fillMaxSize()) { r ->
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    TarjetaCifra("Cultos", r.cultos.posicion?.let { "$it°" } ?: "—", "de ${r.cultos.total} residentes", ResiIconos.Cruz, Acento.Naranja,
                        "Ranking en cultos", Modifier.weight(1f))
                    TarjetaCifra("Limpieza", r.limpieza.posicion?.let { "$it°" } ?: "—", if (r.limpieza.posicion == null) "sin evaluaciones" else "de ${r.limpieza.total} residentes", ResiIconos.Asistencias, Acento.Verde,
                        "Ranking en limpieza", Modifier.weight(1f))
                }
                ResiTarjeta {
                    Text("Notas", style = MaterialTheme.typography.titleMedium)
                    Text("Cultos asistidos: ${r.cultos.valor?.toInt() ?: 0}", style = MaterialTheme.typography.bodyLarge)
                    Text(r.limpieza.valor?.let { "Promedio de limpieza: ${it.unDecimal()}/20" } ?: "Promedio de limpieza: sin evaluaciones", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
