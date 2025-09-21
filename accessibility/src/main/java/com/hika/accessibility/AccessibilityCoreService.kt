package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hika.core.aidl.accessibility.IProjectionSuccess
import java.lang.ref.WeakReference

// 前台通知 & 保活
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock

class AccessibilityCoreService : AccessibilityServicePart4_ScreenWatching() {

    companion object{
        private var instance: WeakReference<AccessibilityCoreService>? = null
        fun getInstance() = instance?.get()
    }

    // --- 保活相关 ---
    private lateinit var wake: PowerManager.WakeLock
    @Volatile private var running = false
    private lateinit var worker: Thread

    // NEW ↓↓↓ 暂停/恢复开关（给悬浮窗调用）
    @Volatile private var paused = false
    fun togglePaused() { paused = !paused }
    fun isPaused() = paused
    // NEW ↑↑↑

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        Log.d("#0x-AS", "onConnected is finished.")

        // 前台服务 + 唤醒锁 + 心跳
        startForeground(1, buildFgNotification())

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mirodaily:wl")
        wake.acquire()

        running = true
        worker = Thread {
            val tick = 200L // 5Hz
            var next = SystemClock.elapsedRealtime()
            while (running) {
                // NEW ↓↓↓ 暂停时不跑流水线
                if (!paused) {
                    // TODO: 这里接入你的 pipelineStep()：抓屏→识别→手势
                    Log.d("AccessibilityCoreService", "tick@${SystemClock.elapsedRealtime()}")
                }
                // NEW ↑↑↑

                next += tick
                val sleep = next - SystemClock.elapsedRealtime()
                if (sleep > 0) {
                    try { Thread.sleep(sleep) } catch (_: InterruptedException) {}
                } else {
                    next = SystemClock.elapsedRealtime()
                }
            }
        }
        worker.start()
    }

    // ------- 你已有的对外接口 -------
    override val iAccessibilityExposed by lazy { IAccessibilityExposed() }
    inner class IAccessibilityExposed: IAccessibilityExposed_Part4(){
        override fun click(point: PointF, startTime: Long, duration: Long)
                = this@AccessibilityCoreService.click(point, startTime, duration)
        override fun swipe(pointFrom: PointF, pointTo: PointF, startTime: Long, duration: Long)
                = this@AccessibilityCoreService.swipe(pointFrom, pointTo, startTime, duration)
        override fun performAction(action: Int) { performGlobalAction(action) }
    }

    fun click(point: PointF, startTime: Long, duration: Long){
        val path = Path().apply { moveTo(point.x, point.y) }
        dispatchGestureEdited(path, startTime, duration)
    }
    fun swipe(pointFrom: PointF, pointTo: PointF, startTime: Long = 0, duration: Long = 100){
        val path = Path().apply { moveTo(pointFrom.x, pointFrom.y); lineTo(pointTo.x, pointTo.y) }
        dispatchGestureEdited(path, startTime, duration)
    }

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private fun dispatchGestureEdited(path: Path, startTime: Long, duration: Long){
        val gd = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, startTime, duration))
            .build()
        mainThreadHandler.post { dispatchGesture(gd, null, null) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        onVisitorDisconnected()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        onVisitorDisconnected()
        instance = null
        // 释放
        running = false
        if (::worker.isInitialized) worker.interrupt()
        if (::wake.isInitialized && wake.isHeld) wake.release()
        super.onDestroy()
    }

    override fun onInterrupt() { /* no-op */ }

    // 前台通知
    private fun buildFgNotification(): Notification {
        val chId = "autotest"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            if (nm.getNotificationChannel(chId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(chId, "AutoTest", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, chId)
                .setContentTitle("AutoTest running")
                .setContentText("Foreground service active")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("AutoTest running")
                .setContentText("Foreground service active")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .build()
        }
    }
}
