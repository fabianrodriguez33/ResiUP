package pe.upeu.resi.resiup.data.repository

import pe.upeu.resi.resiup.data.remote.NotasApi
import pe.upeu.resi.resiup.data.remote.ejecutarLlamada
import pe.upeu.resi.resiup.domain.model.ResumenNotas
import pe.upeu.resi.resiup.domain.repository.NotasRepository

class NotasRepositoryImpl(private val api: NotasApi) : NotasRepository {
    override suspend fun resumen(anio: Int, mes: Int) = ejecutarLlamada {
        api.resumen(anio, mes).let {
            ResumenNotas(
                it.anio, it.mes, it.metodologia == "POR_DIAS", it.cultos, it.notaCultos, it.sinCultos, it.notaProvisional,
                it.diasAsistidos, it.diasConCulto, it.faltasJustificadas, it.porcentajeAsistencia,
                it.evaluaciones, it.promedioLimpieza, it.notaLimpieza,
            )
        }
    }
}
