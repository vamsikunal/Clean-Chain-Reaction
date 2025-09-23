package com.example.chainreaction

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun GameScreen(gameViewModel: GameViewModel = viewModel()) {
    val gridState by gameViewModel.gridState.collectAsState()
    val currentPlayer by gameViewModel.currentPlayer.collectAsState()
    val winner by gameViewModel.winner.collectAsState()
    val animationEvents by gameViewModel.animationEvents.collectAsState()
    val showGameSetupDialog by gameViewModel.showGameSetupDialog.collectAsState()

    val playerColors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .blur(radius = if (winner != -1) 8.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (winner == -1) "Player ${currentPlayer + 1}'s Turn" else "",
                fontSize = 24.sp,
                color = if (winner == -1) playerColors.getOrElse(currentPlayer) { Color.Black } else Color.Transparent
            )
            Spacer(Modifier.height(16.dp))

            GameBoard(
                gridState = gridState,
                playerColors = playerColors,
                animationEvents = animationEvents,
                onAnimationComplete = { gameViewModel.onAnimationComplete() },
                onCellClicked = { r, c ->
                    gameViewModel.onCellClicked(r, c)
                }
            )
        }

        if (winner != -1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Player ${winner + 1} Wins!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = playerColors.getOrElse(winner) { Color.Black }
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { gameViewModel.startGame() }) {
                            Text("Play Again")
                        }
                    }
                }
            }
        }

        if (showGameSetupDialog) {
            GameSetupDialog(
                onConfirmMultiplayer = { playerCount ->
                    gameViewModel.initializeMultiplayerGame(playerCount)
                },
                onConfirmBotGame = { botType ->
                    gameViewModel.initializeBotGame(botType)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupDialog(
    onConfirmMultiplayer: (playerCount: Int) -> Unit,
    onConfirmBotGame: (botType: BotType) -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.VERSUS_BOT) }
    var playerCountExpanded by remember { mutableStateOf(false) }
    val playerOptions = listOf(2, 3, 4, 5)
    var selectedPlayerCount by remember { mutableStateOf(playerOptions[0]) }
    var botTypeExpanded by remember { mutableStateOf(false) }
    val botOptions = BotType.values().toList()
    var selectedBotType by remember { mutableStateOf(botOptions[0]) }

    AlertDialog(
        onDismissRequest = { /* Force a selection */ },
        title = { Text(text = "New Game Setup") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            ExposedDropdownMenu(expanded = botTypeExpanded, onDismissRequest = { botTypeExpanded = false }) {
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
                            ExposedDropdownMenu(expanded = playerCountExpanded, onDismissRequest = { playerCountExpanded = false }) {
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
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedMode == GameMode.VERSUS_BOT) {
                    onConfirmBotGame(selectedBotType)
                } else {
                    onConfirmMultiplayer(selectedPlayerCount)
                }
            }) {
                Text("Start Game")
            }
        }
    )
}


@Composable
fun GameBoard(
    gridState: List<List<CellState>>,
    playerColors: List<Color>,
    animationEvents: List<OrbAnimationEvent>,
    onAnimationComplete: () -> Unit,
    onCellClicked: (row: Int, col: Int) -> Unit
) {
    if (gridState.isEmpty()) return

    val rows = gridState.size
    val cols = gridState[0].size

    val animatedOrbs = remember(animationEvents) {
        animationEvents.map { Animatable(0f) }
    }

    // --- ENHANCEMENT 1: Randomized Rotation Direction ---
    // This creates and remembers a random rotation direction (1f for CCW, -1f for CW)
    // for each cell. It only re-randomizes if the grid size changes.
    val rotationDirections = remember(rows, cols) {
        List(rows) {
            List(cols) { if (Math.random() > 0.5) 1f else -1f }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb_vibration_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_angle"
    )

    LaunchedEffect(animationEvents) {
        if (animationEvents.isNotEmpty()) {
            val jobs = animatedOrbs.map { animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                    )
                }
            }
            jobs.joinAll()
            onAnimationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellWidth = size.width / cols
                    val cellHeight = size.height / rows
                    val c = (offset.x / cellWidth).toInt()
                    val r = (offset.y / cellHeight).toInt()
                    if (r in 0 until rows && c in 0 until cols) {
                        onCellClicked(r, c)
                    }
                }
            }) {
            val cellWidth = size.width / cols
            val cellHeight = size.height / rows
            val orbRadius = cellWidth.coerceAtMost(cellHeight) / 4.5f

            // 1. Draw grid lines
            for (i in 0..rows) {
                drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, i * cellHeight), Offset(size.width, i * cellHeight), 2f)
            }
            for (i in 0..cols) {
                drawLine(Color.Gray.copy(alpha = 0.5f), Offset(i * cellWidth, 0f), Offset(i * cellWidth, size.height), 2f)
            }

            // 2. Draw static orbs
            val isAnimating = animationEvents.isNotEmpty()
            val explodingCells = if (isAnimating) animationEvents.map { it.fromRow to it.fromCol }.toSet() else emptySet()

            gridState.forEachIndexed { r, row ->
                row.forEachIndexed { c, cell ->
                    if (cell.owner != -1 && (r to c) !in explodingCells) {
                        // Pass the randomized direction for the current cell to the drawing function.
                        val direction = rotationDirections[r][c]
                        drawCellOrbs(
                            cell = cell,
                            playerColor = playerColors.getOrElse(cell.owner) { Color.Black },
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth, cellHeight),
                            orbRadius = orbRadius,
                            animationAngle = angle * direction // Apply direction
                        )
                    }
                }
            }

            // 3. Draw moving orbs for chain reaction
            if (isAnimating) {
                animationEvents.forEachIndexed { index, event ->
                    val progress = animatedOrbs[index].value
                    val start = Offset((event.fromCol + 0.5f) * cellWidth, (event.fromRow + 0.5f) * cellHeight)
                    val end = Offset((event.toCol + 0.5f) * cellWidth, (event.toRow + 0.5f) * cellHeight)
                    val currentPos = start + (end - start) * progress
                    val playerColor = playerColors.getOrElse(event.playerOwner) { Color.Black }
                    drawCircle(color = playerColor, radius = orbRadius, center = currentPos)
                }
            }
        }
    }
}

// ⭐ FIX 4: This function is now a private helper. Its signature is updated to take the
// `topLeft` offset so it knows where to draw inside the main Canvas.
private fun DrawScope.drawCellOrbs(
    cell: CellState,
    playerColor: Color,
    topLeft: Offset,
    size: Size,
    orbRadius: Float,
    animationAngle: Float
) {
    val vibrationRadius = orbRadius * 0.1f
    val angleRad = Math.toRadians(animationAngle.toDouble()).toFloat()
    val vibrationOffset = Offset(cos(angleRad) * vibrationRadius, sin(angleRad) * vibrationRadius)

    // The center is now calculated relative to the cell's top-left corner
    val center = Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2)

    when (cell.orbs) {
        1 -> {
            drawCircle(color = playerColor, radius = orbRadius, center = center + vibrationOffset)
        }
        2 -> {
            val separation = orbRadius * 0.8f
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x - separation) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x + separation) + vibrationOffset)
        }
        3 -> {
            val separation = orbRadius * 0.9f
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(y = center.y - separation) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x - separation * 0.866f, y = center.y + separation * 0.5f) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x + separation * 0.866f, y = center.y + separation * 0.5f) + vibrationOffset)
        }
        4 -> {
            val separation = orbRadius * 0.8f
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x - separation, y = center.y - separation) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x + separation, y = center.y - separation) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x - separation, y = center.y + separation) + vibrationOffset)
            drawCircle(color = playerColor, radius = orbRadius, center = center.copy(x = center.x + separation, y = center.y + separation) + vibrationOffset)
        }
    }
}