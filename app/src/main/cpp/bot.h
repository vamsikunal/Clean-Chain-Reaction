#ifndef BOT_H
#define BOT_H

#include <utility>
#include <vector>

// Forward-declare the main game class to avoid circular dependencies
class ChainReactionGame;

/**
 * The Bot "Contract" or Interface.
 */
class IBotStrategy {
public:
    virtual ~IBotStrategy() = default;
    virtual std::pair<int, int> findMove(const ChainReactionGame& gameState, int myPlayerId) = 0;
};


/**
 * A simple bot that finds all valid moves and picks one at random.
 */
class RandomBot : public IBotStrategy {
public:
    std::pair<int, int> findMove(const ChainReactionGame& gameState, int myPlayerId) override;
};

class GreedyBot : public IBotStrategy {
public:
    std::pair<int, int> findMove(const ChainReactionGame& gameState, int myPlayerId) override;
};

class MinimaxBot : public IBotStrategy {
public:
    std::pair<int, int> findMove(const ChainReactionGame& gameState, int myPlayerId) override;
private:
    int minimax(ChainReactionGame gameState, int depth, int alpha, int beta, bool isMaximizingPlayer, int myPlayerId);
};

#endif //BOT_H