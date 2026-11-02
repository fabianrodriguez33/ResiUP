package pe.upeu.resi.resiup.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import org.koin.core.context.GlobalContext
import pe.upeu.resi.resiup.presentation.notificaciones.GestorAvisos

/**
 * Con la app cerrada, cada 15 minutos (el mínimo de Android) revisa los avisos nuevos y los muestra como notificaciones del
 * sistema. Sin sesión iniciada el servidor responde 401 y simplemente no pasa nada.
 */
class RevisarAvisosWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        // Primero se envían los permisos que quedaron pendientes sin conexión, luego se revisan los avisos.
        runCatching { GlobalContext.get().get<pe.upeu.resi.resiup.data.repository.ColaPermisos>().enviarPendientes() }
        runCatching { GlobalContext.get().get<GestorAvisos>().revisar() }
        return Result.success()
    }

    companion object {
        fun programar(contexto: Context) {
            val peticion = PeriodicWorkRequestBuilder<RevisarAvisosWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("revisar-avisos", ExistingPeriodicWorkPolicy.KEEP, peticion)
        }
    }
}
