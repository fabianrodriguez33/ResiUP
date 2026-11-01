package pe.upeu.resi.resiup

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe instalarse antes de super.onCreate para sustituir el tema de arranque.
        var sesionResuelta = false
        installSplashScreen().setKeepOnScreenCondition { !sesionResuelta } // el splash dura hasta saber si hay sesión
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Android 13+ pide permiso para mostrar notificaciones; sin él los avisos solo se ven dentro de la app.
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val esDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        setContent {
            App(modoDesarrollo = esDebug, onSesionResuelta = { sesionResuelta = true }, onCabeceraDeMarca = { azul ->
                enableEdgeToEdge(
                    statusBarStyle = if (azul) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            })
        }
    }
}
