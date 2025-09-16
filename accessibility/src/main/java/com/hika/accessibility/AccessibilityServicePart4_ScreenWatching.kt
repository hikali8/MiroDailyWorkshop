package com.hika.accessibility

import android.graphics.Rect
import com.hika.core.aidl.accessibility.ITextReply
import com.hika.core.aidl.accessibility.ParcelableText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


abstract class AccessibilityServicePart4_ScreenWatching: AccessibilityServicePart3_EventListening() {
    // 4. Image Watching

    // 4.1 Multi-Task manager
    val textJobs = mutableListOf<Job>()

    // 4.2 Interfaces and implements: Received Watching Delegation Dispatch

    abstract inner class IAccessibilityExposed_Part4: IAccessibilityExposed_Part3() {
        override fun getTextInRegion(range: Rect?, iText: ITextReply){
            // we have still to use coroutine, for the screen capturing is limited
            textJobs.add(coroutineScope.launch {
                val recognizable = imageHandler?.getRecognizable()
                val text = recognizable?.findOnGoogleOCRerInRangeAsync(range)
                iText.reply(ParcelableText(text))
            })
        }

        override fun cancelAllTextGetting() {
            textJobs.forEach { it.cancel() }
            textJobs.clear()
        }
    }

    // 4.4 Ultimate clean-ups
    override fun onVisitorDisconnected(){
        iAccessibilityExposed.cancelAllTextGetting()
        super.onVisitorDisconnected()
    }
}