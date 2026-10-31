package pe.upeu.resi.resiup.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.resi.resiup.data.local.AlmacenSeguro

/** Preferencias cifradas (AES‑256) con clave maestra en el Android Keystore. */
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {
    private val prefs = EncryptedSharedPreferences.create(
        contexto,
        "sesion_segura",
        MasterKey.Builder(contexto).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun guardar(valor: String) = prefs.edit().putString(CLAVE, valor).apply()

    // Si el almacén no se puede descifrar (p. ej. se perdió la clave), se trata como «sin sesión».
    override fun leer(): String? = runCatching { prefs.getString(CLAVE, null) }.getOrNull()

    override fun borrar() = prefs.edit().remove(CLAVE).apply()

    private companion object {
        const val CLAVE = "sesion"
    }
}
