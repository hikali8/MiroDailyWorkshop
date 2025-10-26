// ncnn_detector.cpp
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ncnn/net.h>

#include <vector>

#define LOG_TAG "NCNNDetector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// class names (ordered)
static const std::vector<std::string> MODEL_CLASS_NAMES = {
        "Genshin_CheckIn",
        "HK2_CheckIn",
        "HK3_CheckIn",
        "HSR_CheckIn",
        "ZZZ_CheckIn"};
#define MODEL_INPUT_SIZE 640

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
    if (pModel->load_param(mgr, "yolo11/IconLabeling.param") != 0){
        LOGE("Failed to load param");
        return JNI_FALSE;
    }

    if (pModel->load_model(mgr, "yolo11/IconLabeling.bin") != 0){
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
        h *= static_cast<int>((double)MODEL_INPUT_SIZE / w);
        w = MODEL_INPUT_SIZE;
    } else {
        w *= static_cast<int>((double)MODEL_INPUT_SIZE / h);
        h = MODEL_INPUT_SIZE;
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
    ncnn::Mat padded = ncnn::Mat(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, 3);
    padded.fill(0.f); // 填充0

    // 将调整大小后的图像复制到填充图像的左上角
    for (int c = 0; c < 3; c++) {
        float* padded_ptr = padded.channel(c);
        const float* rgb_ptr = rgb_mat.channel(c);

        for (int y = 0; y < h; y++) {
            memcpy(padded_ptr + y * MODEL_INPUT_SIZE, rgb_ptr + y * w, w * sizeof(float));
        }
    }

//    ncnn::copy_make_border();

    // 归一化: Reasonably suspect that the color changing was not considered by the model.
    const float norm_vals[3] = {1/255.f, 1/255.f, 1/255.f};
    padded.substract_mean_normalize(nullptr, norm_vals);

    return padded;
}

void nms_sorted_bboxes(const std::vector<Object>& objects, std::vector<int>& picked, float nms_threshold) {
    picked.clear();
    const int n = objects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++) {
        // 确保宽度和高度为正数
        float w = std::max(0.0f, objects[i].width);
        float h = std::max(0.0f, objects[i].height);
        areas[i] = w * h;

        // 调试无效的边界框
        if (w <= 0 || h <= 0) {
            LOGW("Invalid bbox at index %d: w=%.3f, h=%.3f", i, w, h);
        }
    }

    for (int i = 0; i < n; i++) {
        const Object& a = objects[i];

        // 跳过无效的边界框
        if (areas[i] <= 0) continue;

        bool keep = true;
        for (int j = 0; j < (int)picked.size(); j++) {
            const Object& b = objects[picked[j]];

            // 计算IoU
            float inter_x1 = std::max(a.x, b.x);
            float inter_y1 = std::max(a.y, b.y);
            float inter_x2 = std::min(a.x + a.width, b.x + b.width);
            float inter_y2 = std::min(a.y + a.height, b.y + b.height);

            float inter_area = std::max(0.0f, inter_x2 - inter_x1) * std::max(0.0f, inter_y2 - inter_y1);
            float union_area = areas[i] + areas[picked[j]] - inter_area;

            if (union_area <= 0) continue;

            float iou = inter_area / union_area;

            if (iou > nms_threshold) {
                keep = false;
                break;
            }
        }

        if (keep) {
            picked.push_back(i);
        }
    }
}

// 后处理函数 - YOLO11输出解析
std::vector<Object> postprocess(const ncnn::Mat& outputs, 
                                float prob_threshold = 0.25f, 
                                float nms_threshold = 0.45f) {
    std::vector<Object> objects;

    LOGI("Output dimensions: dims=%d, w=%d, h=%d, c=%d",
         outputs.dims, outputs.w, outputs.h, outputs.c);

    // YOLO11输出格式: [num_attributes, num_boxes] = [9, 8400]
    // 9 = 4(coords) + 5(class_probs_with_objectness)
    // 注意：这里没有单独的objectness分数，可能是每个类别分数已经包含了objectness
    if (outputs.dims != 2) {
        LOGE("Unexpected output dimensions: %d", outputs.dims);
        return objects;
    }

    const int num_attributes = outputs.h;  // 9
    const int num_boxes = outputs.w;       // 8400
    const int num_classes = MODEL_CLASS_NAMES.size();

    LOGI("Processing %d boxes with %d attributes each, num_classes=%d",
         num_boxes, num_attributes, num_classes);

    // 验证属性数量与类别数量匹配
    if (num_attributes != 4 + num_classes) {
        LOGW("Attribute count %d doesn't match expected 4+%d=%d",
             num_attributes, num_classes, 4 + num_classes);
        // 但我们仍然继续处理，因为可能是其他格式
    }

    const float* data = outputs;

    for (int i = 0; i < num_boxes; i++) {
        // 获取第i个框的所有属性
        // 数据排列: [attribute0_box0, attribute0_box1, ..., attribute0_box8399,
        //           attribute1_box0, attribute1_box1, ..., attribute1_box8399,
        //           ...]
        float x_center = data[0 * num_boxes + i];
        float y_center = data[1 * num_boxes + i];
        float width = data[2 * num_boxes + i];
        float height = data[3 * num_boxes + i];

        // 对于属性数量为9的情况，从第4个属性开始就是类别概率
        // 找到最大概率的类别
        int class_id = -1;
        float max_prob = 0.0f;
        for (int j = 0; j < num_classes; j++) {
            float prob = data[(4 + j) * num_boxes + i];
            if (prob > max_prob) {
                max_prob = prob;
                class_id = j;
            }
        }

        // 使用最大类别概率作为置信度
        float final_prob = max_prob;
        if (final_prob < prob_threshold) continue;

        Object obj;
        // YOLO输出通常是归一化坐标 [0,1]
        obj.x = (x_center - width / 2.0f);  // 转换为左上角坐标
        obj.y = (y_center - height / 2.0f);
        obj.width = width;
        obj.height = height;
        obj.label = class_id;
        obj.prob = final_prob;

        objects.push_back(obj);

        // 调试日志（可选，对于问题排查很有用）
        if (objects.size() <= 5) { // 只打印前几个检测结果
            LOGI("Object %d: class=%d, prob=%.3f, box=(%.3f,%.3f,%.3f,%.3f)",
                 (int)objects.size(), class_id, final_prob,
                 obj.x, obj.y, obj.width, obj.height);
        }
    }

    LOGI("Found %d objects before NMS", objects.size());

    // 如果没有检测到任何对象，直接返回
    if (objects.empty()) {
        return objects;
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

    LOGI("Found %d objects after NMS", filtered_objects.size());

    return filtered_objects;
}

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
    ncnn::Mat input = preprocess(reinterpret_cast<unsigned char*>(wrapper.imageBuffer),
                                 wrapper.width, wrapper.height);
    LOGI("Preprocessed input: %d x %d x %d", input.w, input.h, input.c);

    // 推理
    ncnn::Extractor extractor = pModel->create_extractor();
    extractor.set_light_mode(true);

    extractor.input("in0", input);

    ncnn::Mat output;
    extractor.extract("out0", output);

    LOGI("Model output extracted");

    // 后处理 - 传入原图尺寸用于坐标转换
    std::vector<Object> objects = postprocess(output);
    LOGI("Found %d objects after postprocessing", objects.size());

    // 创建Java对象数组
    jclass do_class = env->FindClass("com/hika/core/aidl/accessibility/DetectedObject");
    if (do_class == nullptr) {
        LOGE("Failed to find DetectedObject class");
        return nullptr;
    }

    jmethodID do_constructor = env->GetMethodID(do_class, "<init>",
                                                "(Ljava/lang/String;Landroid/graphics/Rect;F)V");
    if (do_constructor == nullptr) {
        LOGE("Failed to find DetectedObject constructor");
        env->DeleteLocalRef(do_class);
        return nullptr;
    }

    jclass rect_class = env->FindClass("android/graphics/Rect");
    if (rect_class == nullptr) {
        LOGE("Failed to find Rect class");
        env->DeleteLocalRef(do_class);
        return nullptr;
    }

    jmethodID rect_constructor = env->GetMethodID(rect_class, "<init>", "(IIII)V");
    if (rect_constructor == nullptr) {
        LOGE("Failed to find Rect constructor");
        env->DeleteLocalRef(do_class);
        env->DeleteLocalRef(rect_class);
        return nullptr;
    }

    jobjectArray results = env->NewObjectArray(objects.size(), do_class, nullptr);

    for (size_t i = 0; i < objects.size(); i++) {
        const Object& obj = objects[i];

        // YOLO输出是归一化坐标 [0,1]，需要转换到原图像素坐标
        float x = obj.x * wrapper.width;
        float y = obj.y * wrapper.height;
        float width = obj.width * wrapper.width;
        float height = obj.height * wrapper.height;

        // 确保坐标在图像范围内
        x = std::max(0.f, std::min(x, (float)wrapper.width));
        y = std::max(0.f, std::min(y, (float)wrapper.height));
        width = std::min(width, (float)wrapper.width - x);
        height = std::min(height, (float)wrapper.height - y);

        // 过滤无效检测框
        if (width <= 1 || height <= 1) {
            LOGW("Skipping invalid object: x=%.1f, y=%.1f, w=%.1f, h=%.1f", x, y, width, height);
            continue;
        }

        std::string label = MODEL_CLASS_NAMES[obj.label];
        jstring jlabel = env->NewStringUTF(label.c_str());

        // 创建Rect对象
        jobject rect_obj = env->NewObject(
                rect_class,
                rect_constructor,
                static_cast<int>(x),
                static_cast<int>(y),
                static_cast<int>(x + width),
                static_cast<int>(y + height));

        // 创建DetectedObject对象
        jobject dete_obj = env->NewObject(
                do_class,
                do_constructor,
                jlabel,
                rect_obj,
                obj.prob);

        env->SetObjectArrayElement(results, i, dete_obj);

        // 清理局部引用
        env->DeleteLocalRef(jlabel);
        env->DeleteLocalRef(rect_obj);
        env->DeleteLocalRef(dete_obj);
    }

    // 清理类引用
    env->DeleteLocalRef(do_class);
    env->DeleteLocalRef(rect_class);

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
