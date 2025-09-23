#include "bot.h"
#include "game.h"
#include <random>
#include <future>
#include <thread>


// Random Bot
std::pair<int, int> RandomBot::findMove(const ChainReactionGame& gameState, int myPlayerId) {
    std::vector<std::pair<int, int>> validMoves;
    for (int r = 0; r < gameState.getRows(); ++r) {
        for (int c = 0; c < gameState.getCols(); ++c) {
            if (gameState.isMoveValid(r, c, myPlayerId))
                validMoves.emplace_back(r, c);
        }
    }

    if (validMoves.empty()) return {-1, -1};

    static thread_local std::mt19937 gen([](){
        std::random_device rd;
        return std::mt19937(rd());
    }());

    std::uniform_int_distribution<int> distrib(0, static_cast<int>(validMoves.size()) - 1);
    return validMoves[distrib(gen)];
}


// Greedy Bot
std::pair<int, int> GreedyBot::findMove(const ChainReactionGame& gameState, int myPlayerId) {
    std::vector<std::pair<int, int>> validMoves;
    for (int r = 0; r < gameState.getRows(); ++r) {
        for (int c = 0; c < gameState.getCols(); ++c) {
            if (gameState.isMoveValid(r, c, myPlayerId)) {
                validMoves.emplace_back(r, c);
            }
        }
    }

    if (validMoves.empty()) {
        return {-1, -1}; // No moves available
    }

    std::pair<int, int> bestMove = validMoves[0]; // Default to the first valid move
    int maxScoreGain = std::numeric_limits<int>::min();
    int initialScore = gameState.getPlayerScore(myPlayerId);

    // Iterate through all valid moves to find the best one
    for (const auto& move : validMoves) {
        // 1. Create a "sandbox" copy of the game to simulate the move
        ChainReactionGame simulatedGame = gameState;

        // 2. Execute the move on the copy
        simulatedGame.makeMove(move.first, move.second, myPlayerId);

        // 3. Evaluate the outcome
        int finalScore = simulatedGame.getPlayerScore(myPlayerId);
        int scoreGain = finalScore - initialScore;

        // 4. If this move is better than the best one found so far, update it
        if (scoreGain > maxScoreGain) {
            maxScoreGain = scoreGain;
            bestMove = move;
        }
    }

    return bestMove;
}



// Minimax Bot
const int SEARCH_DEPTH = 3;
std::pair<int, int> MinimaxBot::findMove(const ChainReactionGame& gameState, int myPlayerId) {
    std::vector<std::pair<int, int>> validMoves;
    for (int r = 0; r < gameState.getRows(); ++r) {
        for (int c = 0; c < gameState.getCols(); ++c) {
            if (gameState.isMoveValid(r, c, myPlayerId)) {
                validMoves.emplace_back(r, c);
            }
        }
    }

    if (validMoves.empty()) return {-1, -1};
    if (validMoves.size() == 1) return validMoves[0];

    std::pair<int, int> bestMove = validMoves[0];
    int bestScore = std::numeric_limits<int>::min();

    // --- Multithreading Logic ---
    std::vector<std::future<int>> futures;

    // Launch an asynchronous task for each initial valid move
    futures.reserve(validMoves.size());
    for (const auto& move : validMoves) {
        futures.push_back(std::async(std::launch::async, [this, gameState, move, myPlayerId]() {
            ChainReactionGame simulatedGame = gameState;
            simulatedGame.makeMove(move.first, move.second, myPlayerId);
            return minimax(simulatedGame, SEARCH_DEPTH - 1,
                           std::numeric_limits<int>::min(), // Initial alpha
                           std::numeric_limits<int>::max(), // Initial beta
                           false, myPlayerId);
        }));
    }

    // Collect the results from all threads
    for (size_t i = 0; i < futures.size(); ++i) {
        int moveScore = futures[i].get(); // .get() waits for the thread to finish
        if (moveScore > bestScore) {
            bestScore = moveScore;
            bestMove = validMoves[i];
        }
    }

    return bestMove;
}

// Minimax with Alpha-Beta Pruning
int MinimaxBot::minimax(ChainReactionGame gameState, int depth, int alpha, int beta, bool isMaximizingPlayer, int myPlayerId) {
    // --- Base Case ---
    if (depth == 0 || gameState.getWinner() != -1) {
        int opponentId = 1 - myPlayerId;
        return gameState.getPlayerScore(myPlayerId) - gameState.getPlayerScore(opponentId);
    }

    // --- Recursive Step ---

    if (isMaximizingPlayer) { // The Bot's turn (MAXIMIZE score)
        int maxEval = std::numeric_limits<int>::min();
        std::vector<std::pair<int, int>> validMoves;
        // Find all of my valid moves...
        for (int r = 0; r < gameState.getRows(); ++r) for (int c = 0; c < gameState.getCols(); ++c)
                if (gameState.isMoveValid(r, c, myPlayerId)) validMoves.emplace_back(r, c);

        for (const auto& move : validMoves) {
            ChainReactionGame childState = gameState;
            childState.makeMove(move.first, move.second, myPlayerId);
            int eval = minimax(childState, depth - 1, alpha, beta, false, myPlayerId);
            maxEval = std::max(maxEval, eval);
            alpha = std::max(alpha, eval);
            // --- Pruning Step ---
            if (beta <= alpha) {
                break; // Beta cut-off
            }
        }
        return maxEval;

    } else { // The Opponent's turn (MINIMIZE score)
        int minEval = std::numeric_limits<int>::max();
        int opponentId = 1 - myPlayerId;
        std::vector<std::pair<int, int>> validMoves;
        // Find all of opponent's valid moves...
        for (int r = 0; r < gameState.getRows(); ++r) for (int c = 0; c < gameState.getCols(); ++c)
                if (gameState.isMoveValid(r, c, opponentId)) validMoves.emplace_back(r, c);

        if (validMoves.empty()) {
            return gameState.getPlayerScore(myPlayerId) - gameState.getPlayerScore(opponentId);
        }

        for (const auto& move : validMoves) {
            ChainReactionGame childState = gameState;
            childState.makeMove(move.first, move.second, opponentId);
            int eval = minimax(childState, depth - 1, alpha, beta, true, myPlayerId);
            minEval = std::min(minEval, eval);
            beta = std::min(beta, eval);
            // --- Pruning Step ---
            if (beta <= alpha) {
                break; // Alpha cut-off
            }
        }
        return minEval;
    }
}