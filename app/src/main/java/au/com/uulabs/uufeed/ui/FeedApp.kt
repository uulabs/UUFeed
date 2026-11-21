package au.com.uulabs.uufeed.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import au.com.uulabs.uufeed.ui.details.DetailsScreen
import au.com.uulabs.uufeed.ui.home.HomeScreen
import au.com.uulabs.uufeed.ui.results.ResultsScreen
import au.com.uulabs.uufeed.ui.settings.SettingsScreen
import au.com.uulabs.uufeed.ui.setup.SetupScreen

private object Routes {
    const val Setup = "setup"
    const val Home = "home"
    const val Settings = "settings"
    const val Results = "results"
    const val Place = "place/{placeId}"
    fun place(id: String) = "place/${Uri.encode(id)}"
}

@Composable
fun FeedApp(viewModel: FeedViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val start = remember { if (viewModel.hasPlacesKey()) Routes.Home else Routes.Setup }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FeedEvent.ShowResults -> navController.navigate(Routes.Results) {
                    popUpTo(Routes.Home)
                    launchSingleTop = true
                }
            }
        }
    }
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Setup) {
            SetupScreen(
                state = state,
                viewModel = viewModel,
                allowBack = state.hasPlacesKey,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Setup) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                state = state,
                viewModel = viewModel,
                onSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReplace = { navController.navigate(Routes.Setup) },
                onRetest = { viewModel.retestSavedKey() },
                onClear = {
                    viewModel.clearPlacesKey()
                    if (!viewModel.hasPlacesKey()) {
                        navController.navigate(Routes.Setup) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.Results) {
            ResultsScreen(
                state = state,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Routes.place(id)) },
            )
        }
        composable(
            route = Routes.Place,
            arguments = listOf(navArgument("placeId") { type = NavType.StringType }),
        ) { entry ->
            val id = Uri.decode(entry.arguments?.getString("placeId").orEmpty())
            DetailsScreen(
                placeId = id,
                listed = state.results.firstOrNull { it.id == id },
                detailsState = state.details,
                origin = state.origin,
                demo = state.demoEnabled,
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
    }
}
