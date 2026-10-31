package pe.upeu.resi.resiup.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.AlmacenSeguroIos
import pe.upeu.resi.resiup.data.local.AlmacenSeguro

actual fun plataformaModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
}
