# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.22.1)

# Sets the name of the project.
project("chainreaction")

# Creates your game's shared library.
add_library(chainreaction SHARED
        # Add your C++ source files here.
        game.cpp
        bot.cpp
        jni_bridge.cpp)

# Link the necessary Android libraries to your target.
# - 'android' is required for JNI functions.
# - 'log' is required for logging to Logcat from C++.
target_link_libraries(chainreaction
        android
        log)
