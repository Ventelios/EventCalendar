package com.eventcalendar.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eventcalendar.app.ui.screens.calendar.CalendarScreen
import com.eventcalendar.app.ui.screens.datamanagement.DataManagementScreen
import com.eventcalendar.app.ui.screens.eventtypes.EventTypesScreen
import com.eventcalendar.app.ui.screens.history.HistoryScreen
import com.eventcalendar.app.ui.screens.menu.AboutAuthorScreen
import com.eventcalendar.app.ui.screens.menu.MenuScreen
import com.eventcalendar.app.ui.screens.menu.UpdateCheckScreen
import com.eventcalendar.app.ui.screens.statistics.StatisticsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Calendar : Screen("calendar", "日历", Icons.Rounded.CalendarMonth)
    data object EventTypes : Screen("event_types", "事件", Icons.Rounded.Category)
    data object History : Screen("history", "历史", Icons.Rounded.History)
    data object Statistics : Screen("statistics", "统计", Icons.Rounded.BarChart)
}

private val bottomNavScreens = listOf(
    Screen.Calendar,
    Screen.EventTypes,
    Screen.History,
    Screen.Statistics
)

@Composable
fun EventCalendarNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavScreens.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) {
                CalendarScreen(navController = navController)
            }
            composable(Screen.EventTypes.route) {
                EventTypesScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable("menu") {
                MenuScreen(navController = navController)
            }
            composable("about_author") {
                AboutAuthorScreen(navController = navController)
            }
            composable("update_check") {
                UpdateCheckScreen(navController = navController)
            }
            composable("data_management") {
                DataManagementScreen(navController = navController)
            }
        }
    }
}
