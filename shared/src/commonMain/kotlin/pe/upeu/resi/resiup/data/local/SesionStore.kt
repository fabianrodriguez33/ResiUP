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
)

/**
 * Persiste la [Sesion] cifrada y la mantiene en memoria para no descifrar en cada petición.
 * Si el contenido guardado está dañado o el rol ya no se reconoce, se descarta.
 */
class SesionStore(private val almacen: AlmacenSeguro) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cache: Sesion? = null
    private var cargada = false

    fun guardar(sesion: Sesion) {
        val s = SesionGuardada(sesion.token, sesion.usuario, sesion.nombreCompleto, sesion.rol.name.uppercase(), sesion.permisos.toList(), sesion.expiraEnMillis)
        almacen.guardar(json.encodeToString(SesionGuardada.serializer(), s))
        cache = sesion
        cargada = true
    }

    fun leer(): Sesion? {
        if (cargada) return cache
        cache = almacen.leer()?.let { texto ->
            runCatching { json.decodeFromString(SesionGuardada.serializer(), texto) }.getOrNull()?.let { g ->
                g.rol.aRol()?.let { rol -> Sesion(g.token, g.usuario, g.nombreCompleto, rol, g.permisos.toSet(), g.expiraEnMillis) }
            }
        }
        if (cache == null) almacen.borrar()
        cargada = true
        return cache
    }

    fun token(): String? = leer()?.token

    fun borrar() {
        almacen.borrar()
        cache = null
        cargada = true
    }
}
