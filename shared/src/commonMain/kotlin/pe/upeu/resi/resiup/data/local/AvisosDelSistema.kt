package pe.upeu.resi.resiup.data.local

import pe.upeu.resi.resiup.domain.model.Notificacion

/**
 * Muestra los avisos nuevos como notificaciones del sistema (barra de estado). Recuerda cuáles ya mostró para no repetirlos.
 * Android las crea con NotificationManager; en iOS queda pendiente hasta contar con APNs.
 */
interface AvisosDelSistema {
    /** [noLeidas] viene de la más reciente a la más antigua. */
    fun mostrarNuevos(noLeidas: List<Notificacion>)
}
