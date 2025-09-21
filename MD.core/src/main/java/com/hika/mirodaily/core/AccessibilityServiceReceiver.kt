package com.hika.mirodaily.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

import com.hika.core.aidl.accessibility.IAccessibilityService
import com.hika.core.aidl.accessibility.IASReceiver
import com.hika.core.aidl.accessibility.ITextReply
import com.hika.core.aidl.accessibility.IReply
import com.hika.core.aidl.accessibility.ParcelableText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

var iAccessibilityService: IAccessibilityService? = null
    private set

// Accessibility Service Receiver, to receive the binding of an accessibility service which cannot be bound
class ASReceiver: Service(){
    // 1. Expose the interface received by this
    companion object{

        fun click(x: Float, y: Float, duration: Long = 100, startTime: Long = 0)
            = iAccessibilityService?.click(PointF(x, y), startTime, duration)

        fun swipe(pointFrom: PointF, pointTo: PointF, duration: Long = 100, startTime: Long = 0)
            = iAccessibilityService?.swipe(pointFrom, pointTo, startTime, duration)

        fun clickLocationBox(locationBox: Rect, duration: Long = 50, startTime: Long = 0)
            = iAccessibilityService?.click(PointF(
                (locationBox.left..locationBox.right).random().toFloat(),
                (locationBox.top..locationBox.bottom).random().toFloat()
            ), startTime, duration)

        /**
         * send delegation to act as long as the className is met.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        suspend fun listenToActivityClassNameAsync(
            className: String,
            maximalMillis: Long = 10000
        )   = suspendCancellableCoroutine { continuation ->
                iAccessibilityService?.setListenerOnActivityClassName(className, maximalMillis,
                    object: IReply.Stub(){
                        override fun reply(isSure: Boolean){
                            continuation.resume(isSure, null)
                        }
                    }
                )
            }


        suspend fun getTextInRegionAsync(region: Rect? = null)
            = suspendCancellableCoroutine { continuation ->
                iAccessibilityService?.getTextInRegion(region,
                    object: ITextReply.Stub(){
                        override fun reply(parcelableText: ParcelableText) {
                            continuation.resume(parcelableText, null)
                        }
                    }
                )
            }

        // 2. Get a scope in this service to run codes in this service
        var coroutineScope: CoroutineScope? = null
            private set

        fun executeCode(code: suspend (CoroutineScope) -> Unit)
            = coroutineScope?.launch { code(this) }
    }

    override fun onCreate() {
        super.onCreate()
        //expose instance
        coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    override fun onDestroy() {
        coroutineScope?.cancel()
        super.onDestroy()
    }


    // 3. Return a response when receiving a binding intent from our accessibility service
    override fun onBind(intent: Intent?): IBinder? {
        if (iAccessibilityService != null){
            Log.e("#0x-AR", "Somehow more than one client wants to connect.")
            return null
        }
        return object : IASReceiver.Stub() {
            override fun onASConnected(_iAccessibilityService: IAccessibilityService){
                Log.d("#0x-AR", "Connected by accesssibility: ${_iAccessibilityService}")
                iAccessibilityService = _iAccessibilityService
            }
        }
    }


    // 4. Notice if the client is unbound
    override fun onUnbind(intent: Intent?): Boolean {
        if (iAccessibilityService != null){
            iAccessibilityService = null
        }
        return super.onUnbind(intent)
    }

    // 5. Promote this to foreground to run the main logic
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteThisToForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun promoteThisToForeground() {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("服务运行中: Kiro, is Kihoro")
            .setContentText("Miro Daily UI Service is running your automation flow...")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) // 必须设置有效图标
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                // Android 10+ needs to specify the foreground type
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
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
            channel.description = "无障碍服务接收器运行通知"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

}

// constants
const val AccessibilityPackageName = "com.hika.accessibility"
const val AccessibilityClassName = "com.hika.accessibility.AccessibilityCoreService"
const val ProjectionRequesterClassName = "com.hika.accessibility.ProjectionRequesterActivity"
const val START_BROADCAST = "com.hika.mirodaily.ui.ACTION_START"
private const val NOTIFICATION_ID = 53
private const val CHANNEL_ID = "MiroUI_Channel"