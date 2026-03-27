package com.example.khitomiviewer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector = Icons.Filled.Home
) {
    data object List :
        Screen(
            "ListScreen?page={page}&tagIdList={tagIdList}&titleKeyword={titleKeyword}&gId={gId}",
            "목록",
            Icons.Outlined.Home
        ) {
        fun createRoute(
            page: Long = 1L,
            tagIdList: LongArray? = null,
            titleKeyword: String? = "",
            gId: Long? = null
        ): String {
            val queryBuilder = StringBuilder("ListScreen?page=$page")
            tagIdList?.forEach { id -> queryBuilder.append("&tagIdList=$id") }
            if (!titleKeyword.isNullOrBlank())
                queryBuilder.append("&titleKeyword=${URLEncoder.encode(titleKeyword, "utf-8")}")
            if (gId != null)
                queryBuilder.append("&gId=$gId")
            return queryBuilder.toString()
        }
    }

    data object MyGallery :
        Screen("MyGalleryScreen?page={page}", "좋아요한 갤러리들", Icons.Outlined.Collections) {
        fun createRoute(page: Long = 1L): String {
            return "MyGalleryScreen?page=${page}"
        }
    }

    data object DislikeGallery :
        Screen("DislikeGalleryScreen?page={page}", "싫어요한 갤러리들", Icons.Outlined.Collections) {
        fun createRoute(page: Long = 1L): String {
            return "DislikeGalleryScreen?page=${page}"
        }
    }

    data object MyTag :
        Screen("MyTagScreen?page={page}", "좋아요한 태그들", Icons.Outlined.Sell) {
        fun createRoute(page: Long = 1L): String {
            return "MyTagScreen?page=${page}"
        }
    }

    data object DislikeTag :
        Screen("DislikeTagScreen?page={page}", "싫어요한 태그들", Icons.Outlined.Sell) {
        fun createRoute(page: Long = 1L): String {
            return "DislikeTagScreen?page=${page}"
        }
    }

    data object Subscription :
        Screen(
            "SubscriptionScreen?page={page}",
            "좋아요 태그가 하나라도 있는 갤러리들",
            Icons.Outlined.Subscriptions
        ) {
        fun createRoute(page: Long = 1L): String {
            return "SubscriptionScreen?page=${page}"
        }
    }

    data object Rank :
        Screen("RankScreen?page={page}&period={period}", "랭킹", Icons.Outlined.Leaderboard) {
        fun createRoute(page: Long = 1L, period: String = "week"): String {
            return "RankScreen?page=${page}&period=$period"
        }
    }

    data object ViewManga :
        Screen("ViewMangaScreen/{gidStr}", "만화보기", Icons.Outlined.PictureInPicture) {
        fun createRoute(gid: String): String = "ViewMangaScreen/$gid"
    }

    // 인자가 없는 화면
    data object Menu : Screen("MenuScreen", "메뉴", Icons.Outlined.Menu)
    data object Setting : Screen("SettingScreen", "세팅", Icons.Outlined.Settings)
    data object Crawling : Screen("CrawlingScreen", "크롤링", Icons.Outlined.SmartToy)
    data object Help : Screen("HelpScreen", "도움말", Icons.AutoMirrored.Outlined.HelpOutline)
    data object DatabaseExportImport :
        Screen(
            "DatabaseExportImportSettingScreen",
            "db 내보내기/가져오기",
            Icons.Outlined.ImportExport
        )
}