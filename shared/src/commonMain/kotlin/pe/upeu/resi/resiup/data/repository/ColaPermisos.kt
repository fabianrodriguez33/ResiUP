package pe.upeu.resi.resiup.data.repository

import kotlin.random.Random
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import pe.upeu.resi.resiup.data.local.DatosLocales
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.PermisoPendiente
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.login.aMensaje

/**
 * Permisos que el residente pide sin conexión. Quedan guardados en el teléfono como «pendientes de envío» y se mandan solos
 * cuando vuelve la red. Cada uno lleva una clave propia: si un envío se corta a medias y se repite, el servidor no lo duplica.
 * Si el servidor lo rechaza (por ejemplo, la hora de salida ya pasó) queda marcado con el motivo para que el residente
 * lo descarte y pida otro.
 */
class ColaPermisos(
    private val locales: DatosLocales,
    private val permisos: PermisoRepository,
    private val ahoraMillis: () -> Long,
    private val alEnviar: () -> Unit = {},
) {
    fun pendientes(): Flow<List<PermisoPendiente>> = locales.pendientes()

    /** Clave única para una solicitud: se usa desde el primer intento, así un envío cortado a medias no se duplica al reintentar. */
    fun nuevaClave(): String = "app-" + Random.nextLong().toULong().toString(16) + ahoraMillis().toString(16)

    fun encolar(n: NuevaSolicitud) {
        val clave = n.claveCliente ?: nuevaClave()
        locales.guardarPendiente(PermisoPendiente(clave, n.tipo, n.motivo, n.destino, n.salida, n.retorno, Instant.fromEpochMilliseconds(ahoraMillis())))
    }

    fun descartar(clave: String) = locales.borrarPendiente(clave)

    /** Envía en orden los pendientes sin error. Se detiene al primer fallo de red para reintentar más tarde. @return cuántos se enviaron. */
    suspend fun enviarPendientes(): Int {
        var enviados = 0
        for (p in locales.pendientesPorEnviar()) {
            val resultado = permisos.crear(NuevaSolicitud(p.tipo, p.motivo, p.destino, p.salida, p.retorno, claveCliente = p.clave, creadaEn = p.creadaEn))
            val error = resultado.exceptionOrNull()?.aErrorApi()
            when {
                resultado.isSuccess -> { locales.borrarPendiente(p.clave); enviados++ }
                error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado || error == ErrorApi.Servidor || error == ErrorApi.SesionExpirada -> break
                error != null -> locales.marcarErrorPendiente(p.clave, error.aMensaje())
            }
        }
        if (enviados > 0) alEnviar()
        return enviados
    }

    /** Mientras la app está abierta, reintenta cada [cadaMs] si hay algo pendiente de enviar. */
    suspend fun vigilar(cadaMs: Long = 15_000) {
        while (true) {
            if (locales.pendientesPorEnviar().isNotEmpty()) enviarPendientes()
            delay(cadaMs)
        }
    }
}
