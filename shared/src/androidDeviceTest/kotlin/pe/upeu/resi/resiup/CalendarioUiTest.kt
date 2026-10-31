package pe.upeu.resi.resiup

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.TipoCulto
import pe.upeu.resi.resiup.presentation.components.CalendarioAsistencias
import pe.upeu.resi.resiup.presentation.theme.ResiTheme

class CalendarioUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val hoy = LocalDate(2026, 10, 30)
    private val lista = listOf(
        AsistenciaCulto(1, "2026-10-05", TipoCulto.Matutino, Instant.fromEpochMilliseconds(0), false),
        AsistenciaCulto(2, "2026-10-14", TipoCulto.Matutino, Instant.fromEpochMilliseconds(0), true),
    )

    private fun mostrar() = rule.setContent { ResiTheme(oscuro = false) { CalendarioAsistencias(lista, hoy) } }

    @Test
    fun cadaDiaTieneDescripcionParaLectoresDePantalla() {
        mostrar()
        rule.onNodeWithContentDescription("lunes 5 oct 2026: asistió a culto matutino").assertHasClickAction()
        rule.onNodeWithContentDescription("miércoles 14 oct 2026: asistió a culto matutino después del corte").assertIsDisplayed()
        rule.onNodeWithContentDescription("jueves 8 oct 2026: sin registro de asistencia").assertIsDisplayed()
    }

    @Test
    fun tocarUnDiaMuestraSuDetalle() {
        mostrar()
        rule.onNodeWithContentDescription("lunes 5 oct 2026: asistió a culto matutino").performClick()
        rule.onNodeWithText("Culto matutino · registrado", substring = true).assertIsDisplayed()
    }

    @Test
    fun sePuedeNavegarAlMesAnteriorPeroNoAlFuturo() {
        mostrar()
        rule.onNodeWithText("Octubre 2026").assertIsDisplayed()
        rule.onNodeWithContentDescription("Mes siguiente").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Mes anterior").performClick()
        rule.onNodeWithText("Septiembre 2026").assertIsDisplayed()
        rule.onNodeWithContentDescription("Mes siguiente").performClick()
        rule.onNodeWithText("Octubre 2026").assertIsDisplayed()
    }
}
