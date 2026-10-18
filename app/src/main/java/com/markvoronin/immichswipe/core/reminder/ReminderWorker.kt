package com.markvoronin.immichswipe.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.markvoronin.immichswipe.MainActivity
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.local.AppDatabase
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.text.NumberFormat

/**
 * Affiche la notification de rappel pour trier ses photos, puis planifie le rappel suivant.
 */
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(context)
        val accountRepository = AccountRepository(database.userAccountDao())
        val sessionRepository = SessionRepository(context, accountRepository)

        // Le rappel est désactivé ou la session a été effacée (déconnexion) :
        // on arrête la planification au lieu de notifier.
        val config = sessionRepository.sessionConfig.first()
        if (!sessionRepository.reminderEnabled.first() || config == null) {
            WorkManager.getInstance(context).cancelUniqueWork(ReminderScheduler.WORK_NAME)
            return Result.success()
        }

        val unsortedCount = countUnsorted(config, sessionRepository.includeArchived.first())
        if (unsortedCount == 0) {
            // Tout est déjà trié : inutile de déranger l'utilisateur pour ce cycle.
            AppLogger.i("Reminder", "Aucun média non trié, notification ignorée")
        } else {
            showNotification(unsortedCount)
        }

        // Planifie le rappel suivant avec un nouveau délai aléatoire.
        val customMin = sessionRepository.reminderCustomMinDays.first()
        val customMax = sessionRepository.reminderCustomMaxDays.first()
        ReminderScheduler(context).schedule(
            sessionRepository.reminderInterval.first(),
            customMinDays = customMin,
            customMaxDays = customMax
        )
        return Result.success()
    }

    /**
     * Nombre de médias pas encore triés, calculé comme pour la collection "Tous les médias"
     * de l'accueil : total sur le serveur moins les décisions locales.
     * Retourne null si le serveur est injoignable (la notification reste alors générique).
     */
    private suspend fun countUnsorted(config: SessionConfig, includeArchived: Boolean): Int? {
        return try {
            val sessionManager = SessionManager(context)
            if (sessionManager.api == null) sessionManager.initialize(config)

            val database = AppDatabase.getDatabase(context)
            val total = AssetRepository(
                sessionManager = sessionManager,
                albumAssetDao = database.albumAssetDao()
            ).getTotalAssetCount(includeArchived)

            if (total <= 0) return null

            val treated = SwipeDecisionRepository(database.swipeDecisionDao())
                .getAllAlbumDecisionCounts(config.userId)
                .first()
                .firstOrNull { it.albumId == Album.VIRTUAL_ALL_ID }
                ?.totalCount ?: 0

            (total - treated).coerceAtLeast(0)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLogger.w("Reminder", "Impossible de compter les médias non triés", e)
            null
        }
    }

    private fun showNotification(unsortedCount: Int?) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            AppLogger.w("Reminder", "Permission de notification manquante, rappel ignoré")
            return
        }

        createChannel()

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(
                if (unsortedCount != null) {
                    context.resources.getQuantityString(
                        R.plurals.reminder_notification_text_count,
                        unsortedCount,
                        NumberFormat.getIntegerInstance().format(unsortedCount)
                    )
                } else {
                    context.getString(R.string.reminder_notification_text)
                }
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        AppLogger.i("Reminder", "Notification de rappel affichée")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "cleanup_reminder"
        const val NOTIFICATION_ID = 1001
    }
}