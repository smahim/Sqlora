package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.preferences.EditorPreferences
import com.example.repository.DatabaseRepositoryImpl
import com.example.ui.screens.DatabaseDetailScreen
import com.example.ui.screens.DatabaseListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SqlConsoleScreen
import com.example.ui.screens.TableViewerScreen
import com.example.viewmodel.DatabaseDetailViewModel
import com.example.viewmodel.DatabaseListViewModel
import com.example.viewmodel.SqlConsoleViewModel
import com.example.viewmodel.TableViewerViewModel
import com.example.viewmodel.ViewModelFactory

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val repository = remember {
        val appDatabase = AppDatabase.getInstance(context)
        DatabaseRepositoryImpl(context, appDatabase)
    }
    val editorPreferences = remember {
        EditorPreferences.getInstance(context)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.DatabaseList.route,
        modifier = modifier
    ) {
        composable(Screen.DatabaseList.route) {
            val viewModel: DatabaseListViewModel = viewModel(
                factory = ViewModelFactory(repository)
            )
            DatabaseListScreen(
                viewModel = viewModel,
                onDatabaseClick = { dbId ->
                    navController.navigate(Screen.DatabaseDetail.createRoute(dbId))
                },
                onOpenConsole = { dbId ->
                    navController.navigate(Screen.SqlConsole.createRoute(dbId))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.DatabaseDetail.route,
            arguments = listOf(
                navArgument("databaseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val databaseId = backStackEntry.arguments?.getLong("databaseId") ?: 0L
            val viewModel = remember(databaseId) {
                DatabaseDetailViewModel(databaseId, repository)
            }
            DatabaseDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onTableClick = { tableName ->
                    navController.navigate(Screen.TableViewer.createRoute(databaseId, tableName))
                },
                onOpenConsole = { dbId ->
                    navController.navigate(Screen.SqlConsole.createRoute(dbId))
                }
            )
        }

        composable(
            route = Screen.TableViewer.route,
            arguments = listOf(
                navArgument("databaseId") { type = NavType.LongType },
                navArgument("tableName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val databaseId = backStackEntry.arguments?.getLong("databaseId") ?: 0L
            val tableName = backStackEntry.arguments?.getString("tableName") ?: ""
            val viewModel = remember(databaseId, tableName) {
                TableViewerViewModel(databaseId, tableName, repository)
            }
            TableViewerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onOpenConsoleWithSql = { sql ->
                    navController.navigate(Screen.SqlConsole.createRoute(databaseId))
                }
            )
        }

        composable(
            route = Screen.SqlConsole.route,
            arguments = listOf(
                navArgument("databaseId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val rawDbId = backStackEntry.arguments?.getLong("databaseId") ?: -1L
            val initialDbId = if (rawDbId > 0) rawDbId else null
            val viewModel = remember(initialDbId) {
                SqlConsoleViewModel(initialDbId, repository)
            }
            SqlConsoleScreen(
                viewModel = viewModel,
                editorPreferences = editorPreferences,
                onBackClick = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                editorPreferences = editorPreferences,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
