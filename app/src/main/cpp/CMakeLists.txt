cmake_minimum_required(VERSION 3.22.1)
project("native-libs")
set (CMAKE_CXX_STANDARD 17)
find_package( OpenCV REQUIRED )

add_subdirectory(presenter)