package com.metalens.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.ComponentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metalens.app.R
import com.metalens.app.ui.components.MetaLensTopBar
import com.metalens.app.ui.screens.ConversationScreen
import com.metalens.app.ui.screens.HistoryScreen
import com.metalens.app.ui.screens.HomeScreen
import com.metalens.app.ui.screens.SettingsScreen
import com.metalens.app.wearables.WearablesViewModel

sealed class MetaLensRoute(
    val route: String,
    val titleResId: Int,
) {
    data object Home : MetaLensRoute("home", R.string.tab_home)
    data object History : MetaLensRoute("history", R.string.tab_history)
    data object Settings : MetaLensRoute("settings", R.string.tab_settings)
    data object Stream : MetaLensRoute("stream", R.string.stream_title)
    data object Conversation : MetaLensRoute("conversation", R.string.conversation_title)
}

private val bottomTabs = listOf(
    MetaLensRoute.Home,
    MetaLensRoute.History,
    MetaLensRoute.Settings,
)

@Composable
fun MetaLensApp() {
    val navController = rememberNavController()
    MetaLensScaffold(navController = navController)
}

@Composable
private fun MetaLensScaffold(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = bottomTabs.firstOrNull { it.route == currentRoute } ?: MetaLensRoute.Home
    val isFullScreenRoute =
        currentRoute == MetaLensRoute.Stream.route || currentRoute == MetaLensRoute.Conversation.route
    val canNavigateBack = navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
            if (!isFullScreenRoute) {
                MetaLensTopBar(title = stringResource(currentTab.titleResId))
            } else {
                MetaLensTopBar(
                    title =
                        if (currentRoute == MetaLensRoute.Conversation.route) {
                            stringResource(R.string.conversation_title)
                        } else {
                            stringResource(R.string.stream_title)
                        },
                    onBack = if (canNavigateBack) ({ navController.popBackStack() }) else null,
                )
            }
        },
        bottomBar = {
            if (!isFullScreenRoute) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = tab.route == currentRoute
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector =
                                        when (tab) {
                                            MetaLensRoute.Home -> Icons.Filled.Home
                                            MetaLensRoute.History -> Icons.Filled.History
                                            MetaLensRoute.Settings -> Icons.Filled.Settings
                                            MetaLensRoute.Stream -> Icons.Filled.Home
                                            MetaLensRoute.Conversation -> Icons.Filled.Home
                                        },
                                    contentDescription = stringResource(tab.titleResId),
                                )
                            },
                            label = { Text(stringResource(tab.titleResId), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        MetaLensNavHost(
            navController = navController,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun MetaLensNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MetaLensRoute.Home.route,
    ) {
        composable(MetaLensRoute.Home.route) {
            val activity = LocalContext.current as ComponentActivity
            val wearablesViewModel: WearablesViewModel = viewModel(activity)
            val wearablesUiState by wearablesViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                modifier = modifier,
                isGlassesConnected = wearablesUiState.hasActiveDevice,
                onStartConversation = { navController.navigate(MetaLensRoute.Conversation.route) },
                onStartStreaming = { navController.navigate(MetaLensRoute.Stream.route) },
            )
        }
        composable(MetaLensRoute.Settings.route) {
            SettingsScreen(
                modifier = modifier,
            )
        }
        composable(MetaLensRoute.History.route) {
            HistoryScreen(modifier = modifier)
        }
        composable(MetaLensRoute.Stream.route) {
            com.metalens.app.ui.screens.StreamScreen(
                modifier = modifier,
                onStop = { navController.popBackStack() },
            )
        }
        composable(MetaLensRoute.Conversation.route) {
            ConversationScreen(
                modifier = modifier,
                onStop = { navController.popBackStack() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetaLensAppPreview() {
    MetaLensApp()
}

