package com.hika.accessibility

import android.content.Intent
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import com.hika.core.aidl.accessibility.DetectedObject
import com.hika.core.aidl.accessibility.ParcelableText
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException


abstract class AccessibilityServicePart4_ScreenWatching: AccessibilityServicePart3_EventListening() {
    // 4. Image Watching

    // 4.1 Multi-Task manager
    val detectJobs = mutableListOf<Deferred<Array<DetectedObject>?>>()
    val textJobs = mutableListOf<Deferred<Text?>>()

    // 4.2 Interfaces and implements: Received Watching Delegation Dispatch

    abstract inner class IAccessibilityExposed_Part4: IAccessibilityExposed_Part3() {
        override fun getObjectInRegion(
            detectorName: String,
            region: Rect?
        ): Array<DetectedObject> {
            val deferred = coroutineScope.async {
//                imageHandler?.getRecognizable()?.findOnNCNNDetector(detectorName, region)
                // 现在debug资产图像。
                imageHandler?.debugUsage()

                null
            }
            detectJobs.add(deferred)
            return runBlocking {
                deferred.await() ?: emptyArray()
            }
        }

        override fun cancelAllObjectGetting() {
            detectJobs.forEach { it.cancel() }
            detectJobs.clear()
        }

        override fun getTextInRegion(region: Rect?): ParcelableText {
            // we have still to use coroutine, as the screen capturing is limited
            val deferred = coroutineScope.async {
                imageHandler?.getRecognizable()?.findOnGoogleOCRerInRangeAsync(region)
            }
            textJobs.add(deferred)
            return runBlocking {
                try {
                    ParcelableText(deferred.await())
                }catch (_: CancellationException) {
                    // 如果协程被取消，返回空的结果
                    ParcelableText()
                }
            }
        }

        override fun cancelAllTextGetting() {
            textJobs.forEach { it.cancel() }
            textJobs.clear()
        }
    }

    // 4.4 Ultimate clean-ups
    override fun onMainProgramDisconnected(){
        iAccessibilityExposed.cancelAllTextGetting()
        super.onMainProgramDisconnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        iAccessibilityExposed.cancelAllTextGetting()
        return super.onUnbind(intent)
    }
}