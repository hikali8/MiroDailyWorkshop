package com.hika.accessibility.recognition.means.object_detection

import android.content.Context
import android.content.res.AssetManager
import com.hika.accessibility.recognition.ImageHandler.Recognizable
import com.hika.core.aidl.accessibility.DetectedObject


class NCNNDetector(context: Context) {
    init {
        System.loadLibrary("ncnn_detector");
        init(context.assets)
    }

    external fun init(assetManager: AssetManager?): Boolean // 初始化模型
    external fun detect(recognizable: Recognizable): Array<DetectedObject>  // 检测图像
    external fun release()  // 释放资源
}