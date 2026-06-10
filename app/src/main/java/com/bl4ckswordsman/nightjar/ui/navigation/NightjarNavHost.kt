package com.bl4ckswordsman.nightjar.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bl4ckswordsman.nightjar.ui.screen.HomeScreen
import com.bl4ckswordsman.nightjar.ui.screen.SettingsScreen
import com.bl4ckswordsman.nightjar.viewmodel.TimerViewModel

object NightjarRoutes {
    const val HOME     = "home"
    const val SETTINGS = "settings"
}

@Composable
fun NightjarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NightjarRoutes.HOME,
) {
    val timerViewModel: TimerViewModel = hiltViewModel()

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier,
        enterTransition  = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessMedium,
                )
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessMedium,
                )
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessMedium,
                )
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness    = Spring.StiffnessMedium,
                )
            ) + fadeOut()
        },
    ) {
        composable(NightjarRoutes.HOME) {
            HomeScreen(
                viewModel = timerViewModel,
                onNavigateToSettings = { navController.navigate(NightjarRoutes.SETTINGS) },
            )
        }
        composable(NightjarRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
