package com.hika.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.TouchInteractionController
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import com.hika.core.aidl.accessibility.ParcelableMotion
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

abstract class AccessibilityServicePart5_MotionRecording: AccessibilityServicePart4_ScreenWatching() {
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

    val recordedMotions = mutableListOf<ParcelableMotion>()

    // 1. Motion Capturing & Resending (Passive ceasing)
    private val touchController by lazy { getTouchInteractionController(Display.DEFAULT_DISPLAY) }
    private var canceler: CancellableContinuation<Array<ParcelableMotion>>? = null

    abstract inner class IAccessibilityExposed_Part5: IAccessibilityExposed_Part4(){
        override fun recordMotions(): Array<ParcelableMotion> {
            return runBlocking { suspendCancellableCoroutine { continuation ->
                canceler = continuation
                addPhysoBtnOnEvent()
                touchController.registerCallback(null, touchCallback)
            } }
        }

        override fun stopMotionRecording() {
            touchController.unregisterCallback(touchCallback)
            clearPhysoBtnOnEvent()
            canceler?.resume(recordedMotions.toTypedArray(), null)
            recordedMotions.clear()
        }
    }

    val touchCallback = object : TouchInteractionController.Callback {
        override fun onMotionEvent(event: MotionEvent) {
            recordedMotions.add(ParcelableMotion(event.action, event.eventTime, event.rawX, event.rawY))
        }

        override fun onStateChanged(state: Int) { }
    }

    // 2. Hotkey ceasing (v+ and power, active)
    fun addPhysoBtnOnEvent(){
        serviceInfo.flags = (serviceInfo.flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
    }

    fun clearPhysoBtnOnEvent(){
        serviceInfo.flags = (serviceInfo.flags and
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv())
    }

    var isVUPressing = false
    var isPWPressing = false
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if(event == null)
            return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                isVUPressing = event.action == KeyEvent.ACTION_DOWN
            }
            KeyEvent.KEYCODE_POWER -> {
                isPWPressing = event.action == KeyEvent.ACTION_DOWN
            }
            else -> return false
        }
        if (isVUPressing && isPWPressing){
            iAccessibilityExposed.stopMotionRecording()
            return true
        }
        return false
    }

    // 5.x Ultimate clean-ups
    override fun onVisitorDisconnected(){
        clearPhysoBtnOnEvent()
        super.onVisitorDisconnected()
    }
}