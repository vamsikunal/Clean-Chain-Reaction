package com.example.chainreaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Represents the state of a single cell, parsed from C++
data class CellState(val owner: Int, val orbs: Int)

enum class GameMode {
    MULTIPLAYER,
    VERSUS_BOT
}

enum class BotType(val id: Int, val displayName: String) {
    RANDOM(1, "Level 1: Random Bot"),
    GREEDY(2, "Level 2: Greedy Bot"),
    MINIMAX(3, "Level 3: Minimax Bot")
}

class GameViewModel : ViewModel() {

    // (JNI declarations and other properties remain the same)
    companion object {
        init {
            System.loadLibrary("chainreaction")
        }
    }
    // JNI function declarations
    private external fun nativeInitGame(playerCount: Int, botType: Int, rows: Int, cols: Int)
    private external fun nativeMakeMove(r: Int, c: Int, player: Int): Boolean
    private external fun nativeGetLastAnimationEvents(): List<OrbAnimationEvent>
    private external fun nativeGetGridState(): String
    private external fun nativeGetWinner(): Int
    private external fun nativeIsPlayerEliminated(player: Int): Boolean
    private external fun nativeDestroyGame()
    private external fun nativeIsPlayerBot(player: Int): Boolean
    private external fun nativeGetBotMove(player: Int): IntArray

    // StateFlows for UI observation
    private val _gridState = MutableStateFlow<List<List<CellState>>>(emptyList())
    val gridState = _gridState.asStateFlow()

    private val _animationEvents = MutableStateFlow<List<OrbAnimationEvent>>(emptyList())
    val animationEvents = _animationEvents.asStateFlow()

    private val _currentPlayer = MutableStateFlow(0)
    val currentPlayer = _currentPlayer.asStateFlow()

    private val _winner = MutableStateFlow(-1)
    val winner = _winner.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    var playerCount = 2
        private set
    private val rows = 12
    private val cols = 6
    private var isInitialized = false

    fun initializeGame(playerCount: Int, botType: BotType?) {
        if (isInitialized) return // Prevent re-initialization

        this.playerCount = playerCount
        val botTypeId = botType?.id ?: 0 // Use 0 for multiplayer

        viewModelScope.launch(Dispatchers.Default) {
            nativeInitGame(playerCount, botTypeId, rows, cols)
            withContext(Dispatchers.Main) {
                updateGridState()
            }
        }
        isInitialized = true
    }


    fun onCellClicked(r: Int, c: Int) {
        Log.d("ViewModel_Click", "Input received for cell ($r, $c)")
        if (_winner.value != -1 || _isAnimating.value) {
            Log.d("ViewModel_Click", "Input blocked: Game is busy.")
            return
        }
        processMove(r, c, _currentPlayer.value)
    }

    private fun processMove(r: Int, c: Int, player: Int) {
        viewModelScope.launch {
            val moveMade = withContext(Dispatchers.Default) {
                nativeMakeMove(r, c, player)
            }

            if (moveMade) {
                Log.d("ViewModel_Logic", "Move ($r, $c) for player ${player + 1} was valid.")
                val events = nativeGetLastAnimationEvents()
                if (events.isNotEmpty()) {
                    _isAnimating.value = true
                    _animationEvents.value = events
                } else {
                    proceedToNextTurn()
                }
            } else {
                Log.e("ViewModel_Logic", "Move ($r, $c) for player ${player + 1} was invalid.")
            }
        }
    }

    internal fun onAnimationComplete() {
        _animationEvents.value = emptyList()
        proceedToNextTurn()
    }

    private fun proceedToNextTurn() {
        viewModelScope.launch {
            updateGridState()
            val gameWinner = nativeGetWinner()

            if (gameWinner != -1) {
                _winner.value = gameWinner
            } else {
                var nextPlayer = _currentPlayer.value
                repeat(playerCount - 1) { // Corrected loop
                    nextPlayer = (nextPlayer + 1) % playerCount
                    if (!nativeIsPlayerEliminated(nextPlayer)) {
                        _currentPlayer.value = nextPlayer
                        return@repeat
                    }
                }
            }

            if (_winner.value != -1) {
                _isAnimating.value = false
                return@launch
            }

            if (nativeIsPlayerBot(_currentPlayer.value)) {
                Log.d("ViewModel_Bot", "Player ${_currentPlayer.value + 1} is a bot. Thinking...")

                val botMove = withContext(Dispatchers.Default) {
                    nativeGetBotMove(_currentPlayer.value)
                }

                processMove(botMove[0], botMove[1], _currentPlayer.value)
            } else {
                _isAnimating.value = false
                Log.d("ViewModel_Turn", "Input re-enabled for human player ${_currentPlayer.value + 1}.")
            }
        }
    }

    private fun updateGridState() {
        val stateString = nativeGetGridState()
        val parsedGrid = parseGridState(stateString)
        _gridState.value = parsedGrid
    }

    private fun parseGridState(state: String): List<List<CellState>> {
        if (state.isBlank()) return emptyList()
        return state.split("|").filter { it.isNotEmpty() }.map { rowStr ->
            rowStr.split(";").filter { it.isNotEmpty() }.map { cellStr ->
                val parts = cellStr.split(",").mapNotNull { it.toIntOrNull() }
                if (parts.size == 2) {
                    CellState(owner = parts[0], orbs = parts[1])
                } else {
                    CellState(owner = -1, orbs = 0)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nativeDestroyGame()
    }
}