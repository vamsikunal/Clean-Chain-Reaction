#ifndef GAME_H
#define GAME_H

#include <vector>
#include <queue>
#include <string>
#include <utility>
#include <map>
#include <memory>
#include "bot.h"

// Represents a single cell on the grid
struct Cell {
    int owner;
    int orbs;
};

// Represents an orb moving from one cell to another for animation
struct OrbAnimationEvent {
    int fromRow, fromCol;
    int toRow, toCol;
    int playerOwner;
};

// The main game engine class.
class ChainReactionGame {
public:
    // --- Constructor & Destructor ---
    ChainReactionGame(int pCount, int botType, int r, int c);
    ~ChainReactionGame();

    // --- Core Lifecycle Methods ---
    ChainReactionGame(const ChainReactionGame& other);
//    void initialize(int pCount, int botType);

    // --- Gameplay Methods ---
    bool makeMove(int r, int c, int player);
    int getWinner();
    int getPlayerScore(int player) const;
    bool isPlayerEliminated(int player);

    // --- State & Info Getters ---
    std::string getGridState();
    const std::vector<OrbAnimationEvent> getLastAnimationEvents();
    bool isMoveValid(int r, int c, int player) const;
    bool isPlayerBot(int player) const;
    std::pair<int, int> getBotMove(int player);


    // --- Public Getters for Bot Simulation ---
    int getRows() const { return rows; }
    int getCols() const { return cols; }
    const std::vector<std::vector<Cell>>& getGrid() const { return grid; }

private:
    // --- Private Helper Methods ---
    int getCellCapacity(int r, int c) const;
    void processChainReaction(std::queue<std::pair<int, int>>& q);
    void clampPlayerScores();

    // bookkeeping for fast winner check
    std::vector<char> alive;
    int aliveCount = 0;
    int lastAlivePlayer = -1;
    void markAliveIfNeeded(int player);
    void markDeadIfNeeded(int player);
    inline int flatIndex(int r, int c) const { return r * cols + c; }
    inline void adjustPlayerScore(int player, int delta);

    // --- Game State Members ---
    int rows;
    int cols;
    int playerCount;
    int movesMade;
    std::vector<std::vector<Cell>> grid;
    std::vector<int> playerScores;
//    std::unordered_map<int, int> playerScores;
    std::vector<OrbAnimationEvent> lastAnimationEvents;
    std::map<int, std::unique_ptr<IBotStrategy>> botStrategies;
};

#endif