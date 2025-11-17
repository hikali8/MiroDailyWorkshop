package com.hika.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.hika.core.aidl.accessibility.IReply


abstract class AccessibilityServicePart3_EventListening: AccessibilityServicePart2_Projection() {
    // 3. WindowChange Event Listening

    // Create delegation abstraction
    private data class ClassNameListener(private val onListened: (Boolean) -> Unit,
                                         val expirationTime: Long){
        var isRunning: Boolean = true
            private set

        fun onTrigger(isValid: Boolean){
            Log.d("#0x-AS3", "triggered with: $isValid")
            if (isRunning)
                onListened(isValid)
            isRunning = false
        }
    }

    private val classNameMap by lazy { HashMap<String, MutableList<ClassNameListener>>() }
    private var latestExpirationTime: Long = -1 // negative if there's nothing in map, typically -1

    // 3.1 Interfaces and implements
    abstract inner class IAccessibilityExposed_Part3: IAccessibilityExposed_Part2(){
        override fun setListenerOnActivityClassName(
            className: String,
            maximumMillis: Long,
            iReply: IReply
        ) {
            Log.d("#0x-AS3", "Received Class Name: $className")
            if (maximumMillis < 200){
                Log.e("#0x-AS3", "Error: the maximumMillis must be greater than 200 ms.")
                return
            }

            val value = classNameMap.getOrPut(className, ::mutableListOf)
            val expirationTime = System.currentTimeMillis() + maximumMillis
            value.add(ClassNameListener(iReply::reply, expirationTime))

            if (latestExpirationTime < expirationTime){
                latestExpirationTime = expirationTime
            }
            addWCOnEvent()
        }

        override fun clearClassNameListeners() {
            clearWCOnEvent()
            latestExpirationTime = -1
            classNameMap.clear()
        }
    }

    // 3.1 Listen only if necessary
    fun addWCOnEvent(){
        serviceInfo.eventTypes = (serviceInfo.eventTypes or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        serviceInfo.flags = (serviceInfo.flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)
    }

    fun clearWCOnEvent(){
        serviceInfo.eventTypes = (serviceInfo.eventTypes and
                (AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).inv())
        serviceInfo.flags = (serviceInfo.flags and
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS.inv())
    }

    // event listen
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // some system will mute all Log.d in accessibility onEvent function.
                if (latestExpirationTime < 0) {
                    clearWCOnEvent()
                    return
                }

                val currentTimeMillis = System.currentTimeMillis()
                if (iConnector == null || currentTimeMillis > latestExpirationTime){
                    iAccessibilityExposed.clearClassNameListeners()
                    return
                }

                val className = event.className?.toString()
                classNameMap[className]?.let { delegations ->
                    for (delegation in delegations)
                        delegation.onTrigger(
                            delegation.expirationTime >= currentTimeMillis)
                    delegations.clear()
                    classNameMap.remove(className)
                }
                if (classNameMap.isEmpty){
                    latestExpirationTime = -1
                    clearWCOnEvent()
                }

            }
        }
    }

    // 3.3 clean-ups
    override fun onMainProgramDisconnected(){
        iAccessibilityExposed.clearClassNameListeners()
        super.onMainProgramDisconnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        iAccessibilityExposed.clearClassNameListeners()
        return super.onUnbind(intent)
    }
}