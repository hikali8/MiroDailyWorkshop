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
import com.hika.core.aidl.accessibility.IReply
import com.hika.core.aidl.accessibility.ParcelableText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference

var iAccessibilityService: IAccessibilityService? = null
    private set

// Accessibility Service Receiver, to receive the binding of an accessibility service which cannot be bound
class ASReceiver: Service(){
    // 1. Expose the interface received by this
    companion object{
        var instance = WeakReference<ASReceiver>(null)

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
         * resume as long as the className is met or the maximalMillis has been met.
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


        fun getTextInRegion(region: Rect? = null)
            = iAccessibilityService?.getTextInRegion(region)
    }

    override fun onCreate() {
        super.onCreate()
        //expose instance
        instance = WeakReference(this)
    }


    // 3. Return a response when receiving a binding intent from our accessibility service
    override fun onBind(intent: Intent?): IBinder? {
        if (iAccessibilityService != null){
            Log.e("#0x-AR", "Somehow more than one client wants to connect.")
            return null
        }
        Log.w("#0x-AR", "Received a binding intent: $intent")
        return object : IASReceiver.Stub() {
            override fun onASConnected(_iAccessibilityService: IAccessibilityService){
                Log.d("#0x-AR", "Connected by accesssibility: ${_iAccessibilityService}")
                iAccessibilityService = _iAccessibilityService
            }
        }
    }


    // 4. Notice if the client is unbound by the accessibility service
    override fun onUnbind(intent: Intent?): Boolean {
        if (iAccessibilityService != null){
            iAccessibilityService = null
        }
        return super.onUnbind(intent)
    }

    // 5. clean up
    override fun onDestroy() {
        iAccessibilityService?.stopConnection()
        instance.clear()
        super.onDestroy()
    }
}

// constants
const val AccessibilityPackageName = "com.hika.accessibility"
const val AccessibilityClassName = "com.hika.accessibility.AccessibilityCoreService"
const val ProjectionRequesterClassName = "com.hika.accessibility.ProjectionRequesterActivity"
const val START_BROADCAST = "com.hika.mirodaily.ui.ACTION_START"