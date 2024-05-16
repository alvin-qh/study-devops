cmake_minimum_required(VERSION 3.10)
project(study_devops_tools_vscode_cpp CXX C)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

#####

include_directories("src")
enable_testing()

if (CMAKE_BUILD_TYPE STREQUAL Debug)
    message("build for debug...")
    add_definitions(-DDEBUG)
else()
    message("build for release...")
    add_compile_options(-O3)
endif()

#####

set(app_src_files
    src/text.cpp
    src/main.cpp)

add_executable(app
    ${app_src_files})
