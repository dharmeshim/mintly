package com.mintly.app.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object AddExpense : Screen("add_expense", "Log")
    object Calendar : Screen("timeline", "Timeline")
    object Categories : Screen("labels", "Labels")
}


val navItems = listOf(
    Screen.AddExpense,
    Screen.Calendar,
    Screen.Categories
)
