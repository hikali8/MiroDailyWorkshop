cmake_minimum_required(VERSION 3.22.1)

project("libyuv_converter")

add_library(${CMAKE_PROJECT_NAME} SHARED
        libyuv_converter.cpp)

# 添加 libyuv
# 只抓取 C/C++ 源
file(GLOB libyuv_sources
        "${CMAKE_SOURCE_DIR}/libyuv/*.c"
        "${CMAKE_SOURCE_DIR}/libyuv/*.cc")

# ===== 显式排除 SVE / SVE2 / SME 源文件（避免编译/链接相关实现）=====
list(FILTER libyuv_sources EXCLUDE REGEX ".*/row_sve.*\\.cc$")
list(FILTER libyuv_sources EXCLUDE REGEX ".*/scale_sve.*\\.cc$")
list(FILTER libyuv_sources EXCLUDE REGEX ".*/rotate_sve.*\\.cc$")

list(FILTER libyuv_sources EXCLUDE REGEX ".*/row_sme.*\\.cc$")
list(FILTER libyuv_sources EXCLUDE REGEX ".*/scale_sme.*\\.cc$")
list(FILTER libyuv_sources EXCLUDE REGEX ".*/rotate_sme.*\\.cc$")

# （可选）也可排除非 ARM 架构实现，减少无关文件：
# list(FILTER libyuv_sources EXCLUDE REGEX ".*/row_(lsx|lasx|msa|rvv).*\\.cc$")
# list(FILTER libyuv_sources EXCLUDE REGEX ".*/rotate_(lsx|lasx|msa).*\\.cc$")
# list(FILTER libyuv_sources EXCLUDE REGEX ".*/scale_(lsx|lasx|msa|rvv).*\\.cc$")

message(STATUS "libyuv_sources: ${libyuv_sources}")

add_library(libyuv SHARED ${libyuv_sources})
include_directories("${CMAKE_SOURCE_DIR}/includes")

# 关闭 SVE/SVE2/SME 的宏，防止头文件声明到这些符号
target_compile_definitions(libyuv PRIVATE
        LIBYUV_DISABLE_SVE
        LIBYUV_DISABLE_SVE2
        LIBYUV_DISABLE_SME)

# 仅在 arm64-v8a 生效，启用 dotprod + i8mm（NEON 路径会使用）
if(ANDROID_ABI STREQUAL arm64-v8a)
    target_compile_options(libyuv PRIVATE -march=armv8.2-a+dotprod+i8mm)
endif()

# ====== 关键新增：查找并链接系统库到 libyuv_converter ======
# AHardwareBuffer_* 在 libandroid，__android_log_print 在 liblog
find_library(ANDROID_LIB android)
find_library(LOG_LIB log)

# 把 libyuv、android、log 都链接到 libyuv_converter（显式目标名，避免变量歧义）
target_link_libraries(libyuv_converter
        PRIVATE
        libyuv
        ${ANDROID_LIB}
        ${LOG_LIB}
)