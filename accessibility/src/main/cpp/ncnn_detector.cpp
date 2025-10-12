// ncnn_detector.cpp
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ncnn/net.h>

//#include <opencv2/core/core.hpp>
#include <vector>

#define LOG_TAG "NCNNDetector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static ncnn::Net* g_net = nullptr;
// class names (ordered)
static std::vector<std::string> G_CLASS_NAMES = {
        "Genshin_CheckIn",
        "HK2_CheckIn",
        "HK3_CheckIn",
        "HSR_CheckIn",
        "ZZZ_CheckIn"};
static int G_INPUT_SIZE = 640;

struct Object {
    float x;
    float y;
    float width;
    float height;
    int label;
    float prob;
};

class RecognizableWrapper {
public:
//    jobject recognizable;
    jbyte* imageBuffer;
    jint width;
    jint height;

    RecognizableWrapper(JNIEnv* env, jobject recognizable_obj) {
        jclass cls = env->GetObjectClass(recognizable_obj);

        // 获取字段ID
        jfieldID imageBufferField = env->GetFieldID(cls, "imageBuffer", "Ljava/nio/ByteBuffer;");
        jfieldID widthField = env->GetFieldID(cls, "width", "I");
        jfieldID heightField = env->GetFieldID(cls, "height", "I");

        // 获取字段值
        jobject buffer_obj = env->GetObjectField(recognizable_obj, imageBufferField);
        // 获取直接内存地址
        imageBuffer = (jbyte*)env->GetDirectBufferAddress(buffer_obj);

        width = env->GetIntField(recognizable_obj, widthField);
        height = env->GetIntField(recognizable_obj, heightField);

        env->DeleteLocalRef(buffer_obj);
        env->DeleteLocalRef(cls);
    }

    ~RecognizableWrapper() {
        // 在析构函数中清理全局引用
    }
};

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_init(
        JNIEnv *env, jobject thiz,
        jobject asset_manager) {
    if (g_net != nullptr) {
        delete g_net;
    }

    g_net = new ncnn::Net();

    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    if (mgr == nullptr) {
        LOGE("AAssetManager is null");
        return JNI_FALSE;
    }

//    // 加载模型文件
//    AAsset* param_asset = AAssetManager_open(mgr, "yolo11/IconLabeling.param", AASSET_MODE_BUFFER);
//    if (param_asset == nullptr) {
//        LOGE("Failed to open yolo11/IconLabeling.param");
//        return JNI_FALSE;
//    }
//
//    size_t param_size = AAsset_getLength(param_asset);
//    char* param_buffer = new char[param_size + 1]; // +1 for null terminator
//    AAsset_read(param_asset, param_buffer, param_size);
//    AAsset_close(param_asset);
//    param_buffer[param_size] = '\0'; // null terminate
//
//    AAsset* bin_asset = AAssetManager_open(mgr, "yolo11/IconLabeling.bin", AASSET_MODE_BUFFER);
//    if (bin_asset == nullptr) {
//        LOGE("Failed to open yolo11/IconLabeling.bin");
//        delete[] param_buffer;
//        return JNI_FALSE;
//    }
//
//    size_t bin_size = AAsset_getLength(bin_asset);
//    char* bin_buffer = new char[bin_size + 1];
//    AAsset_read(bin_asset, bin_buffer, bin_size);
//    AAsset_close(bin_asset);
//    bin_buffer[bin_size] = '\0'; // null terminate

    // 加载模型到ncnn
    if (g_net->load_param(mgr, "yolo11/IconLabeling.param") != 0){
        LOGE("Failed to load param");
        return JNI_FALSE;
    }

//    if (g_net->load_param_mem(param_buffer) != 0) {
//        LOGE("Failed to load param");
//        delete[] param_buffer;
//        delete[] bin_buffer;
//        return JNI_FALSE;
//    }

    if (g_net->load_model(mgr, "yolo11/IconLabeling.bin") != 0){
        LOGE("Failed to load model");
        return JNI_FALSE;
    }
//    if (g_net->load_model((const unsigned char*)bin_buffer) != 0) {
//        LOGE("Failed to load model");
//        delete[] param_buffer;
//        delete[] bin_buffer;
//        return JNI_FALSE;
//    }

//    delete[] param_buffer;
//    delete[] bin_buffer;

    LOGI("NCNN model loaded successfully");
    return JNI_TRUE;
}

// 预处理函数
ncnn::Mat preprocess(const unsigned char* rgba_data, int width, int height) {
    // 计算缩放宽高
    int w = width;
    int h = height;

    // 计算缩放比例
    if (w > h) {
        h *= (float)G_INPUT_SIZE / w;
        w = G_INPUT_SIZE;
    } else {
        w *= (float)G_INPUT_SIZE / h;
        h = G_INPUT_SIZE;
    }

    // tencent magic: read rgba & convert to rgb & resize to target scale
    ncnn::Mat rgb_mat = ncnn::Mat::from_pixels_resize(
            rgba_data,
            ncnn::Mat::PIXEL_RGBA2RGB,
            width,
            height,
            w,
            h);

    // 创建填充图像
    ncnn::Mat padded = ncnn::Mat(G_INPUT_SIZE, G_INPUT_SIZE, 3);
    padded.fill(0.f); // 填充0

    // 将调整大小后的图像复制到填充图像的左上角
    for (int c = 0; c < 3; c++) {
        float* padded_ptr = padded.channel(c);
        const float* rgb_ptr = rgb_mat.channel(c);

        for (int y = 0; y < h; y++) {
            memcpy(padded_ptr + y * G_INPUT_SIZE, rgb_ptr + y * w, w * sizeof(float));
        }
    }

    // 归一化
    const float norm_vals[3] = {1/255.f, 1/255.f, 1/255.f};
    padded.substract_mean_normalize(nullptr, norm_vals);

    return padded;
}

void nms_sorted_bboxes(const std::vector<Object>& objects, std::vector<int>& picked, float nms_threshold) {
    picked.clear();
    const int n = objects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        areas[i] = objects[i].width * objects[i].height; // 计算每个框的面积
    }

    for (int i = 0; i < n; i++) {
        const Object& a = objects[i];

        bool keep = true;
        for (int j = 0; j < (int)picked.size(); j++) {
            const Object& b = objects[picked[j]];

            // 计算IoU
            float inter_area = std::max(0.f, std::min(a.x + a.width, b.x + b.width) - std::max(a.x, b.x)) *
                               std::max(0.f, std::min(a.y + a.height, b.y + b.height) - std::max(a.y, b.y));
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            float iou = inter_area / union_area;

            if (iou > nms_threshold) {
                keep = false; // 如果重叠度太高，抑制当前框
                break;
            }
        }

        if (keep) {
            picked.push_back(i); // 保留这个框
        }
    }
}

// 后处理函数 - YOLO11输出解析
std::vector<Object> postprocess(const ncnn::Mat& outputs, float prob_threshold = 0.25f, float nms_threshold = 0.45f) {
    std::vector<Object> objects;

    // 解析YOLO输出 (格式: [batch, num_detections, 6] 或 [batch, 6, num_detections])
    // 具体格式需要根据你的模型输出调整
    const float* data = outputs;
    int num_detections = outputs.w; // 假设输出形状为 [1, 6, num_detections]

    for (int i = 0; i < num_detections; i++) {
        const float* detection = data + i * 6; // [x, y, w, h, conf, class_id]

        float confidence = detection[4];
        if (confidence < prob_threshold) continue;

        int class_id = static_cast<int>(detection[5]);
        float final_prob = confidence;

        Object obj;
        obj.x = detection[0] - detection[2] / 2;  // x_center - width/2
        obj.y = detection[1] - detection[3] / 2;  // y_center - height/2
        obj.width = detection[2];
        obj.height = detection[3];
        obj.label = class_id;
        obj.prob = final_prob;

        objects.push_back(obj);
    }

    // 按置信度排序
    std::sort(objects.begin(), objects.end(), [](const Object& a, const Object& b) {
        return a.prob > b.prob;
    });

    // NMS非极大值抑制
    std::vector<int> picked;
    nms_sorted_bboxes(objects, picked, nms_threshold);

    std::vector<Object> filtered_objects;
    for (int idx : picked) {
        filtered_objects.push_back(objects[idx]);
    }

    return filtered_objects;
}

JNIEXPORT jobjectArray JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_detect(
        JNIEnv *env, jobject thiz,
        jobject recognizable) {
    if (g_net == nullptr) {
        LOGE("Model not initialized");
        return nullptr;
    }

    RecognizableWrapper wrapper(env, recognizable);

    // 获取图像数据
    if (wrapper.imageBuffer == nullptr) {
        LOGE("Failed to get image data");
        return nullptr;
    }

    // 预处理
    ncnn::Mat input = preprocess(reinterpret_cast<unsigned char*>(wrapper.imageBuffer),
                                 wrapper.width, wrapper.height);

    // 推理
    ncnn::Extractor extractor = g_net->create_extractor();
    extractor.set_light_mode(true);
//    extractor.set_num_threads(4); // deepseek redundant

    extractor.input("in0", input);  // 根据你的模型输入名称调整

    ncnn::Mat output;
    extractor.extract("out0", output);  // 根据你的模型输出名称调整

    // 后处理
    std::vector<Object> objects = postprocess(output);

    // Find constructor of custom Java class
    jclass do_class = env->FindClass("com/hika/core/aidl/accessibility/DetectedObject");
    jmethodID do_constructor = env->GetMethodID(do_class, "<init>",
                                                "(Ljava/lang/String;Landroid/graphics/Rect;F)V");

    jclass rect_class = env->FindClass("android/graphics/Rect");
    jmethodID rect_constructor = env->GetMethodID(rect_class, "<init>", "(IIII)V");

    jobjectArray results = env->NewObjectArray(objects.size(), do_class, nullptr);

    for (size_t i = 0; i < objects.size(); i++) {
        const Object& obj = objects[i];

        // 将坐标转换回原图尺寸
        float scale_x = (float)wrapper.width / G_INPUT_SIZE;
        float scale_y = (float)wrapper.height / G_INPUT_SIZE;

        float x = obj.x * scale_x;
        float y = obj.y * scale_y;
        float width = obj.width * scale_x;
        float height = obj.height * scale_y;

        // 确保坐标在图像范围内
        x = std::max(0.f, std::min(x, (float)wrapper.width));
        y = std::max(0.f, std::min(y, (float)wrapper.height));
        width = std::min(width, (float)wrapper.width - x);
        height = std::min(height, (float)wrapper.height - y);

        std::string label = G_CLASS_NAMES[obj.label];
        jstring jlabel = env->NewStringUTF(label.c_str());

        // 第一步：创建Rect对象
        jobject rect_obj = env->NewObject(
                rect_class,
                rect_constructor,
                static_cast<int>(x),
                static_cast<int>(y),
                static_cast<int>(x + width),
                static_cast<int>(y + height));
        // 第二步：创建DetectedObject对象
        jobject dete_obj = env->NewObject(
                do_class,
                do_constructor,
                jlabel,
                rect_obj,
                obj.prob);

        env->SetObjectArrayElement(results, i, dete_obj);

        env->DeleteLocalRef(jlabel);
        env->DeleteLocalRef(dete_obj);
    }

    return results;
}

JNIEXPORT void JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_release(
        JNIEnv *env,
        jobject thiz) {
    if (g_net != nullptr) {
        delete g_net;
        g_net = nullptr;
    }
}

}



