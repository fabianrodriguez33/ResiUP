package pe.upeu.resi.resiup.data.local

/**
 * Almacenamiento cifrado de la sesión. Android: EncryptedSharedPreferences (clave en Android Keystore);
 * iOS: Keychain. Nunca se usan preferencias en texto plano para tokens.
 */
interface AlmacenSeguro {
    fun guardar(valor: String)
    fun leer(): String?
    fun borrar()
}
