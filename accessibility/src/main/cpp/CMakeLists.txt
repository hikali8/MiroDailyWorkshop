cmake_minimum_required(VERSION 3.22.1)
project("libyuv_converter")

# 0. <include> directory
include_directories("${CMAKE_SOURCE_DIR}/includes")

# 1. libyuv_converter."dll" to use in Kotlin
add_library(
    ${CMAKE_PROJECT_NAME}
    SHARED
    libyuv_converter.cpp
)

# 2. libyuv."lib" to use in Cpp
add_library(
    libyuv
    STATIC
    IMPORTED
)

set_target_properties(
    libyuv
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/libs/libyuv/${ANDROID_ABI}/libyuv.a
)

# 3. ncnn."lib" to use in Cpp
add_library(
    ncnn
    STATIC
    IMPORTED
)

set_target_properties(
    ncnn
    PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/libs/ncnn/${ANDROID_ABI}/libncnn.a
)


# ====== 关键新增：查找并链接系统库到 libyuv_converter ======
# AHardwareBuffer_* 在 libandroid，__android_log_print 在 liblog
find_library(ANDROID_LIB android)
find_library(LOG_LIB log)

# 把 libyuv、android、log 都链接到 libyuv_converter
target_link_libraries(
    libyuv_converter
    PRIVATE
    libyuv
    ncnn
    ${ANDROID_LIB}
    ${LOG_LIB}
)