#include "game.h"
#include <queue>
#include <sstream>
#include <algorithm>
#include <android/log.h>

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "ChainReaction", __VA_ARGS__)

// --- ChainReactionGame Implementation ---
ChainReactionGame::ChainReactionGame(int pCount, int botType, int r, int c) : rows(r), cols(c) {
    this->playerCount = pCount;
    this->movesMade = 0;

    this->grid.assign(rows, std::vector<Cell>(cols, {-1, 0}));

    this->playerScores.clear();
    this->alive.clear();
    this->botStrategies.clear();

    if (playerCount > 0) {
        this->playerScores.resize(playerCount, 0);
        this->alive.resize(playerCount, 0);
    }

    aliveCount = 0;
    lastAlivePlayer = -1;

    lastAnimationEvents.clear();
    lastAnimationEvents.reserve(rows * cols);

    // Initialize bot if needed
    if (botType > 0 && playerCount > 1) {
        switch (botType) {
            case 1: botStrategies[1] = std::make_unique<RandomBot>(); break;
            case 2: botStrategies[1] = std::make_unique<GreedyBot>(); break;
            case 3: botStrategies[1] = std::make_unique<MinimaxBot>(); break;
        }
    }
}

ChainReactionGame::~ChainReactionGame() {}

ChainReactionGame::ChainReactionGame(const ChainReactionGame& other) {
    this->rows = other.rows;
    this->cols = other.cols;
    this->playerCount = other.playerCount;
    this->movesMade = other.movesMade;
    this->grid = other.grid;
    this->playerScores = other.playerScores;
    this->alive = other.alive;
    this->aliveCount = other.aliveCount;
    this->lastAlivePlayer = other.lastAlivePlayer;
    this->lastAnimationEvents.clear();
}

// --- Helpers for alive bookkeeping ---
inline void ChainReactionGame::adjustPlayerScore(int player, int delta) {
    if (player < 0 || player >= static_cast<int>(playerScores.size())) return;
    playerScores[player] += delta;
    if (playerScores[player] <= 0) markDeadIfNeeded(player);
    else markAliveIfNeeded(player);
}

void ChainReactionGame::markAliveIfNeeded(int player) {
    if (player < 0 || player >= static_cast<int>(alive.size())) return;
    if (!alive[player] && playerScores[player] > 0) {
        alive[player] = 1;
        ++aliveCount;
        lastAlivePlayer = player;
    }
}

void ChainReactionGame::markDeadIfNeeded(int player) {
    if (player < 0 || player >= static_cast<int>(alive.size())) return;
    if (alive[player] && playerScores[player] <= 0) {
        alive[player] = 0;
        --aliveCount;
        if (aliveCount == 1) {
            for (int p = 0; p < playerCount && p < static_cast<int>(alive.size()); ++p) {
                if (alive[p]) { lastAlivePlayer = p; break; }
            }
        } else if (aliveCount == 0) {
            lastAlivePlayer = -1;
        }
    }
}

// --- Grid Utilities ---
std::string ChainReactionGame::getGridState() {
    std::stringstream ss;
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            ss << grid[i][j].owner << "," << grid[i][j].orbs;
            if (j < cols - 1) ss << ";";
        }
        if (i < rows - 1) ss << "|";
    }
    return ss.str();
}

int ChainReactionGame::getPlayerScore(int player) const {
    if (player < 0 || player >= static_cast<int>(playerScores.size())) return 0;
    return playerScores[player];
}

int ChainReactionGame::getCellCapacity(int r, int c) const {
    if ((r == 0 || r == rows - 1) && (c == 0 || c == cols - 1)) return 1;
    if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) return 2;
    return 3;
}

// --- Move Validation ---
bool ChainReactionGame::isMoveValid(int r, int c, int player) const {
    if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
    const Cell& cell = grid[r][c];
    return cell.owner == -1 || cell.owner == player;
}

// --- Move Execution ---
bool ChainReactionGame::makeMove(int r, int c, int player) {
    if (!isMoveValid(r, c, player)) return false;

    lastAnimationEvents.clear();

    Cell& cell = grid[r][c];
    cell.owner = player;
    cell.orbs++;

    adjustPlayerScore(player, 1); // increment player score & update alive

    std::queue<std::pair<int, int>> unstableCells;
    if (cell.orbs > getCellCapacity(r, c)) unstableCells.emplace(r, c);

    if (!unstableCells.empty()) processChainReaction(unstableCells);

    movesMade++;
    return true;
}

// --- Clamp player scores (safety) ---
void ChainReactionGame::clampPlayerScores() {
    for (size_t i = 0; i < playerScores.size(); ++i) {
        if (playerScores[i] < 0) playerScores[i] = 0;
    }
    aliveCount = 0;
    lastAlivePlayer = -1;
    for (size_t i = 0; i < playerScores.size(); ++i) {
        if (playerScores[i] > 0) {
            alive[i] = 1;
            ++aliveCount;
            lastAlivePlayer = i;
        } else alive[i] = 0;
    }
}

// --- Chain Reaction Processing ---
void ChainReactionGame::processChainReaction(std::queue<std::pair<int, int>>& q) {
    const int dr[] = {-1, 1, 0, 0};
    const int dc[] = {0, 0, -1, 1};
    std::vector<char> inQueue(rows * cols, 0);

    std::queue<std::pair<int,int>> temp = q;
    while (!temp.empty()) { inQueue[flatIndex(temp.front().first, temp.front().second)] = 1; temp.pop(); }

    while (!q.empty()) {
        auto pos = q.front(); q.pop();
        int r = pos.first;
        int c = pos.second;
        inQueue[flatIndex(r,c)] = 0;

        if (grid[r][c].owner == -1 || grid[r][c].orbs <= getCellCapacity(r, c)) continue;

        Cell explodingCell = grid[r][c];
        int owner = explodingCell.owner;
        int orbsInCell = explodingCell.orbs;

        adjustPlayerScore(owner, -orbsInCell); // owner loses exploding orbs
        grid[r][c] = {-1, 0};

        for (int i = 0; i < 4; ++i) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;

            lastAnimationEvents.push_back({r, c, nr, nc, owner});

            Cell &neighbor = grid[nr][nc];
            int prevOwner = neighbor.owner;
            int prevOrbs  = neighbor.orbs;

            if (prevOwner == -1) {
                neighbor.owner = owner;
                neighbor.orbs = 1;
                adjustPlayerScore(owner, 1);
            } else if (prevOwner == owner) {
                neighbor.orbs += 1;
                adjustPlayerScore(owner, 1);
            } else {
                neighbor.owner = owner;
                neighbor.orbs = prevOrbs + 1;
                adjustPlayerScore(owner, prevOrbs + 1);
                adjustPlayerScore(prevOwner, -prevOrbs);
            }

            int idx = flatIndex(nr, nc);
            if (neighbor.orbs > getCellCapacity(nr, nc) && !inQueue[idx]) {
                q.emplace(nr, nc);
                inQueue[idx] = 1;
            }
        }
    }

    clampPlayerScores(); // final safety
}

// --- Winner & Eliminated ---
int ChainReactionGame::getWinner() {
    if (movesMade < playerCount) return -1;
    if (aliveCount == 1) return lastAlivePlayer;
    return -1;
}

const std::vector<OrbAnimationEvent> ChainReactionGame::getLastAnimationEvents() {
    return lastAnimationEvents;
}

bool ChainReactionGame::isPlayerEliminated(int player) {
    if (player < 0 || player >= static_cast<int>(playerScores.size())) return true;
    if (movesMade < playerCount) return false;
    return playerScores[player] <= 0;
}

// --- Bot Integration ---
bool ChainReactionGame::isPlayerBot(int player) const {
    return botStrategies.count(player) > 0;
}

std::pair<int, int> ChainReactionGame::getBotMove(int player) {
    if (isPlayerBot(player)) return botStrategies.at(player)->findMove(*this, player);
    return {-1, -1};
}
