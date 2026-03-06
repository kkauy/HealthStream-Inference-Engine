cmake_minimum_required(VERSION 3.10)

# Project name
project(native_relay)

# Use C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include header directory
include_directories(include)

# Build executable
add_executable(relay
        src/relay_main.cpp
)