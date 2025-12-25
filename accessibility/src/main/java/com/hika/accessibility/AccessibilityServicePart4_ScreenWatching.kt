package com.hika.accessibility

import android.content.Intent
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.text.Text
import com.hika.core.aidl.accessibility.DetectedObject
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.aidl.accessibility.ParcelableTextBase
import com.hika.core.getRotation
import com.hika.core.rotateRectFrom
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
            region: Rect?,
            confidence: Float
        ): Array<DetectedObject> {
            val deferred = coroutineScope.async {
                imageHandler?.getRecognizable()?.findOnNCNNDetector(detectorName, region, confidence)
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
            val deferred = coroutineScope.async {
                val rotation = getRotation(this@AccessibilityServicePart4_ScreenWatching)
                val screen = screenSize
                ParcelableTextBase.setRectRotation(
                    rotation,
                    screen.x, screen.y
                )
                imageHandler?.getRecognizable()?.findOnGoogleOCRerInRangeAsync(region?.run {
                    rotateRectFrom(this, zeroW, zeroH, rotation)
                })
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