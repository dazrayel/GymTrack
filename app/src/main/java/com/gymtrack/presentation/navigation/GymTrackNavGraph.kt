package com.gymtrack.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtrack.presentation.exercises.ExercisesScreen
import com.gymtrack.presentation.history.HistoryScreen
import com.gymtrack.presentation.home.HomeScreen
import com.gymtrack.presentation.settings.SettingsScreen
import com.gymtrack.presentation.workouts.WorkoutsScreen

private const val ROUTE_EXERCISES = "exercises"

@Composable
fun GymTrackNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = stringResource(destination.labelResId),
                            )
                        },
                        label = { Text(stringResource(destination.labelResId)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onNavigateToExercises = {
                        navController.navigate(ROUTE_EXERCISES) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(TopLevelDestination.WORKOUTS.route) {
                WorkoutsScreen(contentPadding = innerPadding)
            }
            composable(TopLevelDestination.HISTORY.route) {
                HistoryScreen(contentPadding = innerPadding)
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
            composable(ROUTE_EXERCISES) {
                ExercisesScreen(
                    outerPadding = innerPadding,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
