package com.hika.accessibility.recognition.means.object_detection

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.hika.accessibility.recognition.ImageHandler.Recognizable
import com.hika.core.aidl.accessibility.DetectedObject


class NCNNDetector(context: Context) {
    init {
        System.loadLibrary("ncnn_detector");
        Log.d("#0x-NDkt", "AssetManager hashCode: " + System.identityHashCode(context.assets))

        if(!init(context.assets))
            throw(Exception("Error with ncnn initiation."))
    }

    external fun init(assetManager: AssetManager?): Boolean // 初始化模型
    external fun detect(recognizable: Recognizable, confidence: Float): Array<DetectedObject>  // 检测图像
    external fun release()  // 释放资源
}
