package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference

/**
 * This will be running forever, just because some system disables all stopped applications'
 *  accessibility settings.
 */
class AccessibilityCoreService() : AccessibilityServicePart5_MotionRecording() {
    // 5. Service Core: Instance and Click
    companion object {
        var instance = WeakReference<AccessibilityCoreService>(null)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        Log.d("#0x-AS", "onConnected is finished.")
    }


    // 5.1. Expose this Accessibility-Service's Interface.
    override val iAccessibilityExposed by lazy { IAccessibilityExposed() }

    inner class IAccessibilityExposed: IAccessibilityExposed_Part5(){
        override fun click(point: PointF, startTime: Long, duration: Long)
            = this@AccessibilityCoreService.click(point, startTime, duration)

        override fun swipe(pointFrom: PointF, pointTo: PointF, startTime: Long, duration: Long)
            = this@AccessibilityCoreService.swipe(pointFrom, pointTo, startTime, duration)

        override fun performAction(action: Int) {
            performGlobalAction(action)
        }
    }

    // to set default arguments, we need to put them anywhere else
    fun click(point: PointF, startTime: Long, duration: Long){
        val gesturePath = Path().apply {
            moveTo(point.x, point.y)
        }
        dispatchGestureEdited(gesturePath, startTime, duration)
    }

    fun swipe(pointFrom: PointF, pointTo: PointF, startTime: Long = 0, duration: Long = 100){
        val gesturePath = Path().apply {
            moveTo(pointFrom.x, pointFrom.y)
            lineTo(pointTo.x, pointTo.y)
        }
        dispatchGestureEdited(gesturePath, startTime, duration)
    }

    /** Handler on the main thread. */
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    private fun dispatchGestureEdited(gesturePath: Path, startTime: Long, duration: Long){
        val gestureDescription = GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(
                gesturePath, startTime, duration
            ))
        }.build()

        mainThreadHandler.post {
            dispatchGesture(gestureDescription, null, null)
        }
    }


    // 5.2. Clean-Ups

    // on Master Disconnected
    override fun onUnbind(intent: Intent?): Boolean {
        onVisitorDisconnected()
        return super.onUnbind(intent)
    }

    // on Ego Disconnected
    override fun onDestroy() {
        onVisitorDisconnected()
        instance.clear()
        super.onDestroy()
    }

    // ignore
    override fun onInterrupt() {
        //TODO("Not yet implemented")
    }
}