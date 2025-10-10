#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/hardware_buffer_jni.h>
#include <cstdlib>
#include <cstring>

// 引入 libyuv 头文件
#include <libyuv.h>


#if __ANDROID_API__ >= 29
#define API_GREATER_THAN_29 1
#include <android/hardware_buffer_extra.h>
#endif

#define LOG_TAG "libyuv_converter"
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hika_accessibility_recognition_ImageHandler_00024Companion_convertRGBAtoNV21(
        JNIEnv* env,
        jobject thiz,
        jobject rgba_buffer,
        jint rgba_stride,
        jint width,
        jint height,
        jbyteArray nv21_array) {
    // 获取RGBA缓冲区的直接指针
    uint8_t* rgbaData = static_cast<uint8_t*>(env->GetDirectBufferAddress(rgba_buffer));
    if (!rgbaData) {
        ALOGE("Failed to get RGBA buffer address");
        return JNI_FALSE;
    }

    // 获取NV21数组指针
    jbyte* nv21Data = env->GetByteArrayElements(nv21_array, NULL);
    if (!nv21Data) {
        ALOGE("Failed to get NV21 array elements");
        return JNI_FALSE;
    }

    // 转换为uint8_t*
    uint8_t* nv21Buffer = reinterpret_cast<uint8_t*>(nv21Data);

    // use libyuv to convert
    // ABGR still is RGBA (little order), Google made it unclearly

    int result = libyuv::ABGRToNV21(
            rgbaData,           // src_rgba
            rgba_stride,        // src_stride_rgba
            nv21Buffer,         // dst_y
            width,              // dst_stride_y
            nv21Buffer + width * height, // dst_vu
            width,              // dst_stride_vu
            width,              // width
            height              // height
    );

    // 释放NV21数组
    env->ReleaseByteArrayElements(nv21_array, nv21Data, 0);

    if (result != 0) {
        ALOGE("libyuv conversion failed with error: %d", result);
        return JNI_FALSE;
    }

    ALOGI("Successfully converted RGBA to NV21");
    return JNI_TRUE;
}
