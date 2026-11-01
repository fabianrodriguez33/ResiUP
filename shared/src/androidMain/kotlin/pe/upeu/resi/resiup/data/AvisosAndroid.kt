package pe.upeu.resi.resiup.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import pe.upeu.resi.resiup.data.local.AvisosDelSistema
import pe.upeu.resi.resiup.domain.model.Notificacion

/** Notificaciones del sistema de Android. Recuerda el último aviso mostrado para no repetir ninguno. */
class AvisosAndroid(private val contexto: Context) : AvisosDelSistema {
    private val prefs = contexto.getSharedPreferences("avisos", Context.MODE_PRIVATE)

    override fun mostrarNuevos(noLeidas: List<Notificacion>) {
        val ultimo = prefs.getLong(ULTIMO, 0L)
        val nuevos = noLeidas.filter { it.id > ultimo }.sortedBy { it.id }
        if (nuevos.isEmpty()) return
        prefs.edit().putLong(ULTIMO, nuevos.last().id).apply()
        if (!puedeAvisar()) return
        val gestor = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestor.createNotificationChannel(NotificationChannel(CANAL, "Avisos de la residencia", NotificationManager.IMPORTANCE_HIGH))
        val abrir = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)?.let {
            PendingIntent.getActivity(contexto, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // Si llegaron muchos a la vez, solo se muestran los más recientes.
        nuevos.takeLast(MAXIMO).forEach { n ->
            val aviso = NotificationCompat.Builder(contexto, CANAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(n.titulo)
                .setContentText(n.mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(n.mensaje))
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .build()
            gestor.notify(n.id.toInt(), aviso)
        }
    }

    private fun puedeAvisar() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CANAL = "avisos"
        const val ULTIMO = "ultimo_mostrado"
        const val MAXIMO = 5
    }
}
