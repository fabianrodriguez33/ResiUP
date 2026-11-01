package pe.upeu.resi.resiup.presentation.util

import kotlinx.datetime.LocalDate

private val FORMATO_CORREO = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

/** [fechaIso] es opcional; si se escribe debe ser una fecha real anterior a hoy. Devuelve el mensaje de cada campo con error. */
fun validarContacto(email: String, telefono: String, fechaIso: String, hoy: LocalDate = hoyLima()): Map<String, String> = buildMap {
    if (email.isNotBlank() && !email.trim().matches(FORMATO_CORREO)) put("email", "Escribe un correo válido, por ejemplo nombre@upeu.edu.pe")
    if (telefono.isNotBlank() && telefono.count { it.isDigit() } < 6) put("telefono", "Escribe un teléfono válido")
    if (fechaIso.isNotBlank()) {
        val fecha = runCatching { LocalDate.parse(fechaIso) }.getOrNull()
        if (fecha == null || fecha >= hoy || fecha.year < 1900) put("fechaNacimiento", "Elige una fecha de nacimiento válida")
    }
}
