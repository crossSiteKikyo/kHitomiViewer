package com.example.khitomiviewer.screen

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.example.khitomiviewer.R
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import com.example.khitomiviewer.viewmodel.LikeSettingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryLikeSettingScreen(navController: NavHostController, mainViewModel: KHitomiViewerViewModel) {
    val viewModel: LikeSettingViewModel = viewModel()
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val tabs = listOf("갤러리 좋아요", "갤러리 싫어요")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect("gallery") {
        viewModel.setTagOrGalleryList("gallery")
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState: ScrollState = rememberScrollState()
    val interactionSource = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(true) }
    var isDragging = interactionSource.collectIsDraggedAsState()

    // 값이 변할 때마다 타이머 재시작
    LaunchedEffect(scrollState.value, isDragging.value) {
        if(!isDragging.value) {
            visible = true
            delay(1000L) // 1초 후 자동 숨김
            visible = false
        }
        else {
            visible = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Row (
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                FloatingActionButton(
                    onClick = {coroutineScope.launch {scrollState.scrollTo(0)}},
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "top")
                }

                FloatingActionButton(
                    onClick = {coroutineScope.launch {scrollState.scrollTo(scrollState.maxValue)}},
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, "bottom")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = {
                                selectedTabIndex = index
                                if(index == 0) // 좋아요 갤러리
                                    viewModel.loadGallery(2)
                                else // 싫어요 갤러리
                                    viewModel.loadGallery(0)
                            },
                            text = { Text(title) }
                        )
                    }
                }
                // 다이얼로그
                if(showDialog.value) {
                    val likeStatusIntToStr = listOf("싫어요 \uD83C\uDD96", "기본", "좋아요 ♥")
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {TextButton(onClick = { showDialog.value = false }) {
                            Text("닫기")
                        }},
                        title = {
                            val what = if(viewModel.whatStr.value == "tag") "태그" else "갤러리"
                            Text("${what} - ${viewModel.selectedTagNameOrGalleryId.value}")
                        },
                        text = {
                            Column (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                for (x in 0..2) {
                                    if(x == viewModel.likeStatus.intValue)
                                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(likeStatusIntToStr[x])}
                                    else
                                        Button(onClick = {
                                            // db에 상태 변경
                                            viewModel.changeLike(x)
                                            // 다이얼로그 닫기
                                            showDialog.value = false
                                        }) { Text(likeStatusIntToStr[x])}
                                }
                            }
                        })
                }
                val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()
                val typeColor: Map<Int, Long> = remember { mapOf<Int, Long>(
                    1 to 0xFFCC9999,  // c99 "doujinshi"
                    2 to 0xFFCC99CC,  // c9c "manga"
                    3 to 0xFF99CCCC,  // 9cc "artistcg"
                    4 to 0xFF9999CC,  // 99c "gamecg"
                    5 to 0xFF999999,  // 999 "imageset"
                    // CC보다 크면 안되고 66보다 작으면 안된다.
                ) }
                val realHitomiThumbnailUrl: (String) -> String = { url ->
                    val hash = """[0-9a-z]{40,}""".toRegex().find(url)?.value
                    if (hash == null) {
                        url
                    }
                    else {
                        val s = "${hash[hash.length-1]}${hash[hash.length-3]}${hash[hash.length-2]}".toInt(16).toString(10)
                        val ch = if(s in mainViewModel.mList) mainViewModel.mNextThumbChar.value else mainViewModel.mDefaultThumbChar.value
                        url.replace("tn.hitomi.la", "${ch}tn.gold-usergeneratedcontent.net")
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (g in viewModel.galleries) {
                        val cardBorderColor:Color = when(g.likeStatus) {
                            2 -> Color(0xFFCC00CC)
                            else -> Color.Gray
                        }
                        val widthFloat1:Float = when(g.likeStatus) {
                            2 -> 0.5f
                            else -> 0.2f
                        }
                        val widthFloat2:Float = when(g.likeStatus) {
                            2 -> 1f
                            else -> 0.25f
                        }
                        Card(
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            border = BorderStroke(2.dp, cardBorderColor)
                        ) {
                            val titleColorLong = typeColor[g.typeId.toInt()] ?: 0xFFEE44EE
                            val titleColor = Color(titleColorLong)
                            val typeBgColor = Color(titleColorLong + 0x333333)
                            val textColor = Color(titleColorLong - 0x666666)
                            // 제목
                            Text(
                                g.title.replace("""\[[\s\S]+?\]""".toRegex(), "").replace("""\([\s\S]+?\)""".toRegex(), "").trim(),
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(titleColor)
                                    .padding(2.dp),
                                color = Color.White,
                                style = TextStyle(shadow = Shadow(Color(0xFF000000), blurRadius = 7f), fontSize = 21.sp)
                            )
                            // 타입
                            Text(
                                "종류: ${g.typeName}",
                                color = textColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(typeBgColor)
                                    .padding(horizontal = 3.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                            // 작가
                            val artists = g.tags.filter { tag -> tag.name.startsWith("artist:") }
                            if(artists.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                ) {
                                    Text("작가: ", color = textColor)
                                    for (i in artists.indices) {
                                        if(i != 0)
                                            Text(", ", color = textColor)
                                        MainTagText(artists[i], textColor, mainViewModel, showDialog, navController)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                            }
                            // 그룹
                            val groups = g.tags.filter { tag -> tag.name.startsWith("group:") }
                            if(groups.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                ) {
                                    Text("그룹: ", color = textColor)
                                    for (i in groups.indices) {
                                        if(i != 0)
                                            Text(", ", color = textColor)
                                        MainTagText(groups[i], textColor, mainViewModel, showDialog, navController)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                            }
                            // 시리즈
                            val parodies = g.tags.filter { tag -> tag.name.startsWith("parody:") }
                            if(parodies.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                ) {
                                    Text("시리즈: ", color = textColor)
                                    for (i in parodies.indices) {
                                        if(i != 0)
                                            Text(", ", color = textColor)
                                        MainTagText(parodies[i], textColor, mainViewModel, showDialog, navController)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                            }
                            // 캐릭터
                            val characters = g.tags.filter { tag -> tag.name.startsWith("character:") }
                            if(characters.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().background(color = typeBgColor).padding(horizontal = 3.dp)
                                ) {
                                    Text("캐릭터: ", color = textColor)
                                    for (i in characters.indices) {
                                        if(i != 0)
                                            Text(", ", color = textColor)
                                        MainTagText(characters[i], textColor, mainViewModel, showDialog, navController)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(color = titleColor))
                            }
                            // 이미지 미리보기
                            Row (
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("MangaViewScreen/${g.gId}")
                                        },
                                        onLongClick = {
                                            viewModel.setDialog("gallery", g.gId.toString())
                                            showDialog.value = true
                                        }
                                    )
                            ){
                                AsyncImage(
//                                    model = "https://tn.gold-usergeneratedcontent.net/webpbigtn/6/67/9a7943d3898b3f275ef4d5f6c9f012ee806e043f3c52fdc292caffbb9b988676.webp",
                                    model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb1))
                                        .httpHeaders(hitomiHeaders).build(),
                                    contentDescription = "thumbnail",
                                    placeholder = painterResource(R.drawable.loading),
                                    error = painterResource(R.drawable.errorimg),
                                    onError = {e -> Log.i("섬네일 에러", e.toString())},
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopStart,
                                    modifier = Modifier
                                        .fillMaxWidth(widthFloat1)
                                        .border(1.dp, Color.Black),
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(realHitomiThumbnailUrl(g.thumb2))
                                        .httpHeaders(hitomiHeaders).build(),
                                    contentDescription = "thumbnail",
                                    placeholder = painterResource(R.drawable.loading),
                                    error = painterResource(R.drawable.errorimg),
                                    onError = {e -> Log.i("섬네일 에러", e.toString())},
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopStart,
                                    modifier = Modifier
                                        .fillMaxWidth(widthFloat2)
                                        .border(1.dp, Color.Black),
                                )
                            }
                            // 나머지 태그들
                            val males = g.tags.filter { tag -> tag.name.startsWith("male:") }
                            val females = g.tags.filter { tag -> tag.name.startsWith("female:") }
                            val others = g.tags.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                                    && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(3.dp)
                            ) {
                                for (tag in males) {
                                    TagText(tag, mainViewModel, showDialog, navController)
                                }
                                for (tag in females) {
                                    TagText(tag, mainViewModel, showDialog, navController)
                                }
                                for (tag in others) {
                                    TagText(tag, mainViewModel, showDialog, navController)
                                }
                            }
                            // 갤러리id   포스트 시간   페이지 수
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 갤러리 id
                                Text("${g.gId}",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                )
                                // 포스트 시간
                                Text(
                                    g.date,
                                    color = titleColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                // 페이지 수
                                Text("${g.filecount}p",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                )
                            }
                        }
                    }
                    if(viewModel.galleries.isEmpty())
                        Text("싫어요, 좋아요 갤러리가 없습니다", style = TextStyle(fontSize = 50.sp))
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Slider(
                    value = scrollState.value.toFloat(),
                    valueRange = 0f..scrollState.maxValue.toFloat(),
                    onValueChange = { v ->
                        coroutineScope.launch {
                            scrollState.scrollTo(v.toInt())
                        }
                    },
                    interactionSource = interactionSource,
                    colors = SliderDefaults.colors(
                        activeTrackColor  = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    thumb = {
                        Row(
                            modifier = Modifier.background(Color(0xFF9933FF), shape = RoundedCornerShape(10.dp))
                        ) {
                            if (visible) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    modifier = Modifier.width(25.dp)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.width(25.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .graphicsLayer{
                            rotationZ = 90f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth
                                )
                            )
                            layout(placeable.height, placeable.width) {
                                placeable.place(0, -placeable.height)
                            }
                        }
                        .fillMaxWidth()
                )
            }
        }
    }
}