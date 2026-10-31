package pe.upeu.resi.resiup.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.error.ErrorApiException
import pe.upeu.resi.resiup.domain.usecase.IniciarSesionUseCase
import pe.upeu.resi.resiup.presentation.sesion.GestorSesion

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val usuarioError: String? = null,
    val claveError: String? = null,
    val cargando: Boolean = false,
    val error: String? = null,
)

fun ErrorApi.aMensaje(): String = when (this) {
    is ErrorApi.CredencialesInvalidas -> "Usuario o contraseña incorrectos"
    is ErrorApi.SinConexion -> "No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo."
    is ErrorApi.TiempoAgotado -> "El servidor tardó demasiado en responder. Inténtalo de nuevo."
    is ErrorApi.Servidor -> "El servidor tuvo un problema. Inténtalo más tarde."
    is ErrorApi.SesionExpirada -> "Tu sesión expiró. Inicia sesión nuevamente."
    is ErrorApi.SinPermiso -> "Tu cuenta no tiene permiso para esta acción."
    is ErrorApi.RolNoSoportado -> "Tu cuenta no tiene un rol compatible con la aplicación."
    is ErrorApi.NoEncontrado -> "No se encontró lo solicitado."
    is ErrorApi.Conflicto -> mensaje
    is ErrorApi.Validacion -> porCampo.values.firstOrNull() ?: "Revisa los datos ingresados."
}

class LoginViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val gestor: GestorSesion,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun alCambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, usuarioError = null, error = null) }
    fun alCambiarClave(valor: String) = _estado.update { it.copy(clave = valor, claveError = null, error = null) }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, error = null, usuarioError = null, claveError = null) }
        viewModelScope.launch {
            iniciarSesion(actual.usuario, actual.clave)
                .onSuccess { sesion ->
                    _estado.update { LoginUiState() } // no se conserva la contraseña en memoria de la pantalla
                    gestor.sesionIniciada(sesion)
                }
                .onFailure { e ->
                    val error = (e as? ErrorApiException)?.error ?: ErrorApi.SinConexion
                    _estado.update {
                        if (error is ErrorApi.Validacion) {
                            it.copy(cargando = false, usuarioError = error.porCampo["username"], claveError = error.porCampo["password"],
                                error = if (error.porCampo.keys.all { k -> k != "username" && k != "password" }) error.aMensaje() else null)
                        } else {
                            it.copy(cargando = false, clave = "", error = error.aMensaje())
                        }
                    }
                }
        }
    }
}
