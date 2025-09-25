package com.example.chainreaction

import android.util.Log
import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val gridState by gameViewModel.gridState.collectAsState()
    val currentPlayer by gameViewModel.currentPlayer.collectAsState()
    val winner by gameViewModel.winner.collectAsState()
    val animationEvents by gameViewModel.animationEvents.collectAsState()
    val playerColors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta)

    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // 1. BackHandler to show the exit dialog when the game is in progress
    BackHandler(enabled = winner == -1) {
        showExitConfirmDialog = true
    }

    // New exit confirmation dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Exit Game?") },
            text = { Text("Are you sure you want to quit the current game?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateBack() // Call the lambda to navigate back
                    }
                ) {
                    Text("Yes, Exit")
                }
            },
            dismissButton = {
                Button(onClick = { showExitConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main game UI
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

        // Winner dialog overlay
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
                        // 2. "Play Again" now correctly navigates back to the setup screen
                        Button(onClick = onNavigateBack) {
                            Text("Play Again")
                        }
                    }
                }
            }
        }
    }
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