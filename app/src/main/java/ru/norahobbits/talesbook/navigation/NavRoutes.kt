package ru.norahobbits.talesbook.navigation

sealed class NavRoutes(val route: String) {
    object Library : NavRoutes("library")
    object Book : NavRoutes("book/{bookId}") {
        fun create(bookId: Long) = "book/$bookId"
    }
    object ChapterEditor : NavRoutes("chapter/{chapterId}") {
        fun create(chapterId: Long) = "chapter/$chapterId"
    }
    object AppearanceSettings : NavRoutes("appearance")
}
