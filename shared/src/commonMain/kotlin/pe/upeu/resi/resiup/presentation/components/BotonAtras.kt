package pe.upeu.resi.resiup.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Flecha para volver desde una pantalla secundaria (zona táctil de 48 dp). */
@Composable
fun BotonAtras(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Volver", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ResiIcono(ResiIconos.Atras, "Volver", tint = MaterialTheme.colorScheme.onSurface, tamano = 26.dp)
    }
}
