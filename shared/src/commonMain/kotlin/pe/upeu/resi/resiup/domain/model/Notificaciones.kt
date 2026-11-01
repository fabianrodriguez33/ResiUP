package pe.upeu.resi.resiup.domain.model

import kotlin.time.Instant

/** Aviso de la residencia para el usuario (permisos, garita, días sin culto). */
data class Notificacion(
    val id: Long,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val creadaEn: Instant,
)
