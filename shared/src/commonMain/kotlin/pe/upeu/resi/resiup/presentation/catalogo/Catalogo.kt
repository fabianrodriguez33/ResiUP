package pe.upeu.resi.resiup.presentation.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.BannerSinConexion
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.DialogoConfirmacion
import pe.upeu.resi.resiup.presentation.components.ElementoLista
import pe.upeu.resi.resiup.presentation.components.EstadoError
import pe.upeu.resi.resiup.presentation.components.EstadoVacio
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.ResiIconos
import pe.upeu.resi.resiup.presentation.components.ResiTarjeta
import pe.upeu.resi.resiup.presentation.components.SelectorFecha
import pe.upeu.resi.resiup.presentation.components.SelectorHora
import pe.upeu.resi.resiup.presentation.components.TarjetaEstadistica
import pe.upeu.resi.resiup.presentation.components.TarjetaVidrio
import pe.upeu.resi.resiup.presentation.components.TituloPantalla
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

/** Pantalla de apoyo para revisar el sistema de diseño. Solo accesible en modo desarrollo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogoScreen(onVolver: () -> Unit) {
    var dialogo by remember { mutableStateOf(false) }
    var fecha by remember { mutableStateOf(false) }
    var hora by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    val marca = ResiTheme.colores.marca

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(Espacio.l),
        verticalArrangement = Arrangement.spacedBy(Espacio.l),
    ) {
        TituloPantalla("Catálogo", subtitulo = "Sistema de diseño de ResiUP")
        ResiBoton("Volver", onVolver, variante = VarianteBoton.Terciario)

        Text("Botones", style = MaterialTheme.typography.titleMedium)
        ResiBoton("Primario", {}, Modifier.fillMaxWidth())
        ResiBoton("Secundario", {}, Modifier.fillMaxWidth(), VarianteBoton.Secundario)
        ResiBoton("Terciario", {}, Modifier.fillMaxWidth(), VarianteBoton.Terciario)
        ResiBoton("Destructivo", { dialogo = true }, Modifier.fillMaxWidth(), VarianteBoton.Destructivo)
        ResiBoton("Cargando", {}, Modifier.fillMaxWidth(), cargando = true)
        ResiBoton("Deshabilitado", {}, Modifier.fillMaxWidth(), habilitado = false)

        Text("Campos", style = MaterialTheme.typography.titleMedium)
        ResiCampoTexto(texto, { texto = it }, "Motivo", ayuda = "Texto de ayuda")
        ResiCampoTexto("", {}, "Con error", error = "Campo obligatorio")
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            ResiBoton("Fecha", { fecha = true }, variante = VarianteBoton.Secundario)
            ResiBoton("Hora", { hora = true }, variante = VarianteBoton.Secundario)
        }

        Text("Estados", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            ChipEstado("Aprobado", EstadoVisual.Exito)
            ChipEstado("Pendiente", EstadoVisual.Advertencia)
            ChipEstado("Rechazado", EstadoVisual.Error)
            ChipEstado("En revisión", EstadoVisual.Info)
            ChipEstado("Cancelado", EstadoVisual.Neutro)
        }
        BannerMensaje("Operación realizada correctamente", EstadoVisual.Exito)
        BannerMensaje("No se pudo guardar el cambio", EstadoVisual.Error)
        BannerSinConexion()

        Text("Tarjetas", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            TarjetaEstadistica("Promedio", "17,5", Modifier.weight(1f), "de 20 puntos")
            TarjetaEstadistica("Asistencias", "92%", Modifier.weight(1f))
        }
        ResiTarjeta {
            ElementoLista("Permiso de salida", subtitulo = "Hoy · 4:00 p. m.", icono = ResiIconos.Permisos, final = { ChipEstado("Aprobado", EstadoVisual.Exito) }, onClick = {})
        }
        Column(Modifier.fillMaxWidth().background(marca, MaterialTheme.shapes.large).padding(Espacio.l)) {
            TarjetaVidrio(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Espacio.l)) {
                    Text("Tarjeta de vidrio", style = MaterialTheme.typography.titleMedium)
                    Text("Superficie translúcida sobre fondo de marca", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("Vacío y error", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.height(220.dp)) { EstadoVacio("Sin registros", "Aún no hay elementos para mostrar.") }
        Column(Modifier.height(240.dp)) { EstadoError("Revisa tu conexión e inténtalo de nuevo.", onReintentar = {}) }
    }

    if (dialogo) {
        DialogoConfirmacion("Eliminar", "¿Seguro que deseas continuar?", "Eliminar", { dialogo = false }, { dialogo = false }, destructivo = true)
    }
    if (fecha) SelectorFecha({ fecha = false }, { fecha = false })
    if (hora) SelectorHora({ _, _ -> hora = false }, { hora = false })
}
