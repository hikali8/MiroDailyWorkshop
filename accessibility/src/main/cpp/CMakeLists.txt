cmake_minimum_required(VERSION 3.22.1)
project("libyuv_converter")

# 查找 OpenMP
find_package(OpenMP REQUIRED)

# 0. <include> directory
include_directories("${CMAKE_SOURCE_DIR}/includes")

# 关键：查找系统库
find_library(ANDROID_LIB android)
find_library(LOG_LIB log)
# AHardwareBuffer_* 在 libandroid，__android_log_print 在 liblog



# 1. libyuv_converter."dll" to use in Kotlin
add_library(
    ${CMAKE_PROJECT_NAME}
    SHARED
    libyuv_converter.cpp
)

# 1.1. libyuv."lib" to use in libyuv_converter."dll" Cpp
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

# 把 libyuv、android、log 都链接到 libyuv_converter
target_link_libraries(
    libyuv_converter
    PRIVATE
    libyuv
    ${ANDROID_LIB}
    ${LOG_LIB}
)



# 2. ncnn_detector."dll" to use in Kotlin
add_library(
    ncnn_detector
    SHARED
    ncnn_detector.cpp
)

# 2.1. multiple-ncnn."lib"s to use in ncnn_detector."dll" Cpp
# Array of "lib"s
set(NCNN_LIBS ncnn glslang SPIRV OSDependent GenericCodeGen glslang-default-resource-limits MachineIndependent)

# 遍历数组，为每个库创建 IMPORTED 目标
foreach(lib ${NCNN_LIBS})
    add_library(${lib} STATIC IMPORTED)
    set_target_properties(${lib} PROPERTIES
            IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/ncnn/${ANDROID_ABI}/lib${lib}.a
    )
endforeach()

# 把 ncnn家族、android、log、OpenMP 都链接到 ncnn_detector
target_link_libraries(
    ncnn_detector
    PRIVATE
    ${NCNN_LIBS}
    ${ANDROID_LIB}
    ${LOG_LIB}
    OpenMP::OpenMP_CXX
)

# 如果需要，也可以设置编译标志
if(OpenMP_CXX_FOUND)
    target_compile_options(ncnn_detector PRIVATE ${OpenMP_CXX_FLAGS})
endif()
