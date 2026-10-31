package pe.upeu.resi.resiup.presentation.login

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import pe.upeu.resi.resiup.presentation.navigation.Rol
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    estado: LoginUiState,
    onUsuario: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit,
    modoDesarrollo: Boolean,
    onVistaPrevia: (Rol) -> Unit,
    onCatalogo: () -> Unit,
) {
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val marca = ResiTheme.colores.marca

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Cabecera de marca
        Box(
            Modifier.fillMaxWidth().background(marca, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).padding(bottom = Espacio.xl),
        ) {
            Column(
                Modifier.fillMaxWidth().safeDrawingPadding().padding(top = Espacio.xl).alpha(entrada.value).scale(0.92f + 0.08f * entrada.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(IsotipoResi, null, Modifier.size(72.dp))
                Spacer(Modifier.height(Espacio.m))
                Text("ResiUP", style = MaterialTheme.typography.displaySmall, color = ResiTheme.colores.sobreMarca)
                Text(
                    "Todo en un solo lugar, una mejor experiencia residencial.",
                    Modifier.padding(horizontal = Espacio.xxl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResiTheme.colores.sobreMarca.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Espacio.xl).alpha(entrada.value),
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
            ResiCampoTexto(estado.usuario, onUsuario, "Usuario o código", error = estado.usuarioError, tipoTeclado = KeyboardType.Ascii)
            ResiCampoTexto(estado.clave, onClave, "Contraseña", error = estado.claveError, esClave = true)
            estado.mensaje?.let { BannerMensaje(it, EstadoVisual.Info) }
            ResiBoton("Ingresar", onIngresar, Modifier.fillMaxWidth())

            if (modoDesarrollo) {
                Spacer(Modifier.height(Espacio.l))
                ChipEstado("Solo desarrollo", EstadoVisual.Advertencia)
                Text(
                    "Vista previa de la navegación por rol (sin autenticación real).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                    Rol.entries.forEach { rol ->
                        ResiBoton(rol.etiqueta, { onVistaPrevia(rol) }, variante = VarianteBoton.Secundario)
                    }
                }
                ResiBoton("Catálogo de componentes", onCatalogo, Modifier.fillMaxWidth(), variante = VarianteBoton.Terciario)
            }
        }
    }
}
