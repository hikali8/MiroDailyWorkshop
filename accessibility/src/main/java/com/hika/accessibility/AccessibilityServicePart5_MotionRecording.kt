package com.hika.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hika.core.aidl.accessibility.ParcelableMotion
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

abstract class AccessibilityServicePart5_MotionRecording: AccessibilityServicePart4_ScreenWatching() {
    // 5. Tap / Gesture / Motion Capture
    val recordedEvents = mutableListOf<ParcelableMotion>()

    // 5.1. Accessibility Event Capturing
    private var canceler: CancellableContinuation<Array<ParcelableMotion>>? = null
    var recordingJob: Deferred<Array<ParcelableMotion>>? = null

    // 配置要监听的事件类型
    private val eventTypesToRecord = setOf(
        AccessibilityEvent.TYPE_VIEW_CLICKED,
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
        AccessibilityEvent.TYPE_VIEW_SCROLLED
    )

    abstract inner class IAccessibilityExposed_Part5: IAccessibilityExposed_Part4(){
        override fun recordMotions(): Array<ParcelableMotion> {
            if (recordingJob == null)
                recordingJob = coroutineScope.async {
                    suspendCancellableCoroutine { continuation ->
                        canceler = continuation
                        addPhysoBtnOnEvent()
                        addScreenOpOnEvent()
                        notify("希卡正在录制用户操作....按音量上键和电源键停止录制")
                    }
                }
            return runBlocking {
                val result = recordingJob!!.await()
                recordingJob = null
                result
            }
        }

        override fun stopMotionRecording() {
            clearPhysoBtnOnEvent()
            canceler?.apply {
                resume(recordedEvents.toTypedArray(), null)
                Log.d("#0x-AS5", "已停止操作录制，共录制 ${recordedEvents.size} 个事件")
            }
            recordedEvents.clear()
        }
    }

    // 配置服务以监听可访问性事件
    private fun addScreenOpOnEvent(){
        serviceInfo.eventTypes = serviceInfo.eventTypes or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
        serviceInfo.flags = serviceInfo.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
    }

    private fun clearScreenOpOnEvent(){
        serviceInfo.eventTypes = serviceInfo.eventTypes and
                (AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED).inv()
        serviceInfo.flags = serviceInfo.flags and
                (AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS).inv()
    }


    // 重写 onAccessibilityEvent 来捕获用户操作
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (recordingJob == null) return

        // 只记录我们关心的事件类型
        if (eventTypesToRecord.contains(event.eventType)) {
            Log.w("#0x-AS5", "接收到可访问性事件: ${AccessibilityEvent.eventTypeToString(event.eventType)}")

            // 创建基于可访问性事件的 ParcelableMotion
            val motion = createMotionFromAccessibilityEvent(event)
            recordedEvents.add(motion)
        }
    }

    // 从 AccessibilityEvent 创建 ParcelableMotion
    private fun createMotionFromAccessibilityEvent(event: AccessibilityEvent): ParcelableMotion {
        // 获取事件的基本信息
        val action = event.eventType
        val eventTime = event.eventTime

        // 尝试获取坐标信息
        var x = 0f
        var y = 0f

        // 从事件源获取坐标（如果可用）
        if (event.source != null) {
            val bounds = android.graphics.Rect()
            event.source?.getBoundsInScreen(bounds)
            x = bounds.exactCenterX()
            y = bounds.exactCenterY()

            // 记录额外的信息用于调试
            Log.d("#0x-AS5", "事件源: ${event.source?.className}, 坐标: ($x, $y)")
        }

//         如果没有坐标信息，使用时间戳生成伪坐标（用于区分不同时间的事件）
        if (x == 0f && y == 0f) {
            x = event.scrollX.toFloat()
            y = event.scrollY.toFloat()
        }

        return ParcelableMotion(action, eventTime, x, y)
    }

    // 5.2. Hotkey ceasing (v+ and power, active)
    fun addPhysoBtnOnEvent(){
        serviceInfo.flags = (serviceInfo.flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
        serviceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        setServiceInfo(serviceInfo)
        Log.d("#0x-AS5", "已启用按键监听")
    }

    fun clearPhysoBtnOnEvent(){
        serviceInfo.flags = (serviceInfo.flags and
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv())
        serviceInfo.eventTypes = 0
        setServiceInfo(serviceInfo)
        Log.d("#0x-AS5", "已禁用按键监听")
    }

    var isVUPressing = false
    var isPWPressing = false
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if(event == null)
            return false

        Log.e("#0x-AS5", "按键事件: ${event.keyCode}, action=${event.action}")

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                isVUPressing = event.action == KeyEvent.ACTION_DOWN
            }
            KeyEvent.KEYCODE_POWER -> {
                isPWPressing = event.action == KeyEvent.ACTION_DOWN
            }
            else -> return false
        }

        Log.d("#0x-AS5", "音量上键: $isVUPressing, 电源键: $isPWPressing")

        if (isVUPressing && isPWPressing){
            iAccessibilityExposed.stopMotionRecording()
            return true
        }
        return false
    }

    // 5.x Ultimate clean-ups
    override fun onMainProgramDisconnected(){
        iAccessibilityExposed.stopMotionRecording()
        super.onMainProgramDisconnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        iAccessibilityExposed.stopMotionRecording()
        return super.onUnbind(intent)
    }
}