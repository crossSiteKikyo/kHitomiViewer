package com.example.khitomiviewer

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okio.IOException

// 전역 인스턴스 생성
val Context.dataStore by preferencesDataStore(name = "setting")

class PreferenceManager(private val context: Context) {
    // 키들 정의
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_RTL_MODE = booleanPreferencesKey("is_rtl")
        val IS_AUTO_PLAY_LOOP = booleanPreferencesKey("is_auto_play_loop")
        val AUTO_PLAY_PERIOD = intPreferencesKey("auto_play_period")
        val TYPE_ID_LIST = stringPreferencesKey("showTypeIdList")
        val IS_VOLUME_KEY_PAGING = booleanPreferencesKey("is_volume_key_paging")
        val LAST_CRAWL_MISSED_GALLERIES = longPreferencesKey("last_crawl_missed_galleries")
        val LAST_DELETE_DELETED_GALLERY = longPreferencesKey("last_delete_deleted_gallery")
        val LAST_SYNC_GALLERY_TAG_1000 = longPreferencesKey("last_sync_gallery_tag_1000")
        val LAST_SYNC_GALLERY_TAG_2000 = longPreferencesKey("last_sync_gallery_tag_2000")
        val LAST_SYNC_GALLERY_TAG_3000 = longPreferencesKey("last_sync_gallery_tag_3000")
        val LAST_SYNC_GALLERY_TAG_4000 = longPreferencesKey("last_sync_gallery_tag_4000")
        val LAST_SYNC_GALLERY_TAG_5000 = longPreferencesKey("last_sync_gallery_tag_5000")
        val LAST_SYNC_GALLERY_TAG_6000 = longPreferencesKey("last_sync_gallery_tag_6000")
        val LAST_SYNC_GALLERY_TAG_7000 = longPreferencesKey("last_sync_gallery_tag_7000")
        val LAST_SYNC_GALLERY_TAG_8000 = longPreferencesKey("last_sync_gallery_tag_8000")
    }

    // 공통 읽기 로직
    private fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences())
                else throw exception
            }
            .map { preferences -> preferences[key] ?: defaultValue }
    }

    // 공통 쓰기 로직
    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    /* 외부에서 사용할 메서드들 */
    // 다크모드 관련
    val isDarkMode: Flow<Boolean> = getPreference(Keys.IS_DARK_MODE, false)
    suspend fun setDarkMode(isDark: Boolean) = setPreference(Keys.IS_DARK_MODE, isDark)

    // 만화 읽기 방향 관련
    val isRtlMode: Flow<Boolean> = getPreference(Keys.IS_RTL_MODE, false)
    suspend fun setRtl(isRtl: Boolean) = setPreference(Keys.IS_RTL_MODE, isRtl)

    // 만화 자동 넘기기 관련
    val isAutoPlayLoop: Flow<Boolean> = getPreference(Keys.IS_AUTO_PLAY_LOOP, false)
    suspend fun setAutoPlayLoop(isAutoPlayLoop: Boolean) =
        setPreference(Keys.IS_AUTO_PLAY_LOOP, isAutoPlayLoop)

    val autoPlayPeriod: Flow<Int> = getPreference(Keys.AUTO_PLAY_PERIOD, 10)
    suspend fun updateAutoPlayPeriod(period: Int) = setPreference(Keys.AUTO_PLAY_PERIOD, period)

    // 볼륨 키로 페이징하기 관련
    val isVolumeKeyPaging: Flow<Boolean> = getPreference(Keys.IS_VOLUME_KEY_PAGING, false)
    suspend fun setVolumeKeyPaging(enabled: Boolean) =
        setPreference(Keys.IS_VOLUME_KEY_PAGING, enabled)

    // 미처 크롤링 하지 못한 갤러리 크롤링 관련
    val lastCrawlMissedGalleries: Flow<Long> = getPreference(Keys.LAST_CRAWL_MISSED_GALLERIES, 0L)
    suspend fun updateLastCrawlMissedGalleries(time: Long) =
        setPreference(Keys.LAST_CRAWL_MISSED_GALLERIES, time)

    // 삭제된 갤러리 삭제 관련
    val lastDeleteDeletedGallery: Flow<Long> = getPreference(Keys.LAST_DELETE_DELETED_GALLERY, 0L)
    suspend fun updateLastDeleteDeletedGallery(time: Long) =
        setPreference(Keys.LAST_DELETE_DELETED_GALLERY, time)

    // 갤러리 태그 동기화 1000까지 관련
    val lastSyncGalleryTag1000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_1000, 0L)

    suspend fun updateLastSyncGalleryTag1000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_1000, time)

    val lastSyncGalleryTag2000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_2000, 0L)

    suspend fun updateLastSyncGalleryTag2000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_2000, time)

    val lastSyncGalleryTag3000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_3000, 0L)

    suspend fun updateLastSyncGalleryTag3000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_3000, time)

    val lastSyncGalleryTag4000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_4000, 0L)

    suspend fun updateLastSyncGalleryTag4000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_4000, time)

    val lastSyncGalleryTag5000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_5000, 0L)

    suspend fun updateLastSyncGalleryTag5000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_5000, time)

    val lastSyncGalleryTag6000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_6000, 0L)

    suspend fun updateLastSyncGalleryTag6000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_6000, time)

    val lastSyncGalleryTag7000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_7000, 0L)

    suspend fun updateLastSyncGalleryTag7000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_7000, time)

    val lastSyncGalleryTag8000: Flow<Long> =
        getPreference(Keys.LAST_SYNC_GALLERY_TAG_8000, 0L)

    suspend fun updateLastSyncGalleryTag8000(time: Long) =
        setPreference(Keys.LAST_SYNC_GALLERY_TAG_8000, time)

    // 보일 타입 ID들 관련
    val typeIdList: Flow<List<Long>> = getPreference(Keys.TYPE_ID_LIST, "[1,2,3,4,5]")
        .map { jsonString ->
            try {
                Json.decodeFromString<List<Long>>(jsonString)
            } catch (e: Exception) {
                listOf(1L, 2L, 3L, 4L, 5L) // 파싱 실패 시 기본값
            }
        }

    suspend fun setTypeIdList(list: List<Long>) {
        val jsonString = Json.encodeToString(list)
        setPreference(Keys.TYPE_ID_LIST, jsonString)
    }
}