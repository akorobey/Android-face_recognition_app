add_subdirectory("${OMZ_DIR}/demos/common/cpp/monitors" "${CMAKE_CURRENT_BINARY_DIR}/monitors")

add_library(
        # Sets the name of the library.
        presenter
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        presenter.cpp)
target_link_libraries(presenter ${OpenCV_LIBRARIES} monitors)
