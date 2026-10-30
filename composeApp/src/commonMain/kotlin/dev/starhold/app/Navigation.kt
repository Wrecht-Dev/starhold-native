package dev.starhold.app

import androidx.compose.runtime.mutableStateListOf

sealed interface Screen {
    data object Home : Screen
    data class Library(val id: String) : Screen
    data class Detail(val id: String) : Screen
    data class Watch(val id: String, val live: Boolean = false, val liveTitle: String? = null) : Screen
    data class Read(val id: String) : Screen
    data class Search(val query: String) : Screen
    data object Genres : Screen
    data object History : Screen
    data object LiveTv : Screen
    data object Settings : Screen
    data object Landing : Screen
    data object Login : Screen
    data class Signup(val token: String = "", val code: String = "") : Screen
}

/** A simple back stack; native clients do not need URL semantics. */
class Navigator(start: Screen) {
    private val stack = mutableStateListOf(start)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1
    val depth: Int get() = stack.size

    fun push(screen: Screen) {
        if (stack.lastOrNull() == screen) return
        stack.add(screen)
    }

    fun replace(screen: Screen) {
        if (stack.isEmpty()) stack.add(screen) else stack[stack.lastIndex] = screen
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun reset(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }
}
