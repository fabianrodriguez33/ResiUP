package pe.upeu.resi.resiup.di

import io.ktor.client.HttpClient
import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Clock
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.upeu.resi.resiup.data.local.AlmacenSeguro
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.db.ResiDb
import pe.upeu.resi.resiup.data.repository.ConsultasResidenteRepositoryImpl
import pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository
import pe.upeu.resi.resiup.presentation.util.Sincronizador
import pe.upeu.resi.resiup.data.local.SesionStore
import pe.upeu.resi.resiup.data.remote.AuthApi
import pe.upeu.resi.resiup.data.remote.CultoApi
import pe.upeu.resi.resiup.data.remote.GaritaApi
import pe.upeu.resi.resiup.data.remote.FamiliaApi
import pe.upeu.resi.resiup.data.remote.LimpiezaApi
import pe.upeu.resi.resiup.data.remote.PermisoApi
import pe.upeu.resi.resiup.data.remote.ResidenteApi
import pe.upeu.resi.resiup.data.repository.CultoRepositoryImpl
import pe.upeu.resi.resiup.data.repository.GaritaRepositoryImpl
import pe.upeu.resi.resiup.data.repository.FamiliaRepositoryImpl
import pe.upeu.resi.resiup.data.repository.LimpiezaRepositoryImpl
import pe.upeu.resi.resiup.data.repository.PermisoRepositoryImpl
import pe.upeu.resi.resiup.data.repository.ResidenteRepositoryImpl
import pe.upeu.resi.resiup.domain.repository.CultoRepository
import pe.upeu.resi.resiup.domain.repository.GaritaRepository
import pe.upeu.resi.resiup.domain.repository.FamiliaRepository
import pe.upeu.resi.resiup.domain.repository.LimpiezaRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.domain.repository.ResidenteRepository
import pe.upeu.resi.resiup.presentation.personal.AsistenciaCultoViewModel
import pe.upeu.resi.resiup.presentation.personal.DiasSinCultoViewModel
import pe.upeu.resi.resiup.presentation.perfil.PerfilCuentaViewModel
import pe.upeu.resi.resiup.presentation.perfil.VerificacionCorreoViewModel
import pe.upeu.resi.resiup.presentation.personal.GaritaEscanerViewModel
import pe.upeu.resi.resiup.presentation.personal.HistorialGaritaViewModel
import pe.upeu.resi.resiup.presentation.personal.InicioPreceptorViewModel
import pe.upeu.resi.resiup.presentation.personal.LimpiezaPreceptorViewModel
import pe.upeu.resi.resiup.presentation.familia.FamiliaDetalleViewModel
import pe.upeu.resi.resiup.presentation.familia.FamiliaInicioViewModel
import pe.upeu.resi.resiup.presentation.familia.FamiliaNuevoViewModel
import pe.upeu.resi.resiup.presentation.familia.FamiliaSolicitudesViewModel
import pe.upeu.resi.resiup.presentation.personal.SolicitudRevisionViewModel
import pe.upeu.resi.resiup.presentation.personal.PermisosAprobadosViewModel
import pe.upeu.resi.resiup.presentation.personal.PermisosEnEsperaViewModel
import pe.upeu.resi.resiup.presentation.personal.PermisosTodosViewModel
import pe.upeu.resi.resiup.presentation.residente.EditarPerfilViewModel
import pe.upeu.resi.resiup.presentation.residente.AsistenciasViewModel
import pe.upeu.resi.resiup.presentation.residente.InicioResidenteViewModel
import pe.upeu.resi.resiup.presentation.residente.LimpiezaResidenteViewModel
import pe.upeu.resi.resiup.presentation.residente.MisSolicitudesViewModel
import pe.upeu.resi.resiup.presentation.residente.MovimientosViewModel
import pe.upeu.resi.resiup.presentation.residente.PerfilViewModel
import pe.upeu.resi.resiup.presentation.residente.SolicitudDetalleViewModel
import pe.upeu.resi.resiup.presentation.residente.SolicitudFormViewModel
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
    single { ResidenteApi(get()) }
    single { PermisoApi(get()) }
    single { CultoApi(get()) }
    single { LimpiezaApi(get()) }
    single { FamiliaApi(get()) }
    single { GaritaApi(get()) }
    single { pe.upeu.resi.resiup.data.remote.PerfilCuentaApi(get()) }
    single { pe.upeu.resi.resiup.data.remote.CalendarioApi(get()) }
    single<ResidenteRepository> { ResidenteRepositoryImpl(get(), get()) }
    single<PermisoRepository> { PermisoRepositoryImpl(get()) }
    single<CultoRepository> { CultoRepositoryImpl(get()) }
    single<LimpiezaRepository> { LimpiezaRepositoryImpl(get()) }
    single<FamiliaRepository> { FamiliaRepositoryImpl(get()) }
    single<GaritaRepository> { GaritaRepositoryImpl(get()) }
    single<pe.upeu.resi.resiup.domain.repository.PerfilCuentaRepository> { pe.upeu.resi.resiup.data.repository.PerfilCuentaRepositoryImpl(get()) }
    single<pe.upeu.resi.resiup.domain.repository.CalendarioRepository> { pe.upeu.resi.resiup.data.repository.CalendarioRepositoryImpl(get()) }
    single { ResiDb(get<SqlDriver>()) }
    single { DatosLocales(get()) }
    single<ConsultasResidenteRepository> {
        ConsultasResidenteRepositoryImpl(get(), get(), get(), get()) { Clock.System.now().toEpochMilliseconds() }
    }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) { Clock.System.now().toEpochMilliseconds() } }
}

val dominioModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { ObtenerSesionGuardadaUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}

val presentacionModule = module {
    single { Sincronizador(get()) { Clock.System.now().toEpochMilliseconds() } }
    single {
        val consultas = get<ConsultasResidenteRepository>()
        val sincronizador = get<Sincronizador>()
        GestorSesion(
            get(), get(), get(),
            alIniciar = { consultas.asegurarPropietario(it.usuario) },
            alCerrar = { consultas.borrarTodo(); sincronizador.reiniciar() },
        )
    }
    viewModelOf(::LoginViewModel)
    // Residente
    viewModelOf(::InicioResidenteViewModel)
    viewModelOf(::PerfilViewModel)
    viewModelOf(::AsistenciasViewModel)
    viewModelOf(::LimpiezaResidenteViewModel)
    viewModelOf(::MisSolicitudesViewModel)
    viewModelOf(::MovimientosViewModel)
    viewModel { SolicitudFormViewModel(get(), get()) { get<Sincronizador>().refrescar(forzar = true) } }
    viewModelOf(::FamiliaInicioViewModel)
    viewModelOf(::FamiliaSolicitudesViewModel)
    viewModelOf(::FamiliaNuevoViewModel)
    viewModel { (id: Long) -> FamiliaDetalleViewModel(id, get()) }
    viewModel { (id: Long) -> SolicitudDetalleViewModel(id, get()) { get<Sincronizador>().refrescar(forzar = true) } }
    // Preceptor
    viewModelOf(::InicioPreceptorViewModel)
    viewModelOf(::AsistenciaCultoViewModel)
    viewModelOf(::PermisosAprobadosViewModel)
    viewModelOf(::PermisosEnEsperaViewModel)
    viewModelOf(::PermisosTodosViewModel)
    viewModelOf(::EditarPerfilViewModel)
    // Cuenta de cualquier rol
    viewModelOf(::PerfilCuentaViewModel)
    viewModelOf(::VerificacionCorreoViewModel)
    viewModelOf(::DiasSinCultoViewModel)
    viewModelOf(::LimpiezaPreceptorViewModel)
    viewModel { (id: Long) -> SolicitudRevisionViewModel(id, get()) }
    // Garita
    viewModelOf(::GaritaEscanerViewModel)
    viewModelOf(::HistorialGaritaViewModel)
}

fun initKoin(config: ConfigEntorno, declaracion: KoinAppDeclaration = {}): KoinApplication = startKoin {
    declaracion()
    modules(plataformaModule(), datosModule(config), dominioModule, presentacionModule)
}
