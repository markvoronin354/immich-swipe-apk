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

class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(interval: ReminderInterval, customMinDays: Long = 3L, customMaxDays: Long = 7L) {
        val delayHours = randomDelayHours(interval, customMinDays, customMaxDays)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        AppLogger.i("Reminder", "Next reminder (${interval.name}) in ${delayHours / 24} d ${delayHours % 24} h")
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
        AppLogger.i("Reminder", "Reminder cancelled")
    }

    companion object {
        const val WORK_NAME = "cleanup_reminder"

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