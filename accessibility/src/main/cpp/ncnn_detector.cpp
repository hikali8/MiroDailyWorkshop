// ncnn_detector.cpp
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ncnn/net.h>

#include <vector>

#define LOG_TAG "#0x-ND"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// class names (ordered)
static const std::vector<std::string> MODEL_CLASS_NAMES = {
        "GS_CheckIn",
        "HK2_CheckIn",
        "HK3_CheckIn",
        "HSR_CheckIn",
        "ZZZ_CheckIn"};
#define INPUT_SIZE 640

struct Object {
    float confidence;
    int classIndex;
    float lv[4];  // location_values.
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


static ncnn::Net* pModel = nullptr;

extern "C" {
JNIEXPORT jboolean JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_init(
        JNIEnv *env,
        jobject thiz,
        jobject asset_manager) {
    if (pModel != nullptr) {
        LOGI("NCNN model was loaded.");
        return JNI_TRUE;
    }

    pModel = new ncnn::Net();

    AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
    if (mgr == nullptr) {
        LOGE("AAssetManager is null");
        return JNI_FALSE;
    }

    // 加载模型到ncnn
    if (pModel->load_param(mgr, "ncnn/IconLabeling.param") != 0){
        LOGE("Failed to load param");
        return JNI_FALSE;
    }

    if (pModel->load_model(mgr, "ncnn/IconLabeling.bin") != 0){
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

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
        h = h * INPUT_SIZE / (double)w;
        w = INPUT_SIZE;
    } else {
        w = w * INPUT_SIZE / (double)h;
        h = INPUT_SIZE;
    }

    // tencent method: read rgba & convert to rgb & resize to target scale
    ncnn::Mat mat1 = ncnn::Mat::from_pixels_resize(
            rgba_data,
            //ncnn::Mat::PIXEL_RGBA2RGB,
            ncnn::Mat::PIXEL_BGR2RGB,
            width,
            height,
            w,
            h);
    // 将调整大小后的图像复制到图像中央
    double wpadP2 = (INPUT_SIZE - w) / 2.0;
    double hpadP2 = (INPUT_SIZE - h) / 2.0;
    ncnn::Mat mat2;
    ncnn::copy_make_border(mat1, mat2,
                           floor(hpadP2),
                           ceil(hpadP2),
                           floor(wpadP2),
                           ceil(wpadP2),
                           ncnn::BorderType::BORDER_CONSTANT,
                           114.f);
    // 归一化: Reasonably suspect that the color changing was not considered by the model.
    const float norm_vals[] = {1/255.f, 1/255.f, 1/255.f};
    mat2.substract_mean_normalize(nullptr, norm_vals);
    return mat2;
}

//void nms_sorted_bboxes(const std::vector<Object>& objects, std::vector<int>& picked, float nms_threshold) {
//    picked.clear();
//    const int n = objects.size();
//
//    std::vector<float> areas(n);
//    for (int i = 0; i < n; i++) {
//        // 确保宽度和高度为正数
//        float w = std::max(0.0f, objects[i].width);
//        float h = std::max(0.0f, objects[i].height);
//        areas[i] = w * h;
//
//        // 调试无效的边界框
//        if (w <= 0 || h <= 0) {
//            LOGW("Invalid bbox at index %d: w=%.3f, h=%.3f", i, w, h);
//        }
//    }
//
//    for (int i = 0; i < n; i++) {
//        const Object& a = objects[i];
//
//        // 跳过无效的边界框
//        if (areas[i] <= 0) continue;
//
//        bool keep = true;
//        for (int j = 0; j < (int)picked.size(); j++) {
//            const Object& b = objects[picked[j]];
//
//            // 计算IoU
//            float inter_x1 = std::max(a.x, b.x);
//            float inter_y1 = std::max(a.y, b.y);
//            float inter_x2 = std::min(a.x + a.width, b.x + b.width);
//            float inter_y2 = std::min(a.y + a.height, b.y + b.height);
//
//            float inter_area = std::max(0.0f, inter_x2 - inter_x1) * std::max(0.0f, inter_y2 - inter_y1);
//            float union_area = areas[i] + areas[picked[j]] - inter_area;
//
//            if (union_area <= 0) continue;
//
//            float iou = inter_area / union_area;
//
//            if (iou > nms_threshold) {
//                keep = false;
//                break;
//            }
//        }
//
//        if (keep) {
//            picked.push_back(i);
//        }
//    }
//}


// 后处理函数 - YOLO11输出解析
//std::vector<Object> postprocess(const ncnn::Mat& outputs,
//                                float prob_threshold = 0.25f,
//                                float nms_threshold = 0.45f) {
//
//
//    std::vector<Object> objects;
//
//    LOGI("Output dimensions: dims=%d, w=%d, h=%d, c=%d",
//         outputs.dims, outputs.w, outputs.h, outputs.c);
//
//    const float* data = outputs;
//    const int num_classes = 5;
//
//    // 在postprocess开头添加
//    LOGI("Checking data format...");
//
//// 打印几个完整框的信息
//    for (int i = 0; i < 2; i++) {
//        LOGI("Box %d full attributes:", i);
//        for (int j = 0; j < outputs.h; j++) {
//            float val = data[j * outputs.w + i];
//            LOGI("  [%d]: %.6f", j, val);
//        }
//    }
//
//// 检查所有类别分数是否都很小
//    int small_score_count = 0;
//    for (int i = 0; i < outputs.w; i++) {
//        for (int j = 4; j < outputs.h; j++) {
//            float val = data[j * outputs.w + i];
//            if (fabs(val) < 0.001f) small_score_count++;
//        }
//    }
//    LOGI("Small score (<0.001) count: %d/%d", small_score_count, (outputs.h-4) * outputs.w);
//
//    // 方法1：尝试直接归一化，不应用exp
//    for (int i = 0; i < outputs.w; i++) {
//        // 获取当前框的所有属性
//        float x_center = data[0 * outputs.w + i];
//        float y_center = data[1 * outputs.w + i];
//        float width = data[2 * outputs.w + i];
//        float height = data[3 * outputs.w + i];
//
//        // 找到最大类别分数
//        int class_id = -1;
//        float max_score = -FLT_MAX;
//        for (int j = 0; j < num_classes; j++) {
//            float score = data[(4 + j) * outputs.w + i];
//            if (score > max_score) {
//                max_score = score;
//                class_id = j;
//            }
//        }
//
//        // 注意：这里max_score是原始值，不是概率！
//        // 原始值很小（约0.0001），需要进一步处理
//
//        // 尝试1：直接使用原始值作为置信度
//        // float confidence = max_score;
//
//        // 尝试2：如果值是未激活的，可能需要sigmoid
//        float confidence = 1.0f / (1.0f + expf(-max_score));
//
//        // 尝试3：也可能是logits，需要softmax，但更可能是sigmoid
//
//        if (confidence < prob_threshold) {
//            continue;
//        }
//
//        // 关键：坐标解码
//        // 查看前几个坐标值：3.0, 10.3, 18.7, 26.8, 34.7...
//        // 这些可能是网格索引 * 步长 + sigmoid(offset)
//        // 但更简单的方法：直接除以特征图尺寸（80、40、20）看看
//
//        // 尝试1：直接除以640（输入尺寸）
//        float norm_x = x_center / INPUT_SIZE;
//        float norm_y = y_center / INPUT_SIZE;
//        float norm_w = width / INPUT_SIZE;
//        float norm_h = height / INPUT_SIZE;
//
//        // 转换为左上角坐标
//        norm_x = norm_x - norm_w / 2.0f;
//        norm_y = norm_y - norm_h / 2.0f;
//
//        // 确保在[0,1]范围内
//        norm_x = std::max(0.0f, std::min(1.0f, norm_x));
//        norm_y = std::max(0.0f, std::min(1.0f, norm_y));
//        norm_w = std::max(0.0f, std::min(1.0f - norm_x, norm_w));
//        norm_h = std::max(0.0f, std::min(1.0f - norm_y, norm_h));
//
//        // 过滤无效框
//        if (norm_w <= 0.01f || norm_h <= 0.01f) {
//            continue;
//        }
//
//        Object obj;
//        obj.x = norm_x;
//        obj.y = norm_y;
//        obj.width = norm_w;
//        obj.height = norm_h;
//        obj.label = class_id;
//        obj.prob = confidence;
//
//        objects.push_back(obj);
//
//        if (objects.size() <= 3) {
//            LOGI("Object %d: class=%d, prob=%.6f, raw_scores[4-8]=%.6f,%.6f,%.6f,%.6f,%.6f",
//                 (int)objects.size(), class_id, confidence,
//                 data[4 * outputs.w + i], data[5 * outputs.w + i],
//                 data[6 * outputs.w + i], data[7 * outputs.w + i],
//                 data[8 * outputs.w + i]);
//            LOGI("  Box: norm=(%.3f,%.3f,%.3f,%.3f), raw=(%.3f,%.3f,%.3f,%.3f)",
//                 norm_x, norm_y, norm_w, norm_h,
//                 x_center, y_center, width, height);
//        }
//    }
//
//    LOGI("Found %d objects before NMS", objects.size());
//
//    if (objects.empty()) {
//        LOGI("No objects detected with threshold %.2f", prob_threshold);
//
//        // 调试：检查前10个框的最大分数
//        LOGI("Checking first 10 boxes max scores:");
//        for (int i = 0; i < std::min(10, outputs.w); i++) {
//            float max_score = -FLT_MAX;
//            for (int j = 0; j < num_classes; j++) {
//                float score = data[(4 + j) * outputs.w + i];
//                if (score > max_score) max_score = score;
//            }
//            float sigmoid_score = 1.0f / (1.0f + expf(-max_score));
//            LOGI("  Box %d: raw_max=%.6f, sigmoid=%.6f", i, max_score, sigmoid_score);
//        }
//        return objects;
//    }
//
//    // 按置信度排序
//    std::sort(objects.begin(), objects.end(), [](const Object& a, const Object& b) {
//        return a.prob > b.prob;
//    });
//
//    // NMS
//    std::vector<int> picked;
//    nms_sorted_bboxes(objects, picked, nms_threshold);
//
//    std::vector<Object> filtered_objects;
//    for (int idx : picked) {
//        filtered_objects.push_back(objects[idx]);
//    }
//
//    LOGI("Found %d objects after NMS", filtered_objects.size());
//
//    return filtered_objects;
//}

JNIEXPORT jobjectArray JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_detect(
        JNIEnv *env, jobject thiz,
        jobject recognizable) {
    if (pModel == nullptr) {
        LOGE("Model not initialized");
        return nullptr;
    }

    RecognizableWrapper wrapper(env, recognizable);

    LOGI("Input image: %d x %d", wrapper.width, wrapper.height);

    if (wrapper.imageBuffer == nullptr) {
        LOGE("Failed to get image data");
        return nullptr;
    }

    // 预处理
    ncnn::Mat in_mat = preprocess(reinterpret_cast<unsigned char*>(wrapper.imageBuffer),
                                  wrapper.width, wrapper.height);
    LOGI("done with preprocess.");
    // ncnn::Mat::shape() always fails.
//    LOGI("Preprocessed. Got in_mat, out_shape %d %d %d .", in_mat.w, in_mat.h, in_mat.c);
    // 推理并识别
    ncnn::Mat out_mat;
    ncnn::Extractor extractor = pModel->create_extractor();
    extractor.input("in0", in_mat);
    extractor.extract("out0", out_mat);

    std::vector<Object> objects;
    int mat_w = out_mat.w;
    int mat_h = out_mat.h;

    for (int i = 0; i < 27; i++){
        LOGI("try death: %f", out_mat[i]);
    }

//    LOGW("Got out_mat, out_shape %d %d %d .", out_mat.w, mat_h, out_mat.c);


//// 先尝试高阈值
//    float thresholds[] = {0.7f, 0.5f, 0.0001f, 0.001f, 0.01f};
//    for (float thresh : thresholds) {
//        objects = postprocess(out_mat, thresh, 0.45f);
//        if (!objects.empty()) {
//            LOGI("Found objects with threshold %.4f", thresh);
//            break;
//        }
//    }
//
//    // 创建Java对象数组
    jclass do_class = env->FindClass("com/hika/core/aidl/accessibility/DetectedObject");
//    if (do_class == nullptr) {
//        LOGE("Failed to find DetectedObject class");
//        return nullptr;
//    }
//
//    jmethodID do_constructor = env->GetMethodID(do_class, "<init>",
//                                                "(Ljava/lang/String;Landroid/graphics/Rect;F)V");
//    if (do_constructor == nullptr) {
//        LOGE("Failed to find DetectedObject constructor");
//        env->DeleteLocalRef(do_class);
//        return nullptr;
//    }
//
//    jclass rect_class = env->FindClass("android/graphics/Rect");
//    if (rect_class == nullptr) {
//        LOGE("Failed to find Rect class");
//        env->DeleteLocalRef(do_class);
//        return nullptr;
//    }
//
//    jmethodID rect_constructor = env->GetMethodID(rect_class, "<init>", "(IIII)V");
//    if (rect_constructor == nullptr) {
//        LOGE("Failed to find Rect constructor");
//        env->DeleteLocalRef(do_class);
//        env->DeleteLocalRef(rect_class);
//        return nullptr;
//    }
//
    jobjectArray results = env->NewObjectArray(objects.size(), do_class, nullptr);
//
//    for (size_t i = 0; i < objects.size(); i++) {
//        const Object& obj = objects[i];
//
//        // YOLO输出是归一化坐标 [0,1]，需要转换到原图像素坐标
//        float x = obj.x * wrapper.width;
//        float y = obj.y * wrapper.height;
//        float width = obj.width * wrapper.width;
//        float height = obj.height * wrapper.height;
//
//        // 确保坐标在图像范围内
//        x = std::max(0.f, std::min(x, (float)wrapper.width));
//        y = std::max(0.f, std::min(y, (float)wrapper.height));
//        width = std::min(width, (float)wrapper.width - x);
//        height = std::min(height, (float)wrapper.height - y);
//
//        // 过滤无效检测框
//        if (width <= 1 || height <= 1) {
//            LOGW("Skipping invalid object: x=%.1f, y=%.1f, w=%.1f, h=%.1f", x, y, width, height);
//            continue;
//        }
//
//        std::string label = MODEL_CLASS_NAMES[obj.label];
//        jstring jlabel = env->NewStringUTF(label.c_str());
//
//        // 创建Rect对象
//        jobject rect_obj = env->NewObject(
//                rect_class,
//                rect_constructor,
//                static_cast<int>(x),
//                static_cast<int>(y),
//                static_cast<int>(x + width),
//                static_cast<int>(y + height));
//
//        // 创建DetectedObject对象
//        jobject dete_obj = env->NewObject(
//                do_class,
//                do_constructor,
//                jlabel,
//                rect_obj,
//                obj.prob);
//
//        env->SetObjectArrayElement(results, i, dete_obj);
//
//        // 清理局部引用
//        env->DeleteLocalRef(jlabel);
//        env->DeleteLocalRef(rect_obj);
//        env->DeleteLocalRef(dete_obj);
//    }
//
//    // 清理类引用
//    env->DeleteLocalRef(do_class);
//    env->DeleteLocalRef(rect_class);

    return results;
}

JNIEXPORT void JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_release(
        JNIEnv *env,
        jobject thiz) {
    if (pModel != nullptr) {
        delete pModel;
        pModel = nullptr;
    }
}

}
