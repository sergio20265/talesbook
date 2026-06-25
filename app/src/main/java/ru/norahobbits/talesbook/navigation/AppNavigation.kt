package ru.norahobbits.talesbook.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.ui.screens.appearance.AppearanceSettingsScreen
import ru.norahobbits.talesbook.ui.screens.book.BookScreen
import ru.norahobbits.talesbook.ui.screens.editor.ChapterEditorScreen
import ru.norahobbits.talesbook.ui.screens.library.LibraryScreen

@Composable
fun AppNavigation(appSettings: AppSettings) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.Library.route) {

        composable(NavRoutes.Library.route) {
            LibraryScreen(
                appSettings = appSettings,
                onOpenBook = { bookId ->
                    navController.navigate(NavRoutes.Book.create(bookId))
                },
                onOpenSettings = {
                    navController.navigate(NavRoutes.AppearanceSettings.route)
                }
            )
        }

        composable(
            route = NavRoutes.Book.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStack ->
            val bookId = backStack.arguments?.getLong("bookId") ?: return@composable
            BookScreen(
                bookId = bookId,
                appSettings = appSettings,
                onOpenChapter = { chapterId ->
                    navController.navigate(NavRoutes.ChapterEditor.create(chapterId))
                },
                onOpenSettings = {
                    navController.navigate(NavRoutes.AppearanceSettings.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ChapterEditor.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
        ) { backStack ->
            val chapterId = backStack.arguments?.getLong("chapterId") ?: return@composable
            ChapterEditorScreen(
                chapterId = chapterId,
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
                onOpenChapter = { nextChapterId ->
                    navController.navigate(NavRoutes.ChapterEditor.create(nextChapterId)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(NavRoutes.AppearanceSettings.route)
                }
            )
        }

        composable(NavRoutes.AppearanceSettings.route) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
