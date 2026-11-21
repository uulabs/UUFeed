package com.lunchy.ui

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
import com.lunchy.ui.details.DetailsScreen
import com.lunchy.ui.home.HomeScreen
import com.lunchy.ui.results.ResultsScreen

private object Routes {
    const val Home = "home"
    const val Results = "results"
    const val Place = "place/{placeId}"
    fun place(id: String) = "place/${Uri.encode(id)}"
}

@Composable
fun LunchApp(viewModel: LunchViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LunchEvent.ShowResults -> navController.navigate(Routes.Results) {
                    popUpTo(Routes.Home)
                    launchSingleTop = true
                }
            }
        }
    }
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(state = state, viewModel = viewModel)
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
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
    }
}
