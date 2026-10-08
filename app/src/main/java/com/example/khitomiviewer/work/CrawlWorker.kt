package com.example.khitomiviewer.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.khitomiviewer.repository.AppRepositories

class CrawlWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        promoteToForeground()
        return try {
            AppRepositories.get(applicationContext).hitomiSync.run()
            Result.success()
        } catch (e: Exception) {
            Log.i("백그라운드 크롤링 실패", "${e.message}")
            Result.retry()
        }
    }

    private suspend fun promoteToForeground() {
        try {
            setForeground(foregroundInfo())
        } catch (e: Exception) {
            Log.i("크롤링 포그라운드 전환 실패", "${e.message}")
        }
    }

    private fun foregroundInfo(): ForegroundInfo {
        val notification = crawlNotification(applicationContext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "background_crawl"
        private const val NOTIFICATION_ID = 1001

        fun crawlNotification(context: Context): Notification {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "백그라운드 크롤링",
                    NotificationManager.IMPORTANCE_LOW
                )
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("크롤링 중")
                .setContentText("갤러리 동기화를 진행하고 있습니다")
                .setOngoing(true)
                .build()
        }
    }
}
