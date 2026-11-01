package pe.upeu.resi.resiup.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Clave con la que se cifra la base de datos local (SQLCipher, AES‑256). Se genera una sola vez al azar y se guarda en
 * preferencias cifradas con una clave del Android Keystore: no sale del teléfono ni viaja con copias de seguridad.
 */
object ClaveBaseDatos {
    private const val CLAVE = "clave_base"

    fun obtener(contexto: Context): ByteArray {
        val prefs = EncryptedSharedPreferences.create(
            contexto,
            "base_segura",
            MasterKey.Builder(contexto).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.getString(CLAVE, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val nueva = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(CLAVE, Base64.encodeToString(nueva, Base64.NO_WRAP)).apply()
        return nueva
    }
}
