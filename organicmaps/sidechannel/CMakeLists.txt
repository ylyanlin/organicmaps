project(sidechannel)

set(SRC
        side_channel.cpp
        side_channel.hpp
)

omim_add_library(${PROJECT_NAME} ${SRC})

#find_library( # Sets the name of the path variable.
#        log-lib
#
#        # Specifies the name of the NDK library that
#        # you want CMake to locate.
#        log )

target_link_libraries(${PROJECT_NAME}
  log
)
