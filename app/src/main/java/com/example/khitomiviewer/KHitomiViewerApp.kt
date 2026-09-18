package com.example.khitomiviewer

import android.app.Application
import com.example.khitomiviewer.repository.AppRepositories

class KHitomiViewerApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AppRepositories.get(this).hitomiSync.start()
  }
}
