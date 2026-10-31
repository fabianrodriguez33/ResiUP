package pe.upeu.resi.resiup.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import pe.upeu.resi.resiup.data.local.AlmacenSeguro

/**
 * Keychain de iOS. NO VERIFICADO: este módulo no se puede compilar ni ejecutar en Windows (requiere macOS/Xcode).
 * Revisar y probar en un Mac antes de afirmar que iOS funciona.
 */
@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos : AlmacenSeguro {

    private fun consultaBase() = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr).also {
        CFDictionaryAddValue(it, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(it, kSecAttrService, CFBridgingRetain(SERVICIO))
        CFDictionaryAddValue(it, kSecAttrAccount, CFBridgingRetain(CUENTA))
    }

    override fun guardar(valor: String) {
        borrar()
        val datos = (valor as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val consulta = consultaBase()
        CFDictionaryAddValue(consulta, kSecValueData, CFBridgingRetain(datos))
        CFDictionaryAddValue(consulta, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(consulta, null)
    }

    override fun leer(): String? = memScoped {
        val consulta = consultaBase()
        CFDictionaryAddValue(consulta, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
        val resultado = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(consulta, resultado.ptr) != errSecSuccess) return null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun borrar() {
        SecItemDelete(consultaBase())
    }

    private companion object {
        const val SERVICIO = "pe.upeu.resi.resiup"
        const val CUENTA = "sesion"
    }
}
