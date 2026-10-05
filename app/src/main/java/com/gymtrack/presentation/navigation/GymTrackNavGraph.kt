package com.gymtrack.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymtrack.presentation.achievements.AchievementScreen
import com.gymtrack.presentation.calendar.CalendarScreen
import com.gymtrack.presentation.components.GymNavigationBar
import com.gymtrack.presentation.dashboard.DashboardScreen
import com.gymtrack.presentation.exercises.ExercisesScreen
import com.gymtrack.presentation.history.HistoryScreen
import com.gymtrack.presentation.home.HomeScreen
import com.gymtrack.presentation.planning.WeeklyPlanningScreen
import com.gymtrack.presentation.settings.SettingsScreen
import com.gymtrack.presentation.stats.ExerciseStatsScreen
import com.gymtrack.presentation.workouts.WorkoutsScreen
import com.gymtrack.presentation.workouts.detail.WorkoutDetailScreen
import com.gymtrack.presentation.workouts.execution.WorkoutExecutionScreen
import com.gymtrack.presentation.workouts.summary.WorkoutSessionSummaryScreen

internal const val ROUTE_ACHIEVEMENTS = "achievements"
internal const val ROUTE_CALENDAR = "calendar"
internal const val ROUTE_WEEKLY_PLANNING = "weekly_planning"

private const val ROUTE_EXERCISES = "exercises"
private const val ROUTE_WORKOUT_DETAIL = "workout_detail/{workoutId}"
private const val ROUTE_WORKOUT_EXECUTION = "workout_execution/{sessionId}"
private const val ROUTE_WORKOUT_SESSION_SUMMARY = "workout_session_summary/{sessionId}"
private const val ROUTE_EXERCISE_STATS = "exercise_stats/{exerciseName}"
private fun routeWorkoutDetail(workoutId: Long) = "workout_detail/$workoutId"
private fun routeWorkoutExecution(sessionId: Long) = "workout_execution/$sessionId"
private fun routeWorkoutSessionSummary(sessionId: Long) = "workout_session_summary/$sessionId"
private fun routeExerciseStats(exerciseName: String) =
    "exercise_stats/${Uri.encode(exerciseName)}"

@Composable
fun GymTrackNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            GymNavigationBar(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    if (destination == TopLevelDestination.HOME) {
                        navController.popBackStack(
                            TopLevelDestination.HOME.route,
                            inclusive = false,
                        )
                    } else {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
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
                    onNavigateToWorkouts = {
                        navController.navigate(TopLevelDestination.WORKOUTS.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(routeWorkoutDetail(workoutId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAchievements = {
                        navController.navigate(ROUTE_ACHIEVEMENTS) {
                            launchSingleTop = true
                        }
                    },
                    onSessionClick = { sessionId ->
                        navController.navigate(routeWorkoutSessionSummary(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                    onRecordClick = { exerciseName ->
                        navController.navigate(routeExerciseStats(exerciseName)) {
                            launchSingleTop = true
                        }
                    },
                    onContinueInProgress = { sessionId ->
                        navController.navigate(routeWorkoutExecution(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(TopLevelDestination.WORKOUTS.route) {
                WorkoutsScreen(
                    onNavigateToDetail = { workoutId ->
                        navController.navigate(routeWorkoutDetail(workoutId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToWeeklyPlanning = {
                        navController.navigate(ROUTE_WEEKLY_PLANNING) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(TopLevelDestination.DASHBOARD.route) {
                DashboardScreen(
                    onNavigateToWorkouts = {
                        navController.navigate(TopLevelDestination.WORKOUTS.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(routeWorkoutDetail(workoutId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAchievements = {
                        navController.navigate(ROUTE_ACHIEVEMENTS) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(TopLevelDestination.HISTORY.route) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onSessionClick = { sessionId ->
                        navController.navigate(routeWorkoutSessionSummary(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCalendar = {
                        navController.navigate(ROUTE_CALENDAR) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(TopLevelDestination.SETTINGS.route) {
                SettingsScreen(contentPadding = innerPadding)
            }
            composable(ROUTE_EXERCISES) {
                ExercisesScreen(
                    outerPadding = innerPadding,
                    onNavigateBack = { navController.popBackStack() },
                    onExerciseStatsClick = { exerciseName ->
                        navController.navigate(routeExerciseStats(exerciseName)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(ROUTE_ACHIEVEMENTS) {
                AchievementScreen(
                    onNavigateBack = { navController.popBackStack() },
                    contentPadding = innerPadding,
                )
            }
            composable(ROUTE_CALENDAR) {
                CalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSessionClick = { sessionId ->
                        navController.navigate(routeWorkoutSessionSummary(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(ROUTE_WEEKLY_PLANNING) {
                WeeklyPlanningScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWorkouts = {
                        navController.navigate(TopLevelDestination.WORKOUTS.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(
                route = ROUTE_WORKOUT_DETAIL,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
            ) {
                WorkoutDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExecution = { sessionId ->
                        navController.navigate(routeWorkoutExecution(sessionId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToExercises = {
                        navController.navigate(ROUTE_EXERCISES) {
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(
                route = ROUTE_WORKOUT_EXECUTION,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) {
                WorkoutExecutionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSummary = { sessionId ->
                        navController.navigate(routeWorkoutSessionSummary(sessionId)) {
                            popUpTo(ROUTE_WORKOUT_EXECUTION) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    contentPadding = innerPadding,
                )
            }
            composable(
                route = ROUTE_WORKOUT_SESSION_SUMMARY,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) {
                WorkoutSessionSummaryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    contentPadding = innerPadding,
                )
            }
            composable(
                route = ROUTE_EXERCISE_STATS,
                arguments = listOf(navArgument("exerciseName") { type = NavType.StringType }),
            ) {
                ExerciseStatsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    contentPadding = innerPadding,
                )
            }
        }
    }
}
