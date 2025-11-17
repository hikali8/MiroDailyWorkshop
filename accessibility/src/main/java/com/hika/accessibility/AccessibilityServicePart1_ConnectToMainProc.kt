package com.hika.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
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
    // 1. Connect to the Main Process
    //     :proceeded when it's connected by system or received a broadcast START_ACCESSIBILITY from the main proc
    override fun onServiceConnected() {
        super.onServiceConnected()
        connectToMainProc()
        registerBroadcastReceiver()
    }

    // 1.1 Connect To the Main Process
    var iConnector: IASReceiver? = null     // interface of main-proc's connector. null if disconnected.
        private set

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

    // 1.2 Register Broadcast Receiver to connect spontaneously
    private fun registerBroadcastReceiver(){
        val filter = IntentFilter(START_BROADCAST)
        val registerFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RECEIVER_EXPORTED
        }else { 2 }
        ContextCompat.registerReceiver(this, connectionReceiver, filter, registerFlag)
    }

    val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("#0x-AS", "Received Broadcast")
            if (!isValidBroadcast(intent))
                return
            Log.d("#0x-AS", "After Verification, Connect to Proc now")
            connectToMainProc()
        }
    }

    private fun isValidBroadcast(intent: Intent): Boolean {
        // 1.2.1 Verify the action
        if (intent.action != START_BROADCAST) {
            Log.w("#0x-AS", "收到无效广播: ${intent.action}")
            return false
        }

        // 1.2.2 Verify the timestamp
        val timestamp = intent.getLongExtra("timestamp", 0)
        if (System.currentTimeMillis() - timestamp > 5000) { // 5秒内有效
            Log.w("#0x-AS", "广播已过期")
            return false
        }

        // 1.2.3 TODO: verify the version
        /*
        val version = intent.getIntExtra("version", 0)
        if (version != BuildConfig.VERSION_CODE) {
            Log.w("Accessibility_Core", "版本不匹配: $version vs ${BuildConfig.VERSION_CODE}")
            return false
        }
        */
        return true
    }

    private inner class VuPButtonReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                val keyEvent: KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    val keyCode: Int = keyEvent.getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        Log.d("VolumePowerReceiver", "Volume key pressed");
                    } else if (keyCode == KeyEvent.KEYCODE_POWER) {
                        Log.d("VolumePowerReceiver", "Power key pressed");
                    }
                }
            }
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