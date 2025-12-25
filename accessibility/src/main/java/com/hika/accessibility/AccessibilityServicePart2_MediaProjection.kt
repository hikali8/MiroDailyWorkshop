package com.hika.accessibility

import android.app.Notification
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
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.window.layout.WindowMetricsCalculator
import com.hika.accessibility.recognition.ImageHandler
import com.hika.core.aidl.accessibility.IProjectionSuccess
import com.hika.core.getRotation
import com.hika.core.rotateWHto
import com.hika.core.toastLine


private const val NOTIFICATION_ID = 52
private const val CHANNEL_ID = "hikaAccessibilityChannel"

abstract class AccessibilityServicePart2_Projection: AccessibilityServicePart1_ConnectToMainProc() {
    // 2. Start Media Projection
    //     media projection has to be requested in an activity, and started in a service. to start
    //       it, we have to promote this service to foreground after activity requests the permission.
    fun startProjection(resultCode: Int, resultData: Intent) {
        promoteThisToForeground()
        getProjectionToken(resultCode, resultData)
        if (projectionToken == null)
            return
        getDisplayAndImage()
    }

    // 2.1. Promote this to foreground (a foreground service must have a notification)
    private fun promoteThisToForeground() {
        val notification = createNotification("希卡正在具有屏幕捕获权限，直到系统断开权限或主应用退出...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        else startForeground(NOTIFICATION_ID, notification)

        // Issue the notification.
        notify(notification)
    }

    // 2.1.1. Create Notification
    private fun createNotification(text: CharSequence, title: CharSequence = "希卡无障碍服务"): Notification
            = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // 必须设置有效图标
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        //            .setOngoing(true) // 设置为常驻通知，用户无法手动清除（但部分系统无效）
        .build()

    private fun notify(notification: Notification){
        NotificationManagerCompat.from(this).apply {
            if (areNotificationsEnabled()) {
                notify(NOTIFICATION_ID, notification)
                return
            }
            val toOpenSetting = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(toOpenSetting)
            toastLine("请启用通知权限", applicationContext, true)
            Log.e("0x-AS2", "Failed to issue a notification. You must enable the notification of Hica to show notifications")
        }
    }

    // expose the notification function to the child procedure
    fun notify(text: CharSequence, title: CharSequence = "希卡无障碍服务"){
        notify(createNotification(text, title))
    }

    // 2.1.2. Create Notification Channel
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel(){
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(
                CHANNEL_ID,
                "希卡无障碍通知渠道",
                NotificationManager.IMPORTANCE_DEFAULT
            ))
    }

    private fun deleteNotificationChannel(){
        getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(CHANNEL_ID)
    }

    // 2.2. Get Media Projection token
    private var projectionToken: MediaProjection? = null

    private fun getProjectionToken(resultCode: Int, resultData: Intent){
        if (projectionToken != null)
            return
        projectionToken = getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(resultCode, resultData)
        if (projectionToken == null){
            Log.e("#0x-AS", "媒体投影令牌获取失败")
            return
        }
        Log.i("#0x-AS2", "媒体投影已启动")
        // register media projection callbacks
        projectionToken!!.registerCallback(object : MediaProjection.Callback(){
            override fun onStop() {
                Log.i("#0x-AS2", "媒体投影已停止")
                projectionToken = null
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                rotateWHto(width, height, getRotation(this@AccessibilityServicePart2_Projection)).apply {
                    zeroW = x
                    zeroH = y
                }
                super.onCapturedContentResize(width, height)
                onResize()
            }
        }, null)
    }

    abstract fun onResize()

    // 2.3. Get Virtual Display and Image Handler
    private var virtualDisplay: VirtualDisplay? = null
    // exposure image to the child
    var imageHandler: ImageHandler? = null
        private set

    var zeroW = 0
        private set
    var zeroH = 0
        private set

    private fun getDisplayAndImage(){
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)
        zeroW = metrics.bounds.width()
        zeroH = metrics.bounds.height()
        rotateWHto(zeroW, zeroH, getRotation(this)).apply {
            zeroW = x
            zeroH = y
        }

        imageHandler = ImageHandler(zeroW, zeroH, coroutineScope, this)

        virtualDisplay = projectionToken!!.createVirtualDisplay(
            "ScreenCapture",
            zeroW,
            zeroH,
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


    // 2.4. Interface Exposure And clean-ups
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

        override fun getScreenSize(): Point {
            val rotation = getRotation(this@AccessibilityServicePart2_Projection)
            return rotateWHto(zeroW, zeroH, rotation)
        }
    }

    var iProjectionSuccess: IProjectionSuccess? = null

    // 2.5. clean-ups
    override fun onDestroy(){
        deleteNotificationChannel()
    }

    override fun onMainProgramDisconnected() {
        iAccessibilityExposed.stopProjection()
        notify("主应用已退出，已终止屏幕捕获")
        super.onMainProgramDisconnected()
    }
}