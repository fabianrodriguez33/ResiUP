package pe.upeu.resi.resiup.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Clock
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.local.AlmacenSeguro
import pe.upeu.resi.resiup.data.local.SesionStore
import pe.upeu.resi.resiup.data.remote.AuthApi
import pe.upeu.resi.resiup.data.remote.ConfigEntorno
import pe.upeu.resi.resiup.data.remote.SesionEventos
import pe.upeu.resi.resiup.data.remote.crearHttpClient
import pe.upeu.resi.resiup.data.repository.AuthRepositoryImpl
import pe.upeu.resi.resiup.domain.repository.AuthRepository
import pe.upeu.resi.resiup.domain.usecase.CerrarSesionUseCase
import pe.upeu.resi.resiup.domain.usecase.IniciarSesionUseCase
import pe.upeu.resi.resiup.domain.usecase.ObtenerSesionGuardadaUseCase
import pe.upeu.resi.resiup.presentation.login.LoginViewModel
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion

/** Implementaciones de cada plataforma: motor HTTP y almacén seguro. */
expect fun plataformaModule(): Module

fun datosModule(config: ConfigEntorno) = module {
    single { config }
    single { SesionEventos() }
    single { SesionStore(get<AlmacenSeguro>()) }
    single<HttpClient> { crearHttpClient(get<HttpClientEngine>(), get(), get(), get()) }
    single { AuthApi(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) { Clock.System.now().toEpochMilliseconds() } }
}

val dominioModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { ObtenerSesionGuardadaUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}

val presentacionModule = module {
    single { GestorSesion(get(), get(), get()) }
    viewModelOf(::LoginViewModel)
}

fun initKoin(config: ConfigEntorno, declaracion: KoinAppDeclaration = {}): KoinApplication = startKoin {
    declaracion()
    modules(plataformaModule(), datosModule(config), dominioModule, presentacionModule)
}
