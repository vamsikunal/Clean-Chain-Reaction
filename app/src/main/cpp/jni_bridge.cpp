#include <jni.h>
#include <string>
#include "game.h"

// Pointer to the single game instance
static ChainReactionGame* game = nullptr;

extern "C" {

/**
 * Initializes a new game. Destroys the old one if it exists.
 * This now uses the new constructor and initialize methods.
 */
JNIEXPORT void JNICALL
Java_com_example_chainreaction_GameViewModel_nativeInitGame(
        JNIEnv *env, jobject thiz, jint player_count, jint bot_type, jint rows, jint cols
) {
    if (game != nullptr) {
        delete game;
    }
    // Create and initialize the game in a single, atomic step
    game = new ChainReactionGame(player_count, bot_type, rows, cols);
}

/**
 * Executes a move for a given player at a specific cell.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_chainreaction_GameViewModel_nativeMakeMove(JNIEnv *env, jobject thiz, jint r, jint c, jint player) {
    if (game == nullptr) return false;
    return game->makeMove(r, c, player);
}

/**
 * Returns a string representation of the grid for the UI to parse.
 */
JNIEXPORT jstring JNICALL
Java_com_example_chainreaction_GameViewModel_nativeGetGridState(JNIEnv *env, jobject thiz) {
    if (game == nullptr) return env->NewStringUTF("");
    std::string state = game->getGridState();
    return env->NewStringUTF(state.c_str());
}

/**
 * Checks for a winner. Returns winner's ID or -1.
 */
JNIEXPORT jint JNICALL
Java_com_example_chainreaction_GameViewModel_nativeGetWinner(JNIEnv *env, jobject thiz) {
    if (game == nullptr) return -1;
    return game->getWinner();
}

/**
 * Cleans up the C++ game object from memory.
 */
JNIEXPORT void JNICALL
Java_com_example_chainreaction_GameViewModel_nativeDestroyGame(JNIEnv *env, jobject thiz) {
    if (game != nullptr) {
        delete game;
        game = nullptr;
    }
}

/**
 * Checks if a player has been eliminated from the game.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_chainreaction_GameViewModel_nativeIsPlayerEliminated(JNIEnv *env, jobject thiz, jint player) {
    if (game == nullptr) return false; // Default to not eliminated if game doesn't exist
    return game->isPlayerEliminated(player);
}

/**
 * Returns the animation events from the last move as a Java ArrayList.
 */
JNIEXPORT jobject JNICALL
Java_com_example_chainreaction_GameViewModel_nativeGetLastAnimationEvents(JNIEnv *env, jobject thiz) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject javaArrayList = env->NewObject(arrayListClass, arrayListConstructor);

    if (game == nullptr) return javaArrayList;

    std::vector<OrbAnimationEvent> events = game->getLastAnimationEvents();

    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jclass eventClass = env->FindClass("com/example/chainreaction/OrbAnimationEvent");
    if (eventClass == nullptr) return javaArrayList; // Kotlin class not found

    jmethodID eventConstructor = env->GetMethodID(eventClass, "<init>", "(IIIII)V");

    for (const auto& event : events) {
        jobject javaEvent = env->NewObject(eventClass, eventConstructor,
                                           event.fromRow, event.fromCol,
                                           event.toRow, event.toCol,
                                           event.playerOwner);
        env->CallBooleanMethod(javaArrayList, arrayListAdd, javaEvent);
        env->DeleteLocalRef(javaEvent);
    }

    return javaArrayList;
}

/**
 * NEW: Checks if a given player is controlled by the AI.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_chainreaction_GameViewModel_nativeIsPlayerBot(JNIEnv *env, jobject thiz, jint player) {
    if (game == nullptr) return false;
    return game->isPlayerBot(player);
}

/**
 * NEW: Asks the C++ engine for the bot's move and returns it as an int array [row, col].
 */
JNIEXPORT jintArray JNICALL
Java_com_example_chainreaction_GameViewModel_nativeGetBotMove(JNIEnv *env, jobject thiz, jint player) {
    std::pair<int, int> move = {-1, -1};
    if (game != nullptr) {
        move = game->getBotMove(player);
    }

    jintArray resultArray = env->NewIntArray(2);
    jint moveArray[2] = {move.first, move.second};
    env->SetIntArrayRegion(resultArray, 0, 2, moveArray);

    return resultArray;
}


} // extern "C"