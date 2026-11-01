package pe.upeu.resi.resiup.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.AlmacenSeguroAndroid
import pe.upeu.resi.resiup.data.ClaveBaseDatos
import pe.upeu.resi.resiup.data.local.AlmacenSeguro
import pe.upeu.resi.resiup.db.ResiDb

/** Base local cifrada con SQLCipher. La anterior (sin cifrar) solo era una caché y se borra al actualizar. */
private fun baseCifrada(contexto: Context): SqlDriver {
    System.loadLibrary("sqlcipher")
    contexto.deleteDatabase("resiup.db")
    return AndroidSqliteDriver(ResiDb.Schema, contexto, "resiup-cifrada.db", SupportOpenHelperFactory(ClaveBaseDatos.obtener(contexto)))
}

actual fun plataformaModule(): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<pe.upeu.resi.resiup.data.local.AvisosDelSistema> { pe.upeu.resi.resiup.data.AvisosAndroid(androidContext()) }
    single<SqlDriver> { baseCifrada(androidContext()) }
}
