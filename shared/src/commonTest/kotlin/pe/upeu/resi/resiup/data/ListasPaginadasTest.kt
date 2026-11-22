package pe.upeu.resi.resiup.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import pe.upeu.resi.resiup.Entorno
import pe.upeu.resi.resiup.json
import pe.upeu.resi.resiup.data.repository.AdministracionRepositoryImpl

class ListasPaginadasTest {
    private fun fila(id: Int) = """{"id":$id,"codigo":"20240${1000 + id}","nombreCompleto":"R$id"}"""

    @Test
    fun `la lista de residentes pide todas las paginas y no se corta en la primera`() = runTest {
        val e = Entorno {
            val pagina = it.url.parameters["pagina"]?.toInt() ?: 0
            val filas = if (pagina == 0) (1..100) else (101..114)
            json("""{"contenido":[${filas.joinToString(",") { n -> fila(n) }}],"pagina":$pagina,"ultima":${pagina == 1}}""")
        }
        val lista = AdministracionRepositoryImpl(e.cliente).residentes().getOrThrow()
        assertEquals(114, lista.size)
        assertEquals(listOf("0", "1"), e.peticiones.map { it.url.parameters["pagina"] })
    }
}
