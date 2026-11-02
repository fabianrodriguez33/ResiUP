package pe.upeu.resi.resiup.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pe.upeu.resi.resiup.data.remote.dto.aRol
import pe.upeu.resi.resiup.domain.model.Sesion

@Serializable
private data class SesionGuardada(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val rol: String,
    val permisos: List<String>,
    val expiraEnMillis: Long,
    val qr: String? = null,
    val debeCambiarClave: Boolean = false,
)

/**
 * Persiste la [Sesion] cifrada y la mantiene en memoria para no descifrar en cada petición.
 * Si el contenido guardado está dañado o el rol ya no se reconoce, se descarta.
 */
class SesionStore(private val almacen: AlmacenSeguro) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cache: Sesion? = null
    private var qrGuardado: String? = null
    private var cargada = false

    fun guardar(sesion: Sesion) {
        val s = SesionGuardada(sesion.token, sesion.usuario, sesion.nombreCompleto, sesion.rol.name.uppercase(), sesion.permisos.toList(), sesion.expiraEnMillis, null, sesion.debeCambiarClave)
        qrGuardado = null
        almacen.guardar(json.encodeToString(SesionGuardada.serializer(), s))
        cache = sesion
        cargada = true
    }

    fun leer(): Sesion? {
        if (cargada) return cache
        cache = almacen.leer()?.let { texto ->
            runCatching { json.decodeFromString(SesionGuardada.serializer(), texto) }.getOrNull()?.let { g ->
                qrGuardado = g.qr
                g.rol.aRol()?.let { rol -> Sesion(g.token, g.usuario, g.nombreCompleto, rol, g.permisos.toSet(), g.expiraEnMillis, g.debeCambiarClave) }
            }
        }
        if (cache == null) almacen.borrar()
        cargada = true
        return cache
    }

    /** QR personal cifrado junto a la sesión: se borra con ella (cierre, expiración o cambio de usuario). */
    fun guardarQr(contenido: String) {
        val sesion = leer() ?: return
        val s = SesionGuardada(sesion.token, sesion.usuario, sesion.nombreCompleto, sesion.rol.name.uppercase(), sesion.permisos.toList(), sesion.expiraEnMillis, contenido, sesion.debeCambiarClave)
        almacen.guardar(json.encodeToString(SesionGuardada.serializer(), s))
        qrGuardado = contenido
    }

    /** Ya eligió una contraseña nueva: se conserva todo lo demás de la sesión (incluido el QR guardado). */
    fun claveCambiada() {
        val sesion = leer() ?: return
        val nueva = sesion.copy(debeCambiarClave = false)
        val s = SesionGuardada(nueva.token, nueva.usuario, nueva.nombreCompleto, nueva.rol.name.uppercase(), nueva.permisos.toList(), nueva.expiraEnMillis, qrGuardado, false)
        almacen.guardar(json.encodeToString(SesionGuardada.serializer(), s))
        cache = nueva
    }

    fun qr(): String? = leer()?.let { qrGuardado }

    fun token(): String? = leer()?.token

    fun borrar() {
        almacen.borrar()
        cache = null
        qrGuardado = null
        cargada = true
    }
}
