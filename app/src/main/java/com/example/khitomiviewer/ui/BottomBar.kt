package com.example.khitomiviewer.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.khitomiviewer.Screen
import kotlinx.coroutines.launch

@Composable
fun BottomBar(navController: NavHostController, verticalScrollState: ScrollState) {
    val coroutineScope = rememberCoroutineScope()

    val navigationTabList = listOf(
        Screen.MyGallery,
        Screen.MyTag,
        Screen.Subscription,
        Screen.List,
        Screen.Rank,
        Screen.Menu
    )
    // 현재 내비게이션 상태를 관찰 (selectedItem 변수 대신 사용)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .height(50.dp)
    ) {
        navigationTabList.forEach { screen ->
            val isSelected = currentRoute?.startsWith(screen.route.split("/")[0]) == true

            NavigationBarItem(
                alwaysShowLabel = false,
                label = null,
                selected = isSelected,
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                onClick = {
                    val targetRoute =
                        when (screen) {
                            is Screen.MyGallery -> screen.createRoute()
                            is Screen.MyTag -> screen.createRoute()
                            is Screen.Subscription -> screen.createRoute()
                            is Screen.List -> screen.createRoute()
                            is Screen.Rank -> screen.createRoute()
                            else -> screen.route
                        }

                    if (!isSelected) {
                        navController.navigate(targetRoute)
                        coroutineScope.launch { verticalScrollState.scrollTo(0) }
                    }
                }
            )
        }
    }
}