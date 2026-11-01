package pe.upeu.resi.resiup.presentation.util

import pe.upeu.resi.resiup.domain.model.AsistenciaCulto
import pe.upeu.resi.resiup.domain.model.EvaluacionLimpieza

/** Notas de la residencia, de mejor a peor: AD, A, B y C. */
enum class Nota { AD, A, B, C }

/** Cultos asistidos en el mes: AD 20 o más, A 15-19, B 11-14, C 0-10. */
fun notaCultos(cultos: Int): Nota = when {
    cultos >= 20 -> Nota.AD
    cultos >= 15 -> Nota.A
    cultos >= 11 -> Nota.B
    else -> Nota.C
}

/** Promedio de limpieza del mes sobre 20, redondeado: AD 18-20, A 15-17, B 11-14, C 0-10. */
fun notaLimpieza(promedio: Double): Nota {
    val p = kotlin.math.floor(promedio + 0.5).toInt()
    return when {
        p >= 18 -> Nota.AD
        p >= 15 -> Nota.A
        p >= 11 -> Nota.B
        else -> Nota.C
    }
}

data class NotasMes(
    val anio: Int, val mes: Int,
    val cultos: Int, val notaCultos: Nota,
    val evaluaciones: Int, val promedioLimpieza: Double?, val notaLimpieza: Nota?,
)

/** Notas de un mes con los registros guardados en el teléfono (fechas «AAAA-MM-DD»). */
fun calcularNotasMes(asistencias: List<AsistenciaCulto>, evaluaciones: List<EvaluacionLimpieza>, anio: Int, mes: Int): NotasMes {
    val prefijo = "${anio.toString().padStart(4, '0')}-${mes.toString().padStart(2, '0')}-"
    val cultos = asistencias.count { it.fecha.startsWith(prefijo) }
    val delMes = evaluaciones.filter { it.fecha.startsWith(prefijo) }
    val promedio = if (delMes.isEmpty()) null else delMes.map { it.total }.average()
    return NotasMes(anio, mes, cultos, notaCultos(cultos), delMes.size, promedio, promedio?.let { notaLimpieza(it) })
}
