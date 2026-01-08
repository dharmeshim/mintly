package com.mintly.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mintly.app.ui.navigation.navItems

@Composable
fun MintlyBottomNavigation(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        navItems.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    // Modern "no icon" approach
                },
                label = {
                    Text(
                        text = ":: ${screen.label.uppercase()}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = if (currentRoute == screen.route) FontWeight.Black else FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = if (currentRoute == screen.route) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

