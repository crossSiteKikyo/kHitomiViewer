package com.example.khitomiviewer.repository

import android.util.Log
import com.example.khitomiviewer.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

enum class SyncLogKind { Info, Done, Error, Skipped }

data class SyncLogEntry(
    val timeLabel: String,
    val kind: SyncLogKind,
    val message: String
)

class HitomiSync(
    private val galleryRepository: GalleryRepository,
    private val hitomiRepository: HitomiRepository,
    private val prefManager: PreferenceManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)

    private val crawlAmount = 40
    private val maxLogs = 300
    private val serverGidList = mutableListOf<Int>()
    private val filteredIds = mutableListOf<Int>()
    private val dbAllGId = mutableListOf<Long>()

    private val _crawlStatus = MutableStateFlow("대기 중")
    val crawlStatus: StateFlow<String> = _crawlStatus.asStateFlow()

    private val _remainingCount = MutableStateFlow(0)
    val remainingCount: StateFlow<Int> = _remainingCount.asStateFlow()

    private val _pendingTotal = MutableStateFlow(0)
    val pendingTotal: StateFlow<Int> = _pendingTotal.asStateFlow()

    private val _logs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val logs: StateFlow<List<SyncLogEntry>> = _logs.asStateFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            setPhase("동기화 시작")
            log(SyncLogKind.Info, "동기화 시작")
            hitomiRepository.initType()
            hitomiRepository.initTag()
            getGIdsAndFilterGids()
            crawlNewGalleries(phaseLabel = "신규 갤러리")
            crawlMissedGalleries()
            deleteDeletedGalleries()
            syncGalleryTag1000()
            syncGalleryTag2000()
            syncGalleryTag3000()
            syncGalleryTag4000()
            syncGalleryTag5000()
            syncGalleryTag6000()
            syncGalleryTag7000()
            syncGalleryTag8000()
            setPhase("모든 동기화 작업 완료")
            log(SyncLogKind.Done, "모든 동기화 작업 완료")
        }
    }

    private suspend fun getGIdsAndFilterGids() = withContext(Dispatchers.IO) {
        var isSuccess = false
        setPhase("서버 갤러리 목록 받는 중")

        while (!isSuccess) {
            try {
                serverGidList.clear()
                serverGidList.addAll(hitomiRepository.fetchKoreanGids())
                val targetGid = galleryRepository.findMaxGid().toInt()
                val searchIndex = serverGidList.binarySearch(targetGid, reverseOrder())
                val limitIndex = if (searchIndex >= 0) {
                    searchIndex
                } else {
                    -(searchIndex + 1)
                }
                filteredIds.clear()
                filteredIds.addAll(serverGidList.subList(0, limitIndex))
                filteredIds.reverse()
                publishRemaining(filteredIds.size)
                log(
                    SyncLogKind.Done,
                    "서버 갤러리 ${serverGidList.size}개 확인, 신규 ${filteredIds.size}개"
                )
                isSuccess = true
            } catch (e: Exception) {
                Log.i("갤러리 gId 리스트 얻기 오류", "${e.message}")
                log(SyncLogKind.Error, "갤러리 목록 요청 실패: ${e.message}")
                setPhase("갤러리 목록 재시도 대기")
                delay(5000)
            }
        }
    }

    private suspend fun crawlNewGalleries(phaseLabel: String) {
        if (filteredIds.isEmpty()) {
            log(SyncLogKind.Done, "$phaseLabel 없음")
            publishRemaining(0)
            return
        }
        val total = filteredIds.size
        publishRemaining(total)
        log(SyncLogKind.Info, "$phaseLabel 크롤 시작 · ${total}개")
        while (filteredIds.isNotEmpty()) {
            crawlList(phaseLabel, total)
            delay(20)
        }
        log(SyncLogKind.Done, "$phaseLabel 크롤 완료")
        publishRemaining(0)
    }

    private suspend fun crawlList(phaseLabel: String, total: Int) = withContext(Dispatchers.IO) {
        val gIds = filteredIds.take(crawlAmount)
        setPhase("$phaseLabel 크롤 중 · 남은 ${filteredIds.size}개")

        val deferredList = gIds.map { gId ->
            async {
                try {
                    hitomiRepository.fetchGalleryFullInfo(gId)
                } catch (e: Exception) {
                    Log.i("각종 정보 얻기 오류", "${e.message}")
                    log(SyncLogKind.Error, "갤러리 $gId 정보 요청 실패: ${e.message}")
                    null
                }
            }
        }
        val galleryFullInfos = deferredList.awaitAll()
        val successfulInfos = galleryFullInfos.filterNotNull()
        val successfulGIds = successfulInfos.map { it.galleryInfo.id.toInt() }
        var insertFail = 0
        for (gFI in successfulInfos) {
            try {
                hitomiRepository.insertGalleryInfo(gFI.galleryInfo, gFI.thumb1, gFI.thumb2)
            } catch (e: Exception) {
                Log.i("썸네일 크롤링 오류", "${e.message}")
                insertFail++
                log(
                    SyncLogKind.Error,
                    "갤러리 ${gFI.galleryInfo.id} 저장 실패: ${e.message}"
                )
            }
        }
        filteredIds.removeAll(successfulGIds.toSet())
        publishRemaining(total)
        val failCount = gIds.size - successfulGIds.size + insertFail
        if (failCount > 0) {
            log(
                SyncLogKind.Info,
                "$phaseLabel 배치: 성공 ${successfulGIds.size}, 실패 $failCount · 남은 ${filteredIds.size}"
            )
        }
        if (filteredIds.isEmpty()) {
            setPhase("$phaseLabel 크롤 완료")
        }
    }

    private suspend fun crawlMissedGalleries() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastCrawlMissedGalleries.first()
        if (now - lastDaily <= 1000L * 60 * 60 * 24) {
            log(SyncLogKind.Skipped, "빠진 갤러리 보충: 아직 주기가 되지 않아 건너뜀")
            return@withContext
        }
        setPhase("빠진 갤러리 보충 중")
        filteredIds.clear()
        filteredIds.addAll(serverGidList)
        filteredIds.removeIf { galleryRepository.existsById(it.toLong()) }
        log(SyncLogKind.Info, "빠진 갤러리 ${filteredIds.size}개 보충 시작")
        crawlNewGalleries(phaseLabel = "빠진 갤러리")
        prefManager.updateLastCrawlMissedGalleries(now)
        log(SyncLogKind.Done, "빠진 갤러리 보충 완료")
    }

    private suspend fun deleteDeletedGalleries() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastDeleteDeletedGallery.first()
        if (now - lastDaily <= 1000L * 60 * 60 * 24 * 2) {
            log(SyncLogKind.Skipped, "삭제된 갤러리 정리: 아직 주기가 되지 않아 건너뜀")
            return@withContext
        }
        setPhase("삭제된 갤러리 정리 중")
        if (dbAllGId.isEmpty())
            dbAllGId.addAll(galleryRepository.findAllGId())
        val idsToDelete = dbAllGId.toSet() - serverGidList.map { it.toLong() }.toSet()
        idsToDelete.forEach { gId ->
            hitomiRepository.removeGalleryInfo(gId)
        }
        prefManager.updateLastDeleteDeletedGallery(now)
        log(SyncLogKind.Done, "삭제된 갤러리 ${idsToDelete.size}개 정리 완료")
        setPhase("삭제된 갤러리 정리 완료")
    }

    private suspend fun syncGalleryTag1000() = syncGalleryTagRange(
        label = "태그 동기화 (0–999)",
        fromInclusive = 0,
        toInclusive = 999,
        intervalMs = 1000L * 60 * 60 * 24,
        readLast = { prefManager.lastSyncGalleryTag1000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag1000(it) }
    )

    private suspend fun syncGalleryTag2000() = syncGalleryTagRange(
        label = "태그 동기화 (1000–1999)",
        fromInclusive = 1000,
        toInclusive = 1999,
        intervalMs = 1000L * 60 * 60 * 24 * 2,
        readLast = { prefManager.lastSyncGalleryTag2000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag2000(it) }
    )

    private suspend fun syncGalleryTag3000() = syncGalleryTagRange(
        label = "태그 동기화 (2000–2999)",
        fromInclusive = 2000,
        toInclusive = 2999,
        intervalMs = 1000L * 60 * 60 * 24 * 4,
        readLast = { prefManager.lastSyncGalleryTag3000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag3000(it) }
    )

    private suspend fun syncGalleryTag4000() = syncGalleryTagRange(
        label = "태그 동기화 (3000–3999)",
        fromInclusive = 3000,
        toInclusive = 3999,
        intervalMs = 1000L * 60 * 60 * 24 * 4,
        readLast = { prefManager.lastSyncGalleryTag4000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag4000(it) }
    )

    private suspend fun syncGalleryTag5000() = syncGalleryTagRange(
        label = "태그 동기화 (4000–4999)",
        fromInclusive = 4000,
        toInclusive = 4999,
        intervalMs = 1000L * 60 * 60 * 24 * 8,
        readLast = { prefManager.lastSyncGalleryTag5000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag5000(it) }
    )

    private suspend fun syncGalleryTag6000() = syncGalleryTagRange(
        label = "태그 동기화 (5000–5999)",
        fromInclusive = 5000,
        toInclusive = 5999,
        intervalMs = 1000L * 60 * 60 * 24 * 8,
        readLast = { prefManager.lastSyncGalleryTag6000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag6000(it) }
    )

    private suspend fun syncGalleryTag7000() = syncGalleryTagRange(
        label = "태그 동기화 (6000–6999)",
        fromInclusive = 6000,
        toInclusive = 6999,
        intervalMs = 1000L * 60 * 60 * 24 * 8,
        readLast = { prefManager.lastSyncGalleryTag7000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag7000(it) }
    )

    private suspend fun syncGalleryTag8000() = syncGalleryTagRange(
        label = "태그 동기화 (7000–7999)",
        fromInclusive = 7000,
        toInclusive = 7999,
        intervalMs = 1000L * 60 * 60 * 24 * 8,
        readLast = { prefManager.lastSyncGalleryTag8000.first() },
        writeLast = { prefManager.updateLastSyncGalleryTag8000(it) }
    )

    private suspend fun syncGalleryTagRange(
        label: String,
        fromInclusive: Int,
        toInclusive: Int,
        intervalMs: Long,
        readLast: suspend () -> Long,
        writeLast: suspend (Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - readLast() <= intervalMs) {
            log(SyncLogKind.Skipped, "$label: 아직 주기가 되지 않아 건너뜀")
            return@withContext
        }
        if (dbAllGId.isEmpty())
            dbAllGId.addAll(galleryRepository.findAllGId())
        if (fromInclusive >= dbAllGId.size) {
            writeLast(now)
            log(SyncLogKind.Done, "$label: 대상 없음, 완료 처리")
            return@withContext
        }
        log(SyncLogKind.Info, "$label 시작")
        val end = min(toInclusive + 1, dbAllGId.size)
        for (i in fromInclusive until end step crawlAmount) {
            val until = min(i + crawlAmount, end)
            setPhase("$label · offset $i")
            syncGalleryTag(dbAllGId.subList(i, until))
        }
        writeLast(now)
        setPhase("$label 완료")
        log(SyncLogKind.Done, "$label 완료")
    }

    private suspend fun syncGalleryTag(gIdList: List<Long>) = withContext(Dispatchers.IO) {
        val deferredList = gIdList.map { gId ->
            async {
                try {
                    hitomiRepository.syncGalleryTagsFromRemote(gId)
                } catch (e: Exception) {
                    Log.i("갤러리태그 동기화 $gId 중 오류", "${e.message}")
                    log(SyncLogKind.Error, "갤러리 $gId 태그 동기화 실패: ${e.message}")
                }
            }
        }
        deferredList.awaitAll()
    }

    private fun publishRemaining(total: Int? = null) {
        if (total != null) _pendingTotal.value = total
        _remainingCount.value = filteredIds.size
    }

    private fun setPhase(message: String) {
        _crawlStatus.value = message
    }

    private fun log(kind: SyncLogKind, message: String) {
        val entry = SyncLogEntry(getCurrentFormattedTime(), kind, message)
        _logs.value = (listOf(entry) + _logs.value).take(maxLogs)
    }

    private fun getCurrentFormattedTime(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return current.format(formatter)
    }
}
