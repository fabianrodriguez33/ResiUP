package pe.upeu.resi.resiup

import android.app.Application
import android.content.pm.ApplicationInfo
import org.koin.android.ext.koin.androidContext
import pe.upeu.resi.resiup.data.remote.ConfigEntorno
import pe.upeu.resi.resiup.di.initKoin

class ResiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val esDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        initKoin(ConfigEntorno(apiUrl = BuildConfig.API_URL, depuracion = esDebug)) {
            androidContext(this@ResiApplication)
        }
    }
}
