package com.example.khitomiviewer.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.khitomiviewer.PreferenceManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object CrawlScheduler {
    const val UNIQUE_WORK_NAME = "hitomi_background_crawl"
    val allowedHours = listOf(24, 12, 8, 6, 4)

    fun apply(context: Context, enabled: Boolean, wifiOnly: Boolean, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val hours = if (intervalHours in allowedHours) intervalHours else 8
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()
        val request = PeriodicWorkRequestBuilder<CrawlWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    suspend fun applyFromPrefs(context: Context, prefs: PreferenceManager) {
        apply(
            context,
            prefs.backgroundCrawlEnabled.first(),
            prefs.backgroundCrawlWifiOnly.first(),
            prefs.backgroundCrawlIntervalHours.first()
        )
    }
}
