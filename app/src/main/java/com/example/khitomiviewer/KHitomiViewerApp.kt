package com.example.khitomiviewer

import android.app.Application
import com.example.khitomiviewer.repository.AppRepositories
import com.example.khitomiviewer.work.CrawlScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KHitomiViewerApp : Application() {
  override fun onCreate() {
    super.onCreate()
    val repos = AppRepositories.get(this)
    repos.hitomiSync.start()
    repos.hitomi.prefetchPopularGids()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      CrawlScheduler.applyFromPrefs(this@KHitomiViewerApp, repos.prefs)
    }
  }
}
