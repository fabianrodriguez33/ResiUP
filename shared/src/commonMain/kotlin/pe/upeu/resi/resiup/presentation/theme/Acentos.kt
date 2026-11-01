package pe.upeu.resi.resiup.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores de acento para dar identidad a cada módulo (permisos, asistencias, limpieza...). Cada uno tiene un tono fuerte
 * para íconos y cifras y un tono suave para fondos; en tema oscuro el tono fuerte se aclara y el suave se vuelve translúcido.
 */
enum class Acento(
    private val claro: Color, private val claroSuave: Color,
    private val oscuro: Color, private val oscuroSuave: Color,
) {
    Azul(Color(0xFF1F63E0), Color(0xFFE3ECFF), Color(0xFF7FB0FF), Color(0xFF7FB0FF).copy(alpha = 0.18f)),
    Verde(Color(0xFF0F9D58), Color(0xFFDDF5E8), Color(0xFF5ED79B), Color(0xFF5ED79B).copy(alpha = 0.18f)),
    Naranja(Color(0xFFEA580C), Color(0xFFFFE8D6), Color(0xFFFF9D5C), Color(0xFFFF9D5C).copy(alpha = 0.18f)),
    Violeta(Color(0xFF7C3AED), Color(0xFFEDE4FF), Color(0xFFB794FF), Color(0xFFB794FF).copy(alpha = 0.18f)),
    Rosa(Color(0xFFDB2777), Color(0xFFFFE1EE), Color(0xFFFF8AC0), Color(0xFFFF8AC0).copy(alpha = 0.18f)),
    Turquesa(Color(0xFF0E7490), Color(0xFFD5F2F8), Color(0xFF55D3EE), Color(0xFF55D3EE).copy(alpha = 0.18f)),
    Ambar(Color(0xFFB45309), Color(0xFFFFF0C7), Color(0xFFFFC857), Color(0xFFFFC857).copy(alpha = 0.18f));

    /** Tono fuerte: íconos, cifras destacadas. */
    @Composable
    fun color(): Color = if (ResiTheme.oscuro) oscuro else claro

    /** Tono suave: fondo del ícono o de la tarjeta. */
    @Composable
    fun suave(): Color = if (ResiTheme.oscuro) oscuroSuave else claroSuave
}
