package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.platform.crearMatrizQr
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.QrVista
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.Carga

/**
 * Ventana flotante con el QR personal, para mostrarlo desde cualquier pantalla sin salir de ella.
 * Con conexión se pide al servidor; sin red se muestra la última copia guardada en el teléfono.
 */
@Composable
fun QrFlotante(onCerrar: () -> Unit, vm: PerfilViewModel = koinViewModel()) {
    val qr by vm.qr.collectAsState()
    LaunchedEffect(Unit) { vm.cargarQr() }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(horizontal = Espacio.l).fillMaxWidth().widthIn(max = 420.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mi código QR", style = MaterialTheme.typography.headlineSmall)
                when (val q = qr) {
                    is Carga.Cargando -> Text("Cargando…", Modifier.padding(vertical = Espacio.xl), style = MaterialTheme.typography.bodyMedium)
                    is Carga.Fallo -> {
                        val sinRed = q.error == ErrorApi.SinConexion || q.error == ErrorApi.TiempoAgotado
                        BannerMensaje(
                            if (sinRed) "Aún no hay un QR guardado en este teléfono. Conéctate una vez para descargarlo; después se podrá mostrar sin internet." else q.error.aMensaje(),
                            EstadoVisual.Advertencia,
                        )
                        ResiBoton("Reintentar", vm::cargarQr, variante = VarianteBoton.Secundario)
                    }
                    is Carga.Exito -> {
                        val matriz = crearMatrizQr(q.datos.contenido)
                        if (matriz == null) {
                            BannerMensaje("No se pudo generar el QR en este dispositivo.", EstadoVisual.Advertencia)
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { QrVista(matriz, Modifier.widthIn(max = 320.dp).fillMaxWidth()) }
                            if (q.datos.guardado) {
                                BannerMensaje("Sin conexión: se muestra el QR guardado en este teléfono. Sigue siendo válido mientras no lo hayan regenerado.", EstadoVisual.Info)
                            }
                            Text(
                                "Preséntalo al pastor o en garita. Es personal: no lo compartas ni lo publiques.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                ResiBoton("Cerrar", onCerrar, Modifier.fillMaxWidth(), variante = VarianteBoton.Secundario)
            }
        }
    }
}
