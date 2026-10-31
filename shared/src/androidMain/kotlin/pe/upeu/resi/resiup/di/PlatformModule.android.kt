package pe.upeu.resi.resiup.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.AlmacenSeguroAndroid
import pe.upeu.resi.resiup.data.local.AlmacenSeguro

actual fun plataformaModule(): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
}
