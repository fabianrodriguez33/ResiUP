package pe.upeu.resi.resiup.domain.model

/**
 * Notas de un mes tal como las calcula el servidor. Desde noviembre de 2026 los cultos se miden por días asistidos entre días con culto
 * ([porDias]); los meses anteriores conservan la escala antigua por cantidad de cultos ([cultos]), sin días ni porcentaje.
 * [notaCultos] es nula cuando [sinCultos]. [provisional]: el mes aún no termina (nota a la fecha).
 */
data class ResumenNotas(
    val anio: Int, val mes: Int, val porDias: Boolean,
    val cultos: Long, val notaCultos: String?, val sinCultos: Boolean, val provisional: Boolean,
    val diasAsistidos: Long?, val diasConCulto: Long?, val faltasJustificadas: Long?, val porcentajeAsistencia: Double?,
    val evaluaciones: Long, val promedioLimpieza: Double?, val notaLimpieza: String?,
)
