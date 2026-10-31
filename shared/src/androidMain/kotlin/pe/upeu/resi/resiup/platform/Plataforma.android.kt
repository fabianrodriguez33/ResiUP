package pe.upeu.resi.resiup.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.VarianteBoton

actual fun crearMatrizQr(contenido: String): List<List<Boolean>>? = runCatching {
    val m = QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0))
    List(m.height) { y -> List(m.width) { x -> m.get(x, y) } }
}.getOrNull()

@Composable
actual fun EscanerCamara(activo: Boolean, onCodigo: (String) -> Unit, modifier: Modifier) {
    val contexto = LocalContext.current
    var permitido by remember { mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var rechazado by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permitido = ok
        rechazado = !ok
    }

    if (!permitido) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ResiUP usa la cámara únicamente para leer el código QR del residente.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            ResiBoton(if (rechazado) "Volver a solicitar permiso" else "Permitir cámara", { pedirPermiso.launch(Manifest.permission.CAMERA) }, Modifier.padding(top = 16.dp))
            if (rechazado) {
                Text("El permiso fue denegado. Si el sistema ya no lo pide, actívalo en los ajustes de la aplicación.",
                    Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                ResiBoton("Abrir ajustes", {
                    contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null)))
                }, variante = VarianteBoton.Terciario)
            }
        }
        return
    }

    val ciclo = LocalLifecycleOwner.current
    val vista = remember { PreviewView(contexto) }
    val escaner = remember { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()) }

    DisposableEffect(activo, ciclo) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        var proveedor: ProcessCameraProvider? = null
        val ejecutor = ContextCompat.getMainExecutor(contexto)
        futuro.addListener({
            proveedor = futuro.get()
            if (activo) {
                val preview = Preview.Builder().build().also { it.surfaceProvider = vista.surfaceProvider }
                val analisis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { a ->
                    a.setAnalyzer(ejecutor) { proxy ->
                        val imagen = proxy.image
                        if (imagen == null) {
                            proxy.close()
                        } else {
                            escaner.process(InputImage.fromMediaImage(imagen, proxy.imageInfo.rotationDegrees))
                                .addOnSuccessListener { codigos -> codigos.firstNotNullOfOrNull { it.rawValue }?.let(onCodigo) }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                }
                runCatching {
                    proveedor?.unbindAll()
                    proveedor?.bindToLifecycle(ciclo, CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis)
                }
            }
        }, ejecutor)
        onDispose { proveedor?.unbindAll() }
    }

    AndroidView(factory = { vista }, modifier = modifier.fillMaxSize())
}
