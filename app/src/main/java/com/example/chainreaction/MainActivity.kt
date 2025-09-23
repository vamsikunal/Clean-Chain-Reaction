package com.example.chainreaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chainreaction.ui.theme.ChainReactionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChainReactionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "start") {
                        composable("start") {
                            StartScreen(onStartGame = {
                                navController.navigate("game")
                            })
                        }
                        // The change is here: We get the NavBackStackEntry to scope the ViewModel
                        composable("game") { backStackEntry ->
                            // Explicitly create the ViewModel using the backStackEntry as the owner
                            val gameViewModel: GameViewModel = viewModel(
                                viewModelStoreOwner = backStackEntry
                            )
                            // Pass the created ViewModel to the GameScreen
                            GameScreen(gameViewModel = gameViewModel)
                        }
                    }
                }
            }
        }
    }
}

