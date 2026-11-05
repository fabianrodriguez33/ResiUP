package pe.upeu.resi.resiup.domain.repository

import pe.upeu.resi.resiup.domain.model.ResumenNotas

/** Notas mensuales del residente: se piden siempre al servidor (hace falta conexión). */
interface NotasRepository {
    suspend fun resumen(anio: Int, mes: Int): Result<ResumenNotas>
}
