package pe.upeu.resi.resiup

import androidx.compose.ui.window.ComposeUIViewController
import pe.upeu.resi.resiup.data.remote.ConfigEntorno
import pe.upeu.resi.resiup.di.initKoin

private var koinIniciado = false

// URL del simulador de iOS (comparte red con el Mac). Para un iPhone físico use la IP del Mac y HTTPS/ATS (pendiente, sin Mac para probar).
fun MainViewController() = ComposeUIViewController(configure = {
    if (!koinIniciado) {
        initKoin(ConfigEntorno(apiUrl = "http://localhost:8080/", depuracion = true))
        koinIniciado = true
    }
}) { App(modoDesarrollo = false) }

