package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.api.HitomiApi
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.MyRepository
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.room.entity.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.addAll

// 히토미ViewModel - ggjs정보 해석, 새 갤러리들 크롤링, 없는 갤러리 삭제, 갤러리 태그 동기화
class HitomiViewModel(application: Application) : AndroidViewModel(application) {
    // Application Context를 안전하게 가져오기
    private val context get() = getApplication<Application>().applicationContext

    private val typeDao = DatabaseProvider.getDatabase(application).typeDao()
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()
    private val db = DatabaseProvider.getDatabase(application)
    private val repository = MyRepository(db)

    private val hitomiApi = HitomiApi()

    // 이미지 디코딩에 사용되는 변수들
    val b = mutableStateOf<String?>("1772697601/")
    val thumbChar1 = mutableStateOf<String?>("a") // a or b
    val thumbChar2 = mutableStateOf<String?>("b") // b or a
    val o1 = mutableStateOf<String?>("0") // 0 or 1
    val o2 = mutableStateOf<String?>("1") // 1 or 0
    val mList = mutableStateListOf<String>()

    // 크롤링에 사용되는 변수
    val serverGidList = mutableListOf<Int>() // 서버에 있는 모든 한국어 gId들
    val crawlAmount = 40 // 한번에 몇개씩 크롤링 할지
    val filteredIds = mutableStateListOf<Int>()

    val crawlErrorStr = mutableStateOf("아직까지 에러가 없습니다")
    val crawlStatusStr = mutableStateOf("...")

    // db의 모든 아이디들
    val dbAllGId = mutableListOf<Long>()

    private val prefManager = PreferenceManager(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // category 초기화
            initType()
            // tag 초기화
            initTag()
            // 주기적으로 ggjs의 정보를 분석한다.
            getGgjs()
            // 서버에서 gId리스트를 가져온다. 크롤링해야할 gId들을 필터링한다.
            getGIdsAndFilterGids()

            // db에 없는 갤러리들을 크롤링한다
            crawlNewGalleries()
            // 오류 같은거로 미처 크롤링하지 못해 없는 gid가 있다면 크롤링한다 - 하루한번
            crawlMissedGalleries()
            // 하루한번 삭제된 갤러리들을 삭제한다
            deleteDeletedGalleries()
            // 1일에 한번 갤러리 태그 동기화를 한다. 1000까지
            syncGalleryTag1000()
            // 2일에 한번 갤러리 태그 동기화를 한다. 2000까지
            syncGalleryTag2000()
            // 4일에 한번 갤러리 태그 동기화를 한다. 4000까지
            syncGalleryTag3000()
            syncGalleryTag4000()
            // 8일에 한번 갤러리 태그 동기화를 한다. 8000까지
            syncGalleryTag5000()
            syncGalleryTag6000()
            syncGalleryTag7000()
            syncGalleryTag8000()
        }
    }

    suspend fun initType() = withContext(Dispatchers.IO) {
        val types = listOf("doujinshi", "manga", "artistcg", "gamecg", "imageset")
        for (type in types) {
            if (typeDao.findByNameNullable(type) == null)
                typeDao.insert(Type(name = type))
        }
    }

    suspend fun initTag() = withContext(Dispatchers.IO) {
        val tags = listOf("artist:null", "group:null", "parody:null")
        for (tag in tags) {
            if (tagDao.findByNameNullable(tag) == null)
                tagDao.insert(Tag(name = tag, likeStatus = 1))
        }
    }

    fun getGgjs() = viewModelScope.launch(Dispatchers.IO) {
        while (true) {
            // 히토미에서 ggjs의 정보를 가져와야 섬네일 정상 출력이 된다.
            try {
                val ggjs = hitomiApi.getGgjs()
                b.value = """(?<=b: ')[^']+""".toRegex().find(ggjs)?.value
                o1.value = """(?<=var o = )\d""".toRegex().find(ggjs)?.value
                thumbChar1.value = if (o1.value == "0") "a" else "b"
                o2.value = """(?<=o = )\d(?=; break;)""".toRegex().find(ggjs)?.value
                thumbChar2.value = if (o2.value == "0") "a" else "b"
                mList.clear()
                mList.addAll(
                    """(?<=case )\d+(?=:)""".toRegex().findAll(ggjs)
                        .map { matchResult -> matchResult.value })
            } catch (e: Exception) {
                Log.i("ggjs 얻기 오류", "${e.message}")
                crawlErrorStr.value = "${getCurrentFormattedTime()}: ggjs 얻기 오류: ${e.message}"
            }
            delay(1000 * 60 * 2)
        }
    }

    suspend fun getGIdsAndFilterGids() = withContext(Dispatchers.IO) {
        var isSuccess = false   // serverGidList는 실패하면 안되는 값이기 때문에 성공할 때까지 시도한다.

        while (!isSuccess) {
            try {
                val array = hitomiApi.getKoreanGids()
                serverGidList.addAll(byteArrayToIntList(array))
                Log.i("서버에 있는 갤러리 총 수", "${serverGidList.size}")
                // db에서 제일 큰 gid보다 큰 gid들만 크롤링할 id들에 포함시킨다. 이진탐색으로 함.
                val targetGid = galleryDao.findMaxGid().toInt()
                // 만약 정확한 값이 없다면, 그 값이 있어야 할 위치를 (-(insertion point) - 1) 형태로 반환함
                val searchIndex = serverGidList.binarySearch(targetGid, reverseOrder())
                // index가 0 이상이면 정확한 값이 있는 것이고, 음수면 '그 값보다 작은 첫 번째 값'의 위치가 나옴
                val limitIndex = if (searchIndex >= 0) {
                    searchIndex // maxGid와 같은 값은 제외해야 하므로, 그 앞까지만 포함
                } else {
                    -(searchIndex + 1) // maxGid보다 작은 첫 번째 값의 인덱스
                }
                filteredIds.addAll(serverGidList.subList(0, limitIndex))
                // 내림차순이기 때문에 오름차순으로 바꿔준다
                filteredIds.reverse()
                isSuccess = true
            } catch (e: Exception) {
                Log.i("갤러리 gId 리스트 얻기 오류", "${e.message}")
                crawlErrorStr.value =
                    "${getCurrentFormattedTime()}: 갤러리 gId 리스트 얻기 오류: ${e.message}"
                delay(5000) // 5초후 재시도
            }
        }
    }

    // 크롤링 관련 함수들
    suspend fun crawlNewGalleries() {
        while (true) {
            if (filteredIds.isNotEmpty()) {
                crawlList()
                delay(20)
            }
            if (filteredIds.isEmpty()) break
        }
    }

    suspend fun crawlList() = withContext(Dispatchers.IO) {
        crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 중"
        val gIds = filteredIds.take(crawlAmount)

        val deferredList = gIds.map { gId ->
            async {
                try {
                    val galleryInfo: GalleryInfo = hitomiApi.getGalleryInfo(gId)
                    val html = hitomiApi.getThumbnail(galleryInfo.id)
                    val doc = Jsoup.parse(html)
                    val imgs = doc.select(".dj-img-cont picture img")
                    val thumb1 = "https:${imgs[0].attr("data-src")}"
                    val thumb2 = "https:${imgs[1].attr("data-src")}"
                    return@async GalleryFullInfo(galleryInfo, thumb1, thumb2)
                } catch (e: Exception) {
                    Log.i("각종 정보 얻기 오류", "${e.message}")
                    crawlErrorStr.value =
                        "${getCurrentFormattedTime()}: crawlErrorStr: ${e.message}"
                    return@async null
                }
            }
        }
        val galleryFullInfos: List<GalleryFullInfo?> = deferredList.awaitAll()
        // 실제로 DB 저장을 시도할 성공 목록만 추출
        val successfulInfos = galleryFullInfos.filterNotNull()
        val successfulGIds = successfulInfos.map { it.galleryInfo.id.toInt() }
        // 저장할 때는 항상 작은 gid부터 저장된다.
        for (gFI in successfulInfos) {
            try {
                // 갤러리 저장. 갤러리 저장과 갤러리토큰 저장을 하나의 트랜잭션에 묶는다.
                repository.insertGalleryInfo(gFI.galleryInfo, gFI.thumb1, gFI.thumb2)
            } catch (e: Exception) {
                Log.i("썸네일 크롤링 오류", "${e.message}")
                crawlErrorStr.value = "${getCurrentFormattedTime()}: 썸네일 크롤링 오류: ${e.message}"
            }

        }
        // 크롤링 한 것들은 리스트에서 제외.
        filteredIds.removeAll(successfulGIds)
        crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 끝"
        if (filteredIds.isEmpty()) {
            crawlStatusStr.value = "${getCurrentFormattedTime()}: 모든 크롤링을 마쳤습니다"
        }
    }

    suspend fun crawlMissedGalleries() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastCrawlMissedGalleries.first()
        // 하루마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24) {
            crawlStatusStr.value = "미처 크롤링하지 못한 갤러리 크롤링"
            // 크롤링 동작.
            filteredIds.clear()
            filteredIds.addAll(serverGidList)
            // db에 있는 id들은 없앤다.
            filteredIds.removeIf { galleryDao.existsById(it.toLong()) }
            // filteredIds에 넣어놨으니 crawlNewGalleries()를 호출해도 된다.
            crawlNewGalleries()
            // 마지막 실행 시간 업데이트
            prefManager.updateLastCrawlMissedGalleries(now)
            crawlStatusStr.value = "미처 크롤링하지 못한 갤러리 크롤링 완료"
        }
    }

    // 삭제된 갤러리는 db에서 지운다.
    suspend fun deleteDeletedGalleries() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastDeleteDeletedGallery.first()
        // 이틀마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 2) {
            crawlStatusStr.value = "hitomi에서 삭제된 갤러리 db에서 삭제중"
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            // db에는 있는데 서버에는 없는 gid라면 삭제해야한다.
            val idsToDelete = dbAllGId.toSet() - serverGidList.map { it.toLong() }.toSet()
            idsToDelete.map { gId ->
                repository.removeGalleryInfo(gId)
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastDeleteDeletedGallery(now)
            crawlStatusStr.value = "삭제된 갤러리 db에서 삭제 완료"
        }
    }

    // 갤러리_태그 동기화
    suspend fun syncGalleryTag1000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag1000.first()
        // 1000까지는 하루마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 0..999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag1000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 1000까지)"
        }
    }

    suspend fun syncGalleryTag2000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag2000.first()
        // 2000까지는 2일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 2) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 1000..1999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag2000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 2000까지)"
        }
    }

    suspend fun syncGalleryTag3000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag3000.first()
        // 3000까지는 4일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 4) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 2000..2999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag3000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 3000까지)"
        }
    }

    suspend fun syncGalleryTag4000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag4000.first()
        // 4000까지는 4일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 4) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 3000..3999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag4000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 4000까지)"
        }
    }

    suspend fun syncGalleryTag5000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag5000.first()
        // 5000까지는 8일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 4000..4999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag5000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 5000까지)"
        }
    }

    suspend fun syncGalleryTag6000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag6000.first()
        // 6000까지는 8일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 5000..5999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag6000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 6000까지)"
        }
    }

    suspend fun syncGalleryTag7000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag7000.first()
        // 7000까지는 8일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 6000..6999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag7000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 7000까지)"
        }
    }

    suspend fun syncGalleryTag8000() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastDaily = prefManager.lastSyncGalleryTag8000.first()
        // 8000까지는 8일마다 한번 동작
        if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
            if (dbAllGId.isEmpty())
                dbAllGId.addAll(galleryDao.findAllGId())
            for (i: Int in 7000..7999 step (crawlAmount)) {
                crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
                syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
            }
            // 마지막 실행 시간 업데이트
            prefManager.updateLastSyncGalleryTag8000(now)
            crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 8000까지)"
        }
    }

    suspend fun syncGalleryTag(gIdList: List<Long>) = withContext(Dispatchers.IO) {
        val deferredList = gIdList.map { gId ->
            async {
                try {
                    val galleryInfo: GalleryInfo = hitomiApi.getGalleryInfo(gId.toInt())
                    // gId로 히토미 에서 갤러리에 해당되는 태그 조회
                    // 서버 태그 수집 시 :null 규칙 적용
                    val serverTagNames = mutableListOf<String>()
                    // 각 카테고리별로 데이터가 없으면 ":null"을 추가함 (기존 insert 로직과 동일하게)
                    if (galleryInfo.artists.isNullOrEmpty()) serverTagNames.add("artist:null")
                    else serverTagNames.addAll(galleryInfo.artists.map { it.toArtistString() })
                    if (galleryInfo.groups.isNullOrEmpty()) serverTagNames.add("group:null")
                    else serverTagNames.addAll(galleryInfo.groups.map { it.toGroupString() })
                    if (galleryInfo.parodys.isNullOrEmpty()) serverTagNames.add("parody:null")
                    else serverTagNames.addAll(galleryInfo.parodys.map { it.toParodyString() })
                    // tags와 characters는 null일 때 아무것도 안 넣으셨으므로 그대로 유지
                    galleryInfo.tags?.let { serverTagNames.addAll(it.map { t -> t.toTagString() }) }
                    galleryInfo.characters?.let { serverTagNames.addAll(it.map { c -> c.toCharacterString() }) }

                    // db에서 해당 갤러리의 태그들을 가져온다.
                    val galleryTagList: List<GalleryTag> = galleryTagDao.findByGid(gId)
                    val dbTagNames =
                        tagDao.findByIds(galleryTagList.map { it.tagId }).map { it.name }
                    // 히토미 태그와 db태그가 다르다면, 태그를 추가하거나 없앤다.
                    val tagsToAdd = serverTagNames.toSet() - dbTagNames.toSet()
                    val tagsToRemove = dbTagNames.toSet() - serverTagNames.toSet()
                    if (tagsToAdd.isNotEmpty() || tagsToRemove.isNotEmpty()) {
                        repository.syncGalleryTags(gId, tagsToAdd.toList(), tagsToRemove.toList())
                    }
                } catch (e: Exception) {
                    Log.i("갤러리태그 동기화 $gId 중 오류", "${e.message}")
                }
            }
        }
        deferredList.awaitAll()
    }

    fun byteArrayToIntList(bytes: ByteArray): List<Int> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val result = mutableListOf<Int>()
        while (buffer.remaining() >= 4) {
            result.add(buffer.int)
        }
        return result
    }

    fun getCurrentFormattedTime(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yy년 MM월 dd일 HH시 mm분 ss초")
        return current.format(formatter)
    }
}

data class GalleryFullInfo(
    val galleryInfo: GalleryInfo,
    val thumb1: String,
    val thumb2: String
)