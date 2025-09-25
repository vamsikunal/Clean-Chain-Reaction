package com.example.chainreaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chainreaction.ui.theme.ChainReactionTheme

// MainActivity.kt

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
                    // The navigation starts at the "start" screen
                    NavHost(navController = navController, startDestination = "start") {

                        // Route for the setup/start screen
                        composable("start") {
                            StartScreen(
                                onStartGame = { playerCount, botType ->
                                    // Use 0 if there's no bot (multiplayer)
                                    val botTypeId = botType?.id ?: 0
                                    // Navigate to the game screen, passing settings as arguments
                                    navController.navigate("game/$playerCount/$botTypeId")
                                }
                            )
                        }

                        // Route for the main game screen, which now accepts arguments
                        composable(
                            route = "game/{playerCount}/{botTypeId}",
                            arguments = listOf(
                                navArgument("playerCount") { type = NavType.IntType },
                                navArgument("botTypeId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            // Retrieve the arguments passed from the start screen
                            val playerCount = backStackEntry.arguments?.getInt("playerCount") ?: 2
                            val botTypeId = backStackEntry.arguments?.getInt("botTypeId") ?: 0
                            val botType = BotType.entries.find { it.id == botTypeId }

                            val gameViewModel: GameViewModel = viewModel()

                            LaunchedEffect(Unit) {
                                gameViewModel.initializeGame(playerCount, botType)
                            }

                            GameScreen(
                                gameViewModel = gameViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}