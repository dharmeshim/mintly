package com.mintly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mintly.app.data.MintlyRepository
import com.mintly.app.data.local.AppDatabase
import com.mintly.app.ui.components.MintlyBottomNavigation
import com.mintly.app.ui.navigation.Screen
import com.mintly.app.ui.screens.AddExpenseScreen
import com.mintly.app.ui.screens.CalendarScreen
import com.mintly.app.ui.screens.CategoriesScreen
import com.mintly.app.ui.theme.MintlyTheme
import com.mintly.app.viewmodel.MintlyViewModel
import com.mintly.app.viewmodel.MintlyViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = MintlyRepository(database.expenseDao(), database.categoryDao())
        val factory = MintlyViewModelFactory(repository)

        setContent {
            MintlyTheme {
                val navController = rememberNavController()
                val viewModel: MintlyViewModel = viewModel(factory = factory)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        MintlyBottomNavigation(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.AddExpense.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.AddExpense.route) {
                            AddExpenseScreen(viewModel = viewModel)
                        }
                        composable(Screen.Calendar.route) {
                            CalendarScreen(viewModel = viewModel)
                        }
                        composable(Screen.Categories.route) {
                            CategoriesScreen(viewModel = viewModel)
                        }

                    }
                }
            }
        }
    }
}

