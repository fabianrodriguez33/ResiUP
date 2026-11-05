package pe.upeu.resi.resiup.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class ResumenMensualDto(
    val anio: Int, val mes: Int, val cultos: Long = 0, val notaCultos: String? = null,
    val evaluaciones: Long = 0, val promedioLimpieza: Double? = null, val notaLimpieza: String? = null,
    val metodologia: String = "ANTERIOR", val diasAsistidos: Long? = null, val diasConCulto: Long? = null,
    val faltasJustificadas: Long? = null, val porcentajeAsistencia: Double? = null,
    val sinCultos: Boolean = false, val notaProvisional: Boolean = false,
)

/** Notas del mes calculadas por el servidor (única fuente de verdad). */
class NotasApi(private val client: HttpClient) {
    suspend fun resumen(anio: Int, mes: Int): ResumenMensualDto =
        client.get("api/v1/residentes/yo/resumen-mensual") { parameter("anio", anio); parameter("mes", mes) }.body()
}
