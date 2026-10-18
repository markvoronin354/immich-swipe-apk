package com.markvoronin.immichswipe.core.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.ReminderInterval
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

/**
 * Planifie ou annule le rappel de tri via WorkManager.
 *
 * Le délai avant chaque rappel est tiré au hasard dans la plage de la fréquence choisie.
 * Comme WorkManager ne sait pas varier le délai d'un travail périodique, chaque rappel est
 * un travail unique qui planifie le suivant une fois exécuté (voir [ReminderWorker]).
 * Le travail planifié survit au redémarrage de l'appareil.
 */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /**
     * Planifie le prochain rappel, en remplaçant celui déjà planifié (changement de fréquence).
     */
    fun schedule(interval: ReminderInterval, customMinDays: Long = 3L, customMaxDays: Long = 7L) {
        val delayHours = randomDelayHours(interval, customMinDays, customMaxDays)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        AppLogger.i("Reminder", "Prochain rappel (${interval.name}) dans ${delayHours / 24} j ${delayHours % 24} h")
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        AppLogger.i("Reminder", "Rappel annulé")
    }

    companion object {
        const val WORK_NAME = "cleanup_reminder"

        /**
         * Tire un délai en heures entre minDays et maxDays (bornes incluses).
         */
        fun randomDelayHours(
            interval: ReminderInterval,
            customMinDays: Long = 3L,
            customMaxDays: Long = 7L,
            random: Random = Random.Default
        ): Long {
            val min = interval.getMinDays(customMinDays).coerceAtLeast(1L)
            val max = interval.getMaxDays(customMaxDays).coerceAtLeast(min)
            return random.nextLong(min * 24, max * 24 + 1)
        }
    }
}