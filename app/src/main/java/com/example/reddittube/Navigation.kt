package com.lean.reddittube

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.lean.reddittube.RdTubeApp
import com.lean.reddittube.ui.main.HomeScreen
import com.lean.reddittube.ui.main.MainScreenViewModel
import com.lean.reddittube.ui.main.PlayerScreen
import kotlinx.serialization.Serializable

@Serializable
private data object Home : NavKey

@Serializable
private data object Player : NavKey

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)
    val app = LocalContext.current.applicationContext as RdTubeApp
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(app.container.repository) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Home> {
                    HomeScreen(
                        viewModel = viewModel,
                        onItemClick = { backStack.add(Player) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                entry<Player> {
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
    )
}
