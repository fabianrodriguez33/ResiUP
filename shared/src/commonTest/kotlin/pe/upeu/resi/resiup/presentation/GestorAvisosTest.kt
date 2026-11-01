package pe.upeu.resi.resiup.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.data.local.AvisosDelSistema
import pe.upeu.resi.resiup.domain.model.Notificacion
import pe.upeu.resi.resiup.domain.repository.NotificacionesRepository
import pe.upeu.resi.resiup.presentation.notificaciones.GestorAvisos

class GestorAvisosTest {
    private fun aviso(id: Long, leida: Boolean = false) = Notificacion(id, "Título $id", "Mensaje", leida, Instant.fromEpochMilliseconds(id))

    private class Repo(var lista: List<Notificacion>) : NotificacionesRepository {
        override suspend fun recientes() = Result.success(lista)
        override suspend fun cantidadNoLeidas() = Result.success(lista.count { !it.leida })
        override suspend fun marcarLeida(id: Long) = Result.success(Unit)
        override suspend fun marcarTodasLeidas() = Result.success(Unit)
    }

    private class Sistema : AvisosDelSistema {
        val mostrados = mutableListOf<Long>()
        override fun mostrarNuevos(noLeidas: List<Notificacion>) { mostrados += noLeidas.map { it.id } }
    }

    @Test
    fun `el contador sigue a los avisos sin leer y solo los no leidos llegan al sistema`() = runTest {
        val sistema = Sistema()
        val gestor = GestorAvisos(Repo(listOf(aviso(3), aviso(2, leida = true), aviso(1))), sistema)
        gestor.revisar()
        assertEquals(2, gestor.noLeidas.value)
        assertEquals(listOf(3L, 1L), sistema.mostrados)
    }

    @Test
    fun `sin avisos pendientes no se molesta al sistema y marcar resta del contador`() = runTest {
        val sistema = Sistema()
        val repo = Repo(listOf(aviso(1, leida = true)))
        val gestor = GestorAvisos(repo, sistema)
        gestor.revisar()
        assertEquals(0, gestor.noLeidas.value)
        assertEquals(emptyList(), sistema.mostrados)

        repo.lista = listOf(aviso(2), aviso(1, leida = true))
        gestor.revisar()
        assertEquals(1, gestor.noLeidas.value)
        gestor.restar()
        gestor.restar() // nunca baja de cero
        assertEquals(0, gestor.noLeidas.value)
    }
}
