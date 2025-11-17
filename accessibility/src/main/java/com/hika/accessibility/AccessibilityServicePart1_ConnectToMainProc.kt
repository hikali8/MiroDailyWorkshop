package com.hika.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.hika.core.aidl.accessibility.IASReceiver
import com.hika.core.aidl.accessibility.IAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


private const val MainProgramPackageName = "com.hika.mirodaily.ui"
private const val MainProgramReceiverClassName = "com.hika.mirodaily.core.ASReceiver"
private const val START_BROADCAST = "com.hika.mirodaily.ui.ACTION_START"


abstract class AccessibilityServicePart1_ConnectToMainProc: AccessibilityService() {
    // 1. Connect to the Main Program
    //     :proceeded when it's connected by system or received a broadcast START_ACCESSIBILITY from the main proc
    var iConnector: IASReceiver? = null     // interface to the main-prog's connector. null if disconnected.
        private set

    // 1.1. Try to Connect to the Main Program When it's Connected by the System
    override fun onServiceConnected() {
        super.onServiceConnected()
        connectToMainProc()
        // Meanwhile register broadcast
        registerBroadcastReceiver()
    }

    private fun connectToMainProc(){
        if (iConnector != null){
            Log.d("#0x-AS", "There's already an iConnector")
            return
        }

        val intent = Intent()
        intent.setClassName(MainProgramPackageName, MainProgramReceiverClassName)
        val ret = this.bindService(intent, MainProgramConnection(), BIND_AUTO_CREATE)
        Log.d("#0x-AS", "Tried to bind AccessibilityConnector with: $ret, intent is: $intent")
    }

    inner class MainProgramConnection: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            iConnector = IASReceiver.Stub.asInterface(service)
            iConnector!!.onASConnected(iAccessibilityExposed)
            onMainProgramConnected()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            onMainProgramDisconnected()
            iConnector = null
        }
    }

    // 1.2. Receive Broadcast and Spontaneously connect to the Main Program
    private fun registerBroadcastReceiver(){
        ContextCompat.registerReceiver(this, connectionReceiver,
            IntentFilter(START_BROADCAST),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("#0x-AS", "Received Broadcast")

            // 1.2.1. Verify the action
            if (intent.action != START_BROADCAST) {
                Log.w("#0x-AS", "收到无效广播: ${intent.action}")
                return
            }

            // 1.2.2. Verify the timestamp
            val timestamp = intent.getLongExtra("timestamp", 0)
            if (System.currentTimeMillis() - timestamp > 5000) { // 5秒内有效
                Log.w("#0x-AS", "广播已过期")
                return
            }

            // 1.2.3. TODO: Verify the version
            /*
            val version = intent.getIntExtra("version", 0)
            if (version != BuildConfig.VERSION_CODE) {
                Log.w("Accessibility_Core", "版本不匹配: $version vs ${BuildConfig.VERSION_CODE}")
                return
            }
            */
            connectToMainProc()
        }
    }

    // 1.3. Inheritancial Implements.
    // 1.3.1. Main Program Connection State Interface Exposure
    open fun onMainProgramConnected(){}
    open fun onMainProgramDisconnected(){}

    // 1.3.2. Expose the Accessibility-Service's Interface to main program.
    abstract val iAccessibilityExposed: IAccessibilityService.Stub
    abstract inner class IAccessibilityExposed_Part1: IAccessibilityService.Stub()


    // 1.4 subsequence needed
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    // 1.4 clean-ups
    override fun onDestroy() {
        unregisterReceiver(connectionReceiver)
        super.onDestroy()
    }
}