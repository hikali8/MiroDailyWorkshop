package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * This will be running forever, just because some system disables all stopped applications'
 *  accessibility settings.
 */
class AccessibilityCoreService() : AccessibilityServicePart5_ScriptReplay() {
    // 5. Service Core: Instance and Click
    companion object {
        var instance = WeakReference<AccessibilityCoreService>(null)
    }

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
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
        Log.i("#0x-AS", "点击：$point")
        val gesturePath = Path()
        gesturePath.moveTo(point.x, point.y)
        gesturePath.lineTo(point.x, point.y)
        dispatchGestureEdited(gesturePath, startTime, duration)
    }

    fun swipe(pointFrom: PointF, pointTo: PointF, startTime: Long = 0, duration: Long = 100){
        Log.i("#0x-AS", "滑动：pointFrom $pointFrom pointTo $pointTo")
        val gesturePath = Path()
        gesturePath.moveTo(pointFrom.x, pointFrom.y)
        gesturePath.lineTo(pointTo.x, pointTo.y)
        dispatchGestureEdited(gesturePath, startTime, duration)
    }

    private fun dispatchGestureEdited(gesturePath: Path, startTime: Long, duration: Long){
        val gestureDescription = GestureDescription.Builder().apply {
            addStroke(GestureDescription.StrokeDescription(
                gesturePath, startTime, duration
            ))
        }.build()

        coroutineScope.launch {
            dispatchGesture(gestureDescription, null, null)
        }
    }


    // 5.2. Clean-Ups

    // on System Disconnected (as a accessibility service)
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        onMainProgramDisconnected()
        instance.clear()
        super.onDestroy()
    }

    // ignore
    override fun onInterrupt() {}
}