package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.khitomiviewer.json.TagsAndGalleries
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.MyRepository
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.room.entity.Type
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.jsoup.Jsoup
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import androidx.core.content.edit
import kotlinx.serialization.ExperimentalSerializationApi

class KHitomiViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val typeDao = DatabaseProvider.getDatabase(application).typeDao()
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()
    private val imageUrlDao = DatabaseProvider.getDatabase(application).imageUrlDao()
    private val db = DatabaseProvider.getDatabase(application)
    private val repository = MyRepository(db)

    private val prefs = application.getSharedPreferences("MySettings", MODE_PRIVATE)

    // 크롤링에 사용되는 변수
    val crawlAmount = 100 // 한번에 몇개씩 크롤링 할지
    val delayS = mutableLongStateOf(prefs.getLong("delayS", 60L)) //몇초마다 크롤링 할지
    val crawlOn = mutableStateOf(prefs.getBoolean("crawlOn", false))
    val crawling = mutableStateOf(false) //백업중에 크롤링을 하면 안된다.
    val crawlErrorStr = mutableStateOf("아직까지 에러가 없습니다")
    val crawlStatusStr = mutableStateOf("...")
    val hitomiClient = HttpClient(CIO) {
        defaultRequest {
            headers {
                append("Referer", "https://hitomi.la/")
//                append("Origin", "https://hitomi.la/")
//                append("Range", "bytes=0-99")
            }
        }
    }
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        prettyPrint = true
    }
    val b = mutableStateOf<String?>("1 or 2")
    val mDefaultThumbChar = mutableStateOf<String?>("a") // a or b
    val mNextThumbChar = mutableStateOf<String?>("b") // b or a
    val mDefaultO = mutableStateOf<String?>("0") // 0 or 1
    val mNextO = mutableStateOf<String?>("1") // 1 or 0
    val mList = mutableStateListOf<String>()
    val gIdList = mutableListOf<Int>()

    // 메인 리스트 ui에 사용되는 변수
    private val _pageCount = MutableStateFlow<Long>(10)
    val pageCount: StateFlow<Long> = _pageCount
    var pageSize by mutableIntStateOf(prefs.getInt("pageSize", 20))
    var galleries by mutableStateOf<List<GalleryFullDto>>(emptyList())
        private set
    var lastTags = mutableStateListOf<Tag>()
    var loading = mutableStateOf(true)
    var lastPageSize: Int = pageSize
    var lastShowTypeIdList: MutableList<Long> = mutableListOf()

    // 세팅 화면에 사용되는 변수
    val tagLikeCheck = mutableStateOf(prefs.getBoolean("tagLikeCheck", false))
    val galleryLikeCheck = mutableStateOf(prefs.getBoolean("galleryLikeCheck", false))
    var tagSearchList by mutableStateOf<List<Tag>>(emptyList())
    var lastTagSearchKeyword = mutableStateOf<String>("artist:try")
    var showTypeIdList = Json.decodeFromString<List<Long>>(prefs.getString("showTypeIdList", "[1,2,3,4,5]").toString()).toMutableList()

    // 다이얼로그. 태그 (싫어요,기본,좋아요,구독) 갤러리 DISLIKE, NONE, LIKE선택하는것.
    val selectedTagNameOrGalleryId = mutableStateOf("")
    val likeStatus = mutableIntStateOf(0)
    val whatStr = mutableStateOf("")

    // 하나의 갤러리를 보여주는 view ui에 사용되는 변수
    private var lastGid: Long? = null
    var imageUrls = mutableStateListOf<String>()
        private set
    
    // db 내보내기 불러오기에 사용되는 변수
    val dbExportImportStr = mutableStateOf("내보내기 및 불러오기를 할 수 있습니다")

    private var lastPage: Long = 98765
    private var lastTagIdListJson: String? = null
    var lastTitleKeyword: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // category 초기화
            initType()
            // tag 초기화
            initTag()
            crawlLoop()
        }
    }

    suspend fun initType() = withContext(Dispatchers.IO) {
        val types = listOf("doujinshi", "manga", "artistcg", "gamecg", "imageset")
        for (type in types) {
            if(typeDao.findByNameNullable(type) == null)
                typeDao.insert(Type(name = type))
        }
    }

    suspend fun initTag() = withContext(Dispatchers.IO) {
        val tags = listOf("artist:null", "group:null", "parody:null")
        for (tag in tags) {
            if(tagDao.findByNameNullable(tag) == null)
                tagDao.insert(Tag(name = tag, likeStatus = 1))
        }
    }

    // 크롤링 관련 함수들
    suspend fun crawlLoop() {
        // 처음에는 서버에서 gId리스트를 가져오자.
        getGIds()
        // 주기적으로 ggjs의 정보를 분석하자
        getGgjs()
        while (true) {
            if(crawlOn.value && gIdList.isNotEmpty()) {
                crawlList()
            }
            delay(delayS.longValue * 1000)
        }
    }

    fun toggleCrawlOn() {
        // db백업/불러오기 중에는 변경할 수 없다.
        if(dbExportImportStr.value == "백업중" || dbExportImportStr.value == "불러오기중")
            crawlOn.value = false

        if(crawlOn.value) {
            crawlOn.value = false
            prefs.edit { putBoolean("crawlOn", false) }
        }
        else {
            crawlOn.value = true
            prefs.edit { putBoolean("crawlOn", true) }
        }
    }

    fun setDelayS(l: Long) {
        delayS.longValue = l
        prefs.edit { putLong("delayS", l) }
    }

    suspend fun crawlList() = withContext(Dispatchers.IO) {
        if(gIdList.isEmpty())
            return@withContext
        crawling.value = true
        crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 중"
        val gIds = gIdList.subList(0, min(gIdList.size, crawlAmount))

        // "https://ltn.gold-usergeneratedcontent.net/galleries/${gId}.js"에서 각종 정보(태그 등)를 얻는다.
        val deferreds = gIds.map { gId ->
            async {
                try {
                    val response = hitomiClient.get("https://ltn.gold-usergeneratedcontent.net/galleries/${gId}.js")
                    val body = response.bodyAsText().replace("var galleryinfo = ", "")
                    return@async json.decodeFromString<GalleryInfo>(body)
                } catch (e: Exception) {
                    Log.i("각종 정보 얻기 오류", "${e.message}")
                    crawlErrorStr.value = "${getCurrentFormattedTime()}: crawlErrorStr: ${e.message}"
                    return@async null
                }
            }
        }
        val galleryInfos: List<GalleryInfo?> = deferreds.awaitAll()
        Log.i("각종 정보", galleryInfos.toString())
        //섬네일은 "https://ltn.gold-usergeneratedcontent.net/galleryblock/${gId}.html"에서 얻어야한다.
        for (ginfo in galleryInfos) {
            if(ginfo != null) {
                try {
                    val response = hitomiClient.get("https://ltn.gold-usergeneratedcontent.net/galleryblock/${ginfo.id}.html")
                    val html = response.bodyAsText()
                    val doc = Jsoup.parse(html)
                    val imgs = doc.select(".dj-img-cont picture img")
                    var thumb1 = "https:${imgs[0].attr("data-src")}"
                    var thumb2 = "https:${imgs[1].attr("data-src")}"
                    // 갤러리 저장. 갤러리 저장과 갤러리토큰 저장을 하나의 트랜잭션에 묶는다.
                    repository.insertGalleryInfo(ginfo, thumb1, thumb2)
                    // 저장한 것은 리스트에서 제외
//                    gIdList.removeIf{it == ginfo.id.toInt()}
                } catch (e: Exception) {
                    Log.i("썸네일 크롤링 오류", "${e.message}")
                    crawlErrorStr.value = "${getCurrentFormattedTime()}: 썸네일 크롤링 오류: ${e.message}"
                }
            }
        }
        // 크롤링 한 것들은 리스트에서 제외
        gIdList.removeIf { it in gIds }
        crawling.value = false
        crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 끝"
        if(gIdList.isEmpty()) {
            crawlOn.value = false
            crawlStatusStr.value = "${getCurrentFormattedTime()}: 모든 크롤링을 마쳤습니다"
        }
    }

    suspend fun getGIds() = withContext(Dispatchers.IO) {
        try {
            val url = "https://ltn.gold-usergeneratedcontent.net/index-korean.nozomi"
            val response = hitomiClient.get(url)
            val array = response.bodyAsBytes()
            gIdList.addAll(byteArrayToIntList(array))
            Log.i("서버에 있는 갤러리 총 수", "${gIdList.size}")
            // 삭제된 갤러리는 db에서 지운다.
            val allGId = mutableListOf<Long>()
            allGId.addAll(galleryDao.findAllGId())
            allGId.removeIf { it.toInt() in gIdList }
            allGId.map { gId ->
                repository.removeGalleryInfo(gId)
            }
            // 이중에서 있는것들은 제외한다.
            gIdList.removeIf { galleryDao.existsById(it.toLong()) }
            Log.i("없는 갤러리 수", "${gIdList.size}")
        } catch (e: Exception) {
            Log.i("갤러리 gId 리스트 얻기 오류", "${e.message}")
            crawlErrorStr.value = "${getCurrentFormattedTime()}: 갤러리 gId 리스트 얻기 오류: ${e.message}"
        }
    }

    fun getGgjs() = viewModelScope.launch(Dispatchers.IO) {
        while(true) {
            // 히토미에서 ggjs의 정보를 가져와야 섬네일 정상 출력이 된다.
            try {
                val response = hitomiClient.get("https://ltn.gold-usergeneratedcontent.net/gg.js")
                val ggjs = response.bodyAsText()
                b.value = """(?<=b: ')[^']+""".toRegex().find(ggjs)?.value
                mDefaultO.value = """(?<=var o = )\d""".toRegex().find(ggjs)?.value
                mDefaultThumbChar.value = if(mDefaultO.value == "0") "a" else "b"
                mNextO.value = """(?<=o = )\d(?=; break;)""".toRegex().find(ggjs)?.value
                mNextThumbChar.value = if(mNextO.value == "0") "a" else "b"
                mList.clear()
                mList.addAll("""(?<=case )\d+(?=:)""".toRegex().findAll(ggjs).map { matchResult->matchResult.value })
            } catch (e: Exception) {
                Log.i("ggjs 얻기 오류", "${e.message}")
                crawlErrorStr.value = "${getCurrentFormattedTime()}: ggjs 얻기 오류: ${e.message}"
            }
            delay(1000 * 60)
        }
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

    // 메인 리스트 페이지에서 실행하는 함수
    fun setGalleryList(page: Long, tagIdListJson: String?, titleKeyword: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            var sameCount = false
            // 같은 조건이라면 count는 다시 쿼리할 필요가 없다.
            // tagIdListJson, titleKeyword, pageSize, showTypeIdList
            if(lastTagIdListJson == tagIdListJson && lastTitleKeyword == titleKeyword &&
                lastPageSize == pageSize && lastShowTypeIdList == showTypeIdList) {
                sameCount = true
            }

            if(lastPage != page || lastTagIdListJson != tagIdListJson || lastTitleKeyword != titleKeyword) {
                loading.value = true
            }
            lastPage = page
            lastTagIdListJson = tagIdListJson
            lastTitleKeyword = titleKeyword
            lastPageSize = pageSize
            lastShowTypeIdList = showTypeIdList.toMutableList()

            val tagIdList = mutableListOf<Long>()
            lastTags.clear()
            if(tagIdListJson != null && tagIdListJson.isNotEmpty()) {
                tagIdList.addAll(Json.decodeFromString(tagIdListJson))
                lastTags.addAll(tagDao.findByIds(tagIdList))
            }

            val galleryLikeList = if(galleryLikeCheck.value) listOf(2) else listOf(1, 2)
            val tagLikeList = if(tagLikeCheck.value) listOf(2) else listOf(1, 2)

            // 갤러리 필터. db 크기가 커지니까 너무 오래걸리는데?
            var galleryList: List<Gallery>
            var count: Long
            if(tagIdList.isEmpty()) {
                if(titleKeyword.isNullOrBlank())
                    galleryList = galleryDao.findByCondition(pageSize, (page - 1) * pageSize, galleryLikeList, tagLikeList, showTypeIdList)
                else
                    galleryList = galleryDao.findByConditionTitleKeyword(pageSize, (page - 1) * pageSize, galleryLikeList, tagLikeList, showTypeIdList, "%${titleKeyword}%")
            }
            else {
                galleryList = galleryDao.findByConditionQuery(buildFindByConditionQuery(page, galleryLikeList, tagLikeList, tagIdList, titleKeyword))
            }

            if(!sameCount) {
                if(tagIdList.isEmpty()) {
                    if(titleKeyword.isNullOrBlank())
                        count = galleryDao.countByCondition(galleryLikeList, tagLikeList, showTypeIdList)
                    else
                        count = galleryDao.countByConditionTitleKeyword(galleryLikeList, tagLikeList, showTypeIdList, "%${titleKeyword}%")
                }
                else {
                    count = galleryDao.countByConditionQuery(buildCountByConditionQuery(galleryLikeList, tagLikeList, tagIdList, titleKeyword))
                }
                _pageCount.value = count / pageSize + if (count % pageSize > 0) 1 else 0
            }

            galleries = galleryList.map { g ->
                GalleryFullDto(
                    gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                    date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                    typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                    tags = galleryTagDao.findByGid(g.gId).map { gt -> tagDao.findById(gt.tagId) }
                )
            }
            loading.value = false
        }
    }

    fun buildFindByConditionQuery(page: Long, galleryLikeList: List<Int>, tagLikeList: List<Int>, tagIdList: List<Long>, titleKeyword: String?): SupportSQLiteQuery {
        val limit = pageSize
        val offset = (page - 1) * pageSize
        val args = mutableListOf<Any>()

        // 쿼리를 만든다.
        val queryBuilder = StringBuilder("""
                select * from gallery g
                where g.likeStatus in (${galleryLikeList.joinToString(","){"?"}})
                and g.typeId in (${showTypeIdList.joinToString(","){"?"}})
            """.trimIndent())
        args.addAll(galleryLikeList)
        args.addAll(showTypeIdList)

        // 특정 titleKeyword가 포함돼야 함
        if(titleKeyword != null && titleKeyword.isNotEmpty()) {
            queryBuilder.append(""" and g.title like ? """)
            args.add("%${titleKeyword}%")
        }

        queryBuilder.append("""
                and not exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus = 0
                )
                and exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus in (${tagLikeList.joinToString(","){"?"}}) 
                )
        """.trimIndent())
        args.addAll(tagLikeList)

        // 특정 tagId들이 포함돼야 함
        if(tagIdList.isNotEmpty()) {
            queryBuilder.append("""
                and (
                    select count(gt.galleryTagId) from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.tagId in (${tagIdList.joinToString(","){"?"}}) 
                ) = ?
            """.trimIndent())
            args.addAll(tagIdList)
            args.add(tagIdList.size)
        }

        queryBuilder.append(""" order by gId desc limit ? offset ? """)
        args.add(limit)
        args.add(offset)

        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    fun buildCountByConditionQuery(galleryLikeList: List<Int>, tagLikeList: List<Int>, tagIdList: List<Long>, titleKeyword: String?): SupportSQLiteQuery {
        val args = mutableListOf<Any>()

        // 쿼리를 만든다.
        val queryBuilder = StringBuilder("""
            select count(*) from (
                select g.gId from gallery g
                where g.likeStatus in (${galleryLikeList.joinToString(","){"?"}})
                and g.typeId in (${showTypeIdList.joinToString(","){"?"}})
            """.trimIndent())
        args.addAll(galleryLikeList)
        args.addAll(showTypeIdList)

        // 특정 titleKeyword가 포함돼야 함
        titleKeyword?.let {
            queryBuilder.append(""" and g.title like ? """)
            args.add("%${titleKeyword}%")
        }

        queryBuilder.append("""
                and not exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus = 0
                )
                and exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus in (${tagLikeList.joinToString(","){"?"}}) 
                )
        """.trimIndent())
        args.addAll(tagLikeList)

        // 특정 tagId들이 포함돼야 함
        if(tagIdList.isNotEmpty()) {
            queryBuilder.append("""
                and (
                    select count(gt.galleryTagId) from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.tagId in (${tagIdList.joinToString(","){"?"}}) 
                ) = ?
            """.trimIndent())
            args.addAll(tagIdList)
            args.add(tagIdList.size)
        }

        queryBuilder.append(""" ) """)

        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    fun setDialog(what: String, tagNameOrGalleryId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            whatStr.value = what
            val realTagNameOrGalleryId = tagNameOrGalleryId.replace("♥", "")
            if(what == "tag") {
                val findByName = tagDao.findByName(realTagNameOrGalleryId)
                likeStatus.intValue = findByName.likeStatus
            }
            else {
                val findById = galleryDao.findById(realTagNameOrGalleryId.toLong())
                likeStatus.intValue = findById.likeStatus
            }
            selectedTagNameOrGalleryId.value = realTagNameOrGalleryId
        }
    }

    fun changeLike(like: Int, screen: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if(whatStr.value == "tag") {
                val findByName = tagDao.findByName(selectedTagNameOrGalleryId.value)
                tagDao.update(Tag(tagId = findByName.tagId, name = findByName.name, likeStatus = like))
            }
            else {
                val g = galleryDao.findById(selectedTagNameOrGalleryId.value.toLong())
                galleryDao.update(Gallery(g.gId, g.title, g.thumb1, g.thumb2, g.date, g.filecount, like, g.typeId))
            }
            // ui 재로딩을 위해 갤러리 다시 세팅
            if(screen == "ListScreen") {
                setGalleryList(lastPage, lastTagIdListJson, lastTitleKeyword)
            }
        }
    }

    // 만화 보기 화면 함수들
    fun setGalleryImages(gId: Long, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // 중복실행 방지
        if (lastGid == gId) return@launch
        lastGid = gId

        imageUrls.clear()

        val b = b.value
        val mDefault = mDefaultO.value
        val mNext = mNextO.value
        if(mDefault != null && mNext != null) {
            imageUrls.addAll(imageUrlDao.findByGId(gId).map { imageUrl ->
                // 여러가지 정보를 조합해 이미지 url을 만든다.
                // 구형폰에서는 avif 디코딩을 지원하지 않는거 같다. webp로 하자.어쩔 수 없다.
                val hash = imageUrl.hash
                val s = "${hash[hash.length-1]}${hash[hash.length-3]}${hash[hash.length-2]}".toInt(16).toString(10)
                val subdomainNum = (if(s in mList) mNext.toInt() else mDefault.toInt()) + 1
//                val subdomainChar = if(imageUrl.extension == "avif") "a" else "w"
//                "https://${subdomainChar}${subdomainNum}.gold-usergeneratedcontent.net/${b}${s}/${hash}.${imageUrl.extension}"
                "https://w${subdomainNum}.gold-usergeneratedcontent.net/${b}${s}/${hash}.webp"
            })
        }
    }

    // 메인 세팅 스크린 함수들
    fun searchTagsFromDb(keyword: String, selectedTagList: MutableList<Tag>) = viewModelScope.launch(Dispatchers.IO) {
        if(keyword.isBlank()) {
            tagSearchList = emptyList()
            return@launch
        }
        lastTagSearchKeyword.value = keyword
        tagSearchList = tagDao.findByKeyword("%${keyword}%", selectedTagList.map { tag -> tag.tagId })
    }

    fun setTagLikeCheck(bool: Boolean) {
        tagLikeCheck.value = bool
        prefs.edit { putBoolean("tagLikeCheck", bool) }
    }

    fun setGalleryLikeCheck(bool: Boolean) {
        galleryLikeCheck.value = bool
        prefs.edit { putBoolean("galleryLikeCheck", bool) }
    }

    fun settingPageSize(i: Int) {
        pageSize = i
        prefs.edit { putInt("pageSize", i) }
    }

    fun toggleDarkMode(isDark: MutableState<Boolean>) {
        if(isDark.value) {
            isDark.value = false
            prefs.edit { putBoolean("isDark", false) }
        }
        else {
            isDark.value = true
            prefs.edit { putBoolean("isDark", true) }
        }
    }

    fun typeOnOff(typeId: Long, flag: Boolean) {
        if(flag) {
            if(typeId !in showTypeIdList) {
                showTypeIdList.add(typeId)
                prefs.edit { putString("showTypeIdList", Json.encodeToString(showTypeIdList)) }
            }
        }
        else {
            if(typeId in showTypeIdList) {
                showTypeIdList.remove(typeId)
                prefs.edit { putString("showTypeIdList", Json.encodeToString(showTypeIdList)) }
            }
        }
    }

    // 내보내기 불러오기 함수들
    @OptIn(ExperimentalSerializationApi::class)
    fun backupDatabaseToFolder(context: Context, folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if(crawling.value) {
                dbExportImportStr.value = "크롤링 중에는 할 수 없습니다"
                return@launch
            }
            val lastCrawlOn = crawlOn.value
            crawlOn.value = false

            dbExportImportStr.value = "내보내기중"
            val backupFileUri = createBackupFileInFolder(context, folderUri, "KHitomiViewer좋아요싫어요정보.json")
            val tagsAndGalleries: TagsAndGalleries = TagsAndGalleries(galleries = galleryDao.findNotNone(), tags = tagDao.findNotNone())
            if (backupFileUri != null) {
                context.contentResolver.openOutputStream(backupFileUri)?.use { outputStream ->
                    Json.encodeToStream(tagsAndGalleries, outputStream)
                    dbExportImportStr.value = "내보내기 완료"
                }
            } else {
                dbExportImportStr.value = "이미 KHitomiViewer좋아요싫어요정보.json 이 존재합니다."
            }

            crawlOn.value = lastCrawlOn
        }
    }

    fun createBackupFileInFolder(context: Context, folderUri: Uri, fileName: String): Uri? {
        val docId = DocumentsContract.getTreeDocumentId(folderUri)
        val folderDocumentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)

        // 자식 문서 확인: 이미 동일한 이름이 있으면 오류 처리
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while(cursor.moveToNext()) {
                val existingName = cursor.getString(0)
                if(existingName == fileName) {
                    // 파일이 이미 존재하므로 null 반환 (에러 처리)
                    return null
                }
            }
        }

        // 존재하지 않으면 새 파일 생성
        return DocumentsContract.createDocument(
            context.contentResolver,
            folderDocumentUri,
            "application/json",
            fileName
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun importFromJsonFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if(crawling.value) {
                dbExportImportStr.value = "크롤링 중에는 할 수 없습니다"
                return@launch
            }
            val lastCrawlOn = crawlOn.value
            crawlOn.value = false

            dbExportImportStr.value = "불러오기중"
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val tagsAndGalleries = Json.decodeFromStream<TagsAndGalleries>(inputStream)
                    // db 에 좋아요/싫어요 정보 넣기
                    repository.updateLikeDislikeInfo(tagsAndGalleries)
                } ?: throw IOException("InputStream이 null입니다")
            } catch (e: Exception) {
                dbExportImportStr.value = "불러오기 실패: ${e.message}"
            }
            dbExportImportStr.value = "불러오기 완료"

            crawlOn.value = lastCrawlOn
        }
    }
}