package pe.upeu.resi.resiup

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe instalarse antes de super.onCreate para sustituir el tema de arranque.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val esDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        setContent {
            App(modoDesarrollo = esDebug, onCabeceraDeMarca = { azul ->
                enableEdgeToEdge(
                    statusBarStyle = if (azul) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(modoDesarrollo = true)
}
