package com.example.chainreaction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onStartGame: (playerCount: Int, botType: BotType?) -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.VERSUS_BOT) }
    var playerCountExpanded by remember { mutableStateOf(false) }
    val playerOptions = listOf(2, 3, 4, 5)
    var selectedPlayerCount by remember { mutableIntStateOf(playerOptions[0]) }
    var botTypeExpanded by remember { mutableStateOf(false) }
    val botOptions = BotType.entries
    var selectedBotType by remember { mutableStateOf(botOptions[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Game Setup", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        Text("Select Game Mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedMode == GameMode.VERSUS_BOT,
                onClick = { selectedMode = GameMode.VERSUS_BOT },
                label = { Text("Versus Bot") }
            )
            FilterChip(
                selected = selectedMode == GameMode.MULTIPLAYER,
                onClick = { selectedMode = GameMode.MULTIPLAYER },
                label = { Text("Multiplayer") }
            )
        }
        Spacer(Modifier.height(16.dp))

        when (selectedMode) {
            // FIX: Removed the extra nested "GameMode.VERSUS_BOT" case
            GameMode.VERSUS_BOT -> {
                Text("Select Bot Difficulty", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = botTypeExpanded,
                    onExpandedChange = { botTypeExpanded = !botTypeExpanded }
                ) {
                    TextField(
                        value = selectedBotType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = botTypeExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = botTypeExpanded,
                        onDismissRequest = { botTypeExpanded = false }
                    ) {
                        botOptions.forEach { bot ->
                            DropdownMenuItem(
                                text = { Text(bot.displayName) },
                                onClick = {
                                    selectedBotType = bot
                                    botTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            GameMode.MULTIPLAYER -> {
                Text("Select Number of Players", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = playerCountExpanded,
                    onExpandedChange = { playerCountExpanded = !playerCountExpanded }
                ) {
                    TextField(
                        value = "$selectedPlayerCount Players",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = playerCountExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = playerCountExpanded,
                        onDismissRequest = { playerCountExpanded = false }
                    ) {
                        playerOptions.forEach { count ->
                            DropdownMenuItem(
                                text = { Text("$count Players") },
                                onClick = {
                                    selectedPlayerCount = count
                                    playerCountExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = {
            if (selectedMode == GameMode.VERSUS_BOT) {
                onStartGame(2, selectedBotType)
            } else {
                onStartGame(selectedPlayerCount, null)
            }
        }) {
            Text("Start Game")
        }
    }
}