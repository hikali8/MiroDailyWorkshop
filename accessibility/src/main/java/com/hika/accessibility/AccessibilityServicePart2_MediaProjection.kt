package com.hika.accessibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.window.layout.WindowMetricsCalculator
import com.hika.accessibility.recognition.ImageHandler
import com.hika.core.aidl.accessibility.IProjectionSuccess


private const val NOTIFICATION_ID = 52
private const val CHANNEL_ID = "hikaAccessibility_Channel"

abstract class AccessibilityServicePart2_Projection: AccessibilityServicePart1_ConnectToMainProc() {
    // 2. Start Media Projection
    //     media projection has to be requested in an activity, and started in a service. to start
    //       it, we have to promote this service to foreground after activity requests it.
    fun startProjection(resultCode: Int, resultData: Intent) {
        promoteThisToForeground()
        getProjectionToken(resultCode, resultData)
        if (projectionToken == null) {
            return
        }
        getDisplayAndImage()
    }

    // 2.1 Promote this to foreground
    private fun promoteThisToForeground() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("服务运行中: Mihoro, is Miro")
            .setContentText("无障碍服务正在监控屏幕")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 必须设置有效图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                // Android 10+ needs to specify the foreground type
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "模拟服务通道",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "无障碍服务运行通知"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    // 2.2 Get Media Projection token
    var projectionToken: MediaProjection? = null
        private set

    private fun getProjectionToken(resultCode: Int, resultData: Intent){
        if (projectionToken != null){
            return
        }

        projectionToken = getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(resultCode, resultData)
        if (projectionToken == null){
            Log.e("#0x-AS", "媒体投影令牌获取失败")
            return
        }
        Log.i("#0x-AS", "媒体投影已启动")
        // register media projection callbacks
        projectionToken!!.registerCallback(object : MediaProjection.Callback(){
            override fun onStop() {
                Log.w("#0x-AS", "媒体投影已停止")
                projectionToken = null
            }

            override fun onCapturedContentResize(_width: Int, _height: Int) {
                width = _width
                height = _height
                super.onCapturedContentResize(_width, _height)
            }
        }, null)
    }

    // 2.3 Get Virtual Display and Image Handler
    private var virtualDisplay: VirtualDisplay? = null
    var imageHandler: ImageHandler? = null
        private set
    var width = 0
    var height = 0

    private fun getDisplayAndImage(){
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
        width = metrics.bounds.width()
        height = metrics.bounds.height()

        imageHandler = ImageHandler(width, height, coroutineScope, this)

        virtualDisplay = projectionToken!!.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            (metrics.density * DisplayMetrics.DENSITY_DEFAULT).toInt(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageHandler!!.surface,
            VirtualDisplayCallback()
            , null)
    }

    inner class VirtualDisplayCallback: VirtualDisplay.Callback(){
        override fun onStopped() {
            // Media Projection stopped
            virtualDisplay?.release()
            projectionToken?.stop()
            imageHandler?.release()

            virtualDisplay = null
            projectionToken = null
            imageHandler = null
            super.onStopped()
        }
    }


    // 2.4 Interface Exposure And clean-ups
    abstract inner class IAccessibilityExposed_Part2: IAccessibilityExposed_Part1(){
        override fun isProjectionStarted() = projectionToken != null

        override fun stopProjection(){
            virtualDisplay?.release()
            projectionToken?.stop()
            imageHandler?.release()

            virtualDisplay = null
            projectionToken = null
            imageHandler = null

        }

        override fun setListenerOnProjectionSuccess(_iProjectionSuccess: IProjectionSuccess) {
            iProjectionSuccess = _iProjectionSuccess
        }

        override fun getScreenSize() = Point(width, height)
    }

    var iProjectionSuccess: IProjectionSuccess? = null
    
    // 2.5 clean-ups
    override fun onVisitorDisconnected() {
        iAccessibilityExposed.stopProjection()
        super.onVisitorDisconnected()
    }
}