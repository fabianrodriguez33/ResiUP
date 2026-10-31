package pe.upeu.resi.resiup.presentation.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val usuarioError: String? = null,
    val claveError: String? = null,
    val mensaje: String? = null,
)

class LoginViewModel : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun alCambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, usuarioError = null, mensaje = null) }
    fun alCambiarClave(valor: String) = _estado.update { it.copy(clave = valor, claveError = null, mensaje = null) }

    /** La validación de formato es local; la autenticación real la hace el backend (Fase 3). */
    fun ingresar() {
        val actual = _estado.value
        val usuarioError = if (actual.usuario.isBlank()) "Ingresa tu usuario o código" else null
        val claveError = if (actual.clave.isBlank()) "Ingresa tu contraseña" else null
        _estado.update {
            it.copy(
                usuarioError = usuarioError,
                claveError = claveError,
                mensaje = if (usuarioError == null && claveError == null) {
                    "La conexión con el servidor aún no está implementada (Fase 3)."
                } else null,
            )
        }
    }
}
