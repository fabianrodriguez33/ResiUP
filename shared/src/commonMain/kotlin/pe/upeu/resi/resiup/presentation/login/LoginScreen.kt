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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.upeu.resi.resiup.presentation.components.BannerMensaje
import pe.upeu.resi.resiup.presentation.components.ChipEstado
import pe.upeu.resi.resiup.presentation.components.EstadoVisual
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import resiup.shared.generated.resources.Res
import resiup.shared.generated.resources.residencia_azul
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
        Column(Modifier.fillMaxWidth().statusBarsPadding().alpha(entrada.value), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Image(painterResource(Res.drawable.residencia_azul), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Espacio.xl).padding(top = Espacio.l, bottom = Espacio.xl),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Espacio.s),
            ) {
                Text("Bienvenido a ResiUP", style = MaterialTheme.typography.displaySmall, color = colores.sobreMarca, textAlign = TextAlign.Center)
                Text(
                    "Más que una residencia, una hermandad con propósito",
                    style = MaterialTheme.typography.bodyLarge, color = colores.sobreMarca, textAlign = TextAlign.Center,
                )
            }
        }
        Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Espacio.xl, vertical = Espacio.xl).alpha(entrada.value),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                Column(Modifier.padding(start = Espacio.m, bottom = Espacio.s)) {
                    Text("Inicia Sesión", style = MaterialTheme.typography.headlineLarge, color = pe.upeu.resi.resiup.presentation.theme.Acento.Azul.color())
                    Text("para estar al día con tu espacio.", style = MaterialTheme.typography.bodyMedium, color = pe.upeu.resi.resiup.presentation.theme.Acento.Azul.color())
                }
                aviso?.let { BannerMensaje(it, EstadoVisual.Advertencia) }
                ResiCampoTexto(estado.usuario, onUsuario, "Usuario o código", error = estado.usuarioError, tipoTeclado = KeyboardType.Ascii, habilitado = !estado.cargando)
                ResiCampoTexto(estado.clave, onClave, "Contraseña", error = estado.claveError, esClave = true, habilitado = !estado.cargando)
                estado.error?.let { BannerMensaje(it, EstadoVisual.Error) }
                ResiBoton("Ingresar", onIngresar, Modifier.fillMaxWidth(), cargando = estado.cargando)
                ResiBoton("Olvidé mi contraseña", onOlvide, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario, habilitado = !estado.cargando)
            }
        }
    }
}
