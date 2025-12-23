// ncnn_detector.cpp
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <ncnn/net.h>
#include <cassert>
#include <vector>

#define LOG_TAG "#0x-ND"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace std;


const char* CLASS_NAMES[] = {
        "GS_CheckIn",
        "HK2_CheckIn",
        "HK3_CheckIn",
        "HSR_CheckIn",
        "ZZZ_CheckIn"};

#define INPUT_SIZE 640

static ncnn::Net* pModel = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
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
    // 加载结构到ncnn
    // ncnn::Net::load_param has parsing problem, load_param_mem after reading mem is ok.
    AAsset* paramAsset = AAssetManager_open(
            mgr, "ncnn/IconLabeling.param", AASSET_MODE_UNKNOWN);
    if (!paramAsset) {
        LOGE("Failed to open ncnn/IconLabeling.param with pointer %p", mgr);
        return JNI_FALSE;
    }
    if (pModel->load_param_mem((const char*)AAsset_getBuffer(paramAsset)) != 0){
        LOGE("Failed to load param");
        AAsset_close(paramAsset);
        return JNI_FALSE;
    }
    AAsset_close(paramAsset);

    if (pModel->load_model(mgr, "ncnn/IconLabeling.bin") != 0){
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

    LOGI("NCNN model loaded successfully");
    return JNI_TRUE;
}

// 预处理函数
pair<double, double>
image_normalizing(const unsigned char* imageBuffer, int width, int height, ncnn::Mat& in_mat) {
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
            imageBuffer,
            ncnn::Mat::PIXEL_RGBA2RGB,
            width,
            height,
            w,
            h);
    // 将调整大小后的图像复制到图像中央
    double wpadP2 = (INPUT_SIZE - w) / 2.0;
    double hpadP2 = (INPUT_SIZE - h) / 2.0;
    ncnn::copy_make_border(
            mat1, in_mat,
            floor(hpadP2),
            ceil(hpadP2),
            floor(wpadP2),
            ceil(wpadP2),
            ncnn::BorderType::BORDER_CONSTANT,
            114.f);
    const float norm_vals[] = {1/255.f, 1/255.f, 1/255.f};
    in_mat.substract_mean_normalize(nullptr, norm_vals);
    return make_pair(wpadP2, hpadP2);
}

struct Object {
    float confidence;
    int classIndex{};
    float floats[4]{};  // location_values.

    Object(): confidence(0){ }

    bool isOverlappingWith(const Object& obj){
        for (int i = 0; i < 4; i++)
            if (abs(floats[i] / obj.floats[i] - 1) > 0.3)
                return false;
        return true;
    }

    // true to put back, false to ignore
    bool replaceConfidenceLesser(const vector<unique_ptr<Object>>& objects){
        for (auto& obj_past : objects)
            if (classIndex == obj_past->classIndex && isOverlappingWith(*obj_past)){
                if (confidence > obj_past->confidence){
                    obj_past->confidence = confidence;
                    memcpy(obj_past->floats, floats,sizeof(obj_past->floats));
                }
                return false;
            }
        return true;
    }
};

vector<unique_ptr<Object>> infer_and_recognize(const ncnn::Mat& in_mat, float confidence){
    ncnn::Mat out_mat;
    ncnn::Extractor extractor = pModel->create_extractor();
    extractor.input("in0", in_mat);
    extractor.extract("out0", out_mat);
    assert(out_mat.c == 1);
    int mat_w = out_mat.w;
    int mat_h = out_mat.h;
    vector<unique_ptr<Object>> objects;

    for (int i = 0; i < mat_w; i++){
        auto obj = make_unique<Object>();
        for (int j = 4; j < mat_h; j++){
            float score = out_mat[i + mat_w * j];
            if (score > confidence && score > obj->confidence){
                obj->confidence = score;
                obj->classIndex = j - 4;
                for (int k = 0; k < 4; k++)
                    obj->floats[k] = out_mat[i + mat_w * k];
            }
        }
        if (obj->confidence > 0 && obj->replaceConfidenceLesser(objects)) {
            objects.push_back(std::move(obj));
        }
    }
    return objects;
}

jobjectArray correction(JNIEnv *env, const vector<unique_ptr<Object>>& objects, double wpadP2, double hpadP2, double ratio){
    // 建立Cpp到Java的连接
    jclass dobj_jcls, rect_jcls;        // detected_object_jclass & rect_jclass
    jmethodID dobj_jcstr, rect_jcstr;   // detected_object_jconstructor & rect_jconstructor
    dobj_jcls = env->FindClass("com/hika/core/aidl/accessibility/DetectedObject");
    if (dobj_jcls == nullptr) {
        LOGE("Failed to find DetectedObject class");
        return nullptr;
    }
    dobj_jcstr = env->GetMethodID(
            dobj_jcls, "<init>", "(Ljava/lang/String;Landroid/graphics/Rect;F)V");
    if (dobj_jcstr == nullptr) {
        LOGE("Failed to find DetectedObject constructor");
        return nullptr;
    }
    rect_jcls = env->FindClass("android/graphics/Rect");
    if (rect_jcls == nullptr) {
        LOGE("Failed to find Rect class");
        return nullptr;
    }
    rect_jcstr = env->GetMethodID(rect_jcls, "<init>", "(IIII)V");
    if (rect_jcstr == nullptr) {
        LOGE("Failed to find Rect constructor");
        return nullptr;
    }
    // 开始修正
    const size_t objNum = objects.size();
    jobjectArray dobjects = env->NewObjectArray(
            objNum, dobj_jcls, nullptr);
    for (int i = 0; i < objNum; i++){
        auto& obj = objects[i];
        double realCenterX = obj->floats[0] - wpadP2,
               realCenterY = obj->floats[1] - hpadP2,
               widthP2 = obj->floats[2] / 2,
               heightP2 = obj->floats[3] / 2;
        double x1 = realCenterX - widthP2,
               x2 = realCenterX + widthP2,
               y1 = realCenterY - heightP2,
               y2 = realCenterY + heightP2;
        // 获取Java Rect对象
        jobject rect_jobj = env->NewObject(
                rect_jcls,
                rect_jcstr,
                (int)round(x1 * ratio),
                (int)round(y1 * ratio),
                (int)round(x2 * ratio),
                (int)round(y2 * ratio));
        // 获取Java DetectedObject对象
        jobject dobj_jobj = env->NewObject(
                dobj_jcls,
                dobj_jcstr,
                env->NewStringUTF(CLASS_NAMES[obj->classIndex]),
                rect_jobj,
                obj->confidence);
        env->SetObjectArrayElement(dobjects, i, dobj_jobj);
    }
    return dobjects;
}

struct Recognizable {
    jbyte* imageBuffer;
    jint width;
    jint height;

    Recognizable(JNIEnv* env, jobject recognizable_java) {
        jclass cls = env->GetObjectClass(recognizable_java);
        // 获取imageBuffer
        imageBuffer = (jbyte*)env->GetDirectBufferAddress(env->GetObjectField(
                recognizable_java,
                env->GetFieldID(cls, "imageBuffer", "Ljava/nio/ByteBuffer;")));
        // 获取width
        width = env->GetIntField(
                recognizable_java,
                env->GetFieldID(cls, "width", "I"));
        // 获取height
        height = env->GetIntField(
                recognizable_java,
                env->GetFieldID(cls, "height", "I"));
    }
};

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_detect(
        JNIEnv *env, jobject thiz,
        jobject recognizable_java,
        jfloat confidence) {
    if (pModel == nullptr) {
        LOGE("Model not initialized");
        return nullptr;
    }
    auto recognizable = Recognizable(env, recognizable_java);
    LOGI("Input image: %d x %d", recognizable.width, recognizable.height);
    if (recognizable.imageBuffer == nullptr) {
        LOGE("Failed to get image data");
        return nullptr;
    }

    // 图像拉伸
    ncnn::Mat in_mat;
    pair<double, double> whP2Pair = image_normalizing(
            reinterpret_cast<unsigned char *>(recognizable.imageBuffer),
            recognizable.width, recognizable.height, in_mat);
    // ncnn::Mat::shape() always fails.
    double wpadP2 = whP2Pair.first, hpadP2 = whP2Pair.second;

    // 推理并识别
    vector<unique_ptr<Object>> objects = infer_and_recognize(in_mat, confidence);

    // 转换坐标和名称
    double ratio;
    if (wpadP2 == 0)
        ratio = recognizable.width;
    else
        ratio = recognizable.height;
    ratio /= INPUT_SIZE;
    jobjectArray dobjects = correction(env, objects, wpadP2, hpadP2, ratio);
    LOGI("done with detected objects correction, ratio %lf", ratio);
    return dobjects;
}

extern "C" JNIEXPORT void JNICALL
Java_com_hika_accessibility_recognition_means_object_1detection_NCNNDetector_release(
        JNIEnv *env,
        jobject thiz) {
    if (pModel != nullptr) {
        delete pModel;
        pModel = nullptr;
    }
}
