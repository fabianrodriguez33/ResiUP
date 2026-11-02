package pe.upeu.resi.resiup.presentation.login

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import pe.upeu.resi.resiup.presentation.components.IsotipoResi
import pe.upeu.resi.resiup.presentation.components.ResiBoton
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.VarianteBoton
import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiTheme
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface

/**
 * Solo inicio de sesión: nadie se registra desde la app. Las cuentas las crea la administración de la residencia.
 * Diseño plano: cabecera de marca con el saludo y una hoja clara con el formulario.
 */
@Composable
fun LoginScreen(
    estado: LoginUiState,
    onUsuario: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit,
    aviso: String?,
    onOlvide: () -> Unit = {},
) {
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val colores = ResiTheme.colores

    Column(Modifier.fillMaxSize().background(colores.marca)) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Espacio.xl, vertical = Espacio.xl).alpha(entrada.value),
            verticalArrangement = Arrangement.spacedBy(Espacio.s),
        ) {
            Image(IsotipoResi, null, Modifier.size(56.dp))
            Spacer(Modifier.height(Espacio.l))
            Text("Hola,", style = MaterialTheme.typography.displaySmall, color = colores.sobreMarca.copy(alpha = 0.85f))
            Text("bienvenido a ResiUP", style = MaterialTheme.typography.displaySmall, color = colores.sobreMarca)
            Text(
                "Residencia Universitaria Fernando Stahl",
                style = MaterialTheme.typography.bodyMedium,
                color = colores.sobreMarca.copy(alpha = 0.75f),
            )
        }
        Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(Espacio.xl).alpha(entrada.value),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                Text("Entra a tu cuenta", style = MaterialTheme.typography.headlineSmall)
                aviso?.let { BannerMensaje(it, EstadoVisual.Advertencia) }
                ResiCampoTexto(estado.usuario, onUsuario, "Usuario o código", error = estado.usuarioError, tipoTeclado = KeyboardType.Ascii, habilitado = !estado.cargando)
                ResiCampoTexto(estado.clave, onClave, "Contraseña", error = estado.claveError, esClave = true, habilitado = !estado.cargando)
                estado.error?.let { BannerMensaje(it, EstadoVisual.Error) }
                ResiBoton("Ingresar", onIngresar, Modifier.fillMaxWidth(), cargando = estado.cargando)
                ResiBoton("Olvidé mi contraseña", onOlvide, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !estado.cargando)
                Text(
                    "¿No tienes cuenta? La administración de la residencia te la crea.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
