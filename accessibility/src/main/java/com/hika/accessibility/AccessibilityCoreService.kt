package com.hika.accessibility

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.hika.core.aidl.accessibility.DetectedObject
import com.hika.core.aidl.accessibility.IProjectionSuccess
import java.lang.ref.WeakReference

/**
 * This will be running forever, just because some system disables all stopped applications'
 *  accessibility settings.
 */
class AccessibilityCoreService() : AccessibilityServicePart4_ScreenWatching() {
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

    inner class IAccessibilityExposed: IAccessibilityExposed_Part4(){

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

    // 5.3. Tap / Gesture Capture

    /**
     * When to record
     * 1. User clicks "Record"
     * 2. Motion Capturing started
     * 3. On every motion captured, record motion and the timestamp, save into memory (value)
     * 4. User presses volume + and power
     * 5. Stop motion capturing. Correct the time to starting from zero.
     * 6. Save to file (csv).
     */

    /**
     * When to replay
     * 1. User clicks "Start".
     * 2. Open the app.
     * 3. Read the file.
     * 4. While the initial condition satisfied, replay the file.
     * 5. Stop if the file is over, or if the volume + and power is pressed.
     * 6. Print the log.
     */



    var isToCaptureTap = true
    override fun onMotionEvent(event: MotionEvent) {
//        if (isToCaptureTap){
//            when(event.action){
//                MotionEvent.ACTION_DOWN ->
//                    ;
//                MotionEvent.ACTION_UP ->
//                    ;
//            }
//
//        }
        super.onMotionEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        return super.onKeyEvent(event)
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