package pe.upeu.resi.resiup.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.AlmacenSeguroIos
import pe.upeu.resi.resiup.data.local.AlmacenSeguro
import pe.upeu.resi.resiup.db.ResiDb

actual fun plataformaModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<SqlDriver> { NativeSqliteDriver(ResiDb.Schema, "resiup.db") }
}
