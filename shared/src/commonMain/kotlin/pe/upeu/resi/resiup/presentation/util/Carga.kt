package pe.upeu.resi.resiup.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.presentation.components.EstadoError
import pe.upeu.resi.resiup.presentation.components.IndicadorCarga
import pe.upeu.resi.resiup.presentation.login.aMensaje

/** Estado de una pantalla que carga datos remotos. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Exito<T>(val datos: T) : Carga<T>
    data class Fallo(val error: ErrorApi) : Carga<Nothing>
}

fun Throwable.aErrorApi(): ErrorApi = (this as? ErrorApiException)?.error ?: ErrorApi.SinConexion

fun <T> Result<T>.aCarga(): Carga<T> = fold({ Carga.Exito(it) }, { Carga.Fallo(it.aErrorApi()) })

/** Muestra cargando, error con reintento o el contenido. */
@Composable
fun <T> ContenidoCarga(estado: Carga<T>, onReintentar: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable (T) -> Unit) {
    when (estado) {
        is Carga.Cargando -> IndicadorCarga(modifier)
        is Carga.Fallo -> EstadoError(estado.error.aMensaje(), onReintentar, modifier, titulo = estado.error.titulo())
        is Carga.Exito -> contenido(estado.datos)
    }
}

private fun ErrorApi.titulo() = when (this) {
    ErrorApi.SinConexion -> "Sin conexión"
    ErrorApi.SinPermiso -> "Sin permiso"
    else -> "No se pudo completar la acción"
}
