package pe.upeu.resi.resiup.presentation.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository
import pe.upeu.resi.resiup.presentation.components.BannerSinConexion
import pe.upeu.resi.resiup.presentation.components.EstadoError
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.login.aMensaje
import pe.upeu.resi.resiup.presentation.theme.Espacio

data class EstadoSync(val sincronizando: Boolean = false, val error: ErrorApi? = null)

/**
 * Orquesta la sincronización de la copia local de consulta. Vive más que cualquier pantalla (su propio alcance),
 * evita ejecuciones simultáneas y limita la frecuencia de las automáticas.
 */
class Sincronizador(private val repo: ConsultasResidenteRepository, private val ahoraMillis: () -> Long) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _estado = MutableStateFlow(EstadoSync())
    val estado: StateFlow<EstadoSync> = _estado.asStateFlow()
    private var ultimoIntento: Long? = null

    /** [forzar] ignora el límite de frecuencia (reintento del usuario o tras cambiar datos). */
    fun refrescar(forzar: Boolean = false) {
        if (_estado.value.sincronizando) return
        val t = ahoraMillis()
        if (!forzar && _estado.value.error == null && ultimoIntento?.let { t - it < INTERVALO_MINIMO_MS } == true) return
        ultimoIntento = t
        _estado.value = EstadoSync(sincronizando = true, error = _estado.value.error)
        alcance.launch {
            repo.sincronizar()
                .onSuccess { _estado.value = EstadoSync() }
                .onFailure { _estado.value = EstadoSync(error = it.aErrorApi()) }
        }
    }

    fun reiniciar() {
        _estado.value = EstadoSync()
        ultimoIntento = null
    }

    private companion object {
        const val INTERVALO_MINIMO_MS = 15_000L
    }
}

data class AvisoSync(val sincronizando: Boolean, val error: ErrorApi?, val ultima: Instant)

/** Estado de una pantalla de consulta: siempre muestra lo guardado y avisa si está desactualizado. */
sealed interface Consulta<out T> {
    data object Cargando : Consulta<Nothing>
    data class Fallo(val error: ErrorApi) : Consulta<Nothing>
    data class Contenido<T>(val datos: T, val aviso: AvisoSync) : Consulta<T>
}

/**
 * Combina los datos locales con el estado de sincronización. Mientras nunca se haya completado una sincronización
 * no hay copia válida: se muestra «cargando» o, si falló, el error con reintento.
 */
fun <T> Flow<T>.comoConsulta(sync: Flow<EstadoSync>, ultima: Flow<Long?>): Flow<Consulta<T>> =
    combine(this, sync, ultima) { datos, s, u ->
        when {
            u == null && s.error != null && !s.sincronizando -> Consulta.Fallo(s.error)
            u == null -> Consulta.Cargando
            else -> Consulta.Contenido(datos, AvisoSync(s.sincronizando, s.error, Instant.fromEpochMilliseconds(u)))
        }
    }

@Composable
fun <T> ContenidoConsulta(estado: Consulta<T>, onReintentar: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable (T) -> Unit) {
    when (estado) {
        Consulta.Cargando -> IndicadorCarga(modifier)
        is Consulta.Fallo -> EstadoError(estado.error.aMensaje(), onReintentar, modifier)
        is Consulta.Contenido -> Column(modifier.fillMaxSize()) {
            val error = estado.aviso.error
            if (error != null) {
                val motivo = when (error) {
                    ErrorApi.SinConexion, ErrorApi.TiempoAgotado -> "Sin conexión."
                    else -> "No se pudo actualizar."
                }
                BannerSinConexion(Modifier.padding(horizontal = Espacio.l, vertical = Espacio.s), "$motivo Datos guardados el ${estado.aviso.ultima.formatoLima()}")
            }
            Box(Modifier.weight(1f)) { contenido(estado.datos) }
        }
    }
}
