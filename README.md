# Clean Chain Reaction for Android

This project is a native Android implementation of the classic strategy game Chain Reaction, built with a focus on clean architecture and high performance. The entire game engine, including a sophisticated, extensible system for computational opponents, is written in C++ and integrated with a modern Jetpack Compose UI.

-----

## \#\# Key Features ✨

* **Hybrid Architecture:** High-performance C++ backend for all game logic, integrated with a fully native Kotlin/Jetpack Compose frontend.
* **Single Player vs. Bot:** Play against a challenging computational opponent with multiple difficulty levels.
* **Local Multiplayer:** Hot-seat multiplayer for 2 to 5 players.
* **Extensible Bot Engine:** A modular engine designed using the Strategy Pattern, making it easy for contributors to add their own opponent logic.
* **Modern, Animated UI:** A smooth and reactive user interface built with Jetpack Compose, featuring animations for game events.
* **Configurable Game Setup:** A clean startup dialog to choose the game mode, number of players, and bot difficulty.

-----

## \#\# Tech Stack & Architecture 🛠️

This project separates the core game logic from the UI for better maintainability and performance.

* **Backend (Game Engine): C++17**

    * Handles all game rules, state management, and move validation.
    * Contains the **Bot Engine**, using multithreading (`std::async`, `std::future`) for high-performance calculations.

* **Frontend (UI): Kotlin & Jetpack Compose**

    * A single-activity architecture using modern Android development patterns.
    * State management is handled by a `ViewModel`, exposing game state via Kotlin Flows.

* **Bridge: JNI & Android NDK**

    * The Java Native Interface (JNI) is used to create a seamless and efficient communication layer between the Kotlin frontend and the C++ backend.

-----

## \#\# The Bot Engine 🧠

The engine for the computational opponents is the core of the single-player experience and was designed to be powerful and extensible.

* **Strategy Pattern:** The engine is built around an `IBotStrategy` interface, allowing different "brains" to be swapped out easily.
* **Implemented Bots:**
    1.  **Random Bot:** A simple opponent that chooses any valid move at random.
    2.  **Greedy Bot:** A heuristic-based opponent that simulates one move ahead and picks the option with the best immediate score gain.
    3.  **Minimax Bot:** An advanced backtracking algorithm that searches the tree of future moves to find the optimal play.
* **Performance Optimizations:**
    * **Alpha-Beta Pruning:** The Minimax algorithm is optimized to intelligently prune branches of the game tree, dramatically reducing calculation time.
    * **Multithreading:** The Minimax search is parallelized to run across multiple CPU cores, allowing for a deeper, more strategic search without freezing the UI.

-----

## \#\# Building and Running the Project

You can build and run this project using standard Android development tools.

### Prerequisites

* Android Studio (latest stable version recommended)
* Android NDK and CMake (installable via the Android Studio SDK Manager)

### Steps

1.  Clone the repository:
    ```bash
    git clone https://github.com/vamsikunal/Clean-Chain-Reaction.git
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync and download any required dependencies.
4.  The C++ code will be automatically compiled by CMake during the first build.
5.  Build and run the app on an Android emulator or a physical device.

-----

## \#\# Contributing 🤝

Contributions are welcome\! A great way to start is by creating your own bot.

The bot architecture is defined in `app/src/main/cpp/include/bot.h`. Simply create a new class that inherits from `IBotStrategy`, implement your `findMove` logic, and add it to the factory in `game.cpp`. The developer comments in the header files provide guidance on which game engine functions are most useful for building your opponent's logic.