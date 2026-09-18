package com.example.khitomiviewer.repository

import android.content.Context
import com.example.khitomiviewer.api.GithubApi
import com.example.khitomiviewer.api.HitomiApi
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.KHitomiDatabase

class AppRepositories internal constructor(
    db: KHitomiDatabase,
    hitomiApi: HitomiApi,
    githubApi: GithubApi
) {
    val gallery = GalleryRepository(db)
    val tag = TagRepository(db)
    val hitomi = HitomiRepository(db, hitomiApi)
    val backup = BackupRepository(db)
    val github = GithubRepository(githubApi)

    companion object {
        @Volatile
        private var INSTANCE: AppRepositories? = null

        fun get(context: Context): AppRepositories {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepositories(
                    DatabaseProvider.getDatabase(context.applicationContext),
                    HitomiApi(),
                    GithubApi()
                ).also { INSTANCE = it }
            }
        }
    }
}
