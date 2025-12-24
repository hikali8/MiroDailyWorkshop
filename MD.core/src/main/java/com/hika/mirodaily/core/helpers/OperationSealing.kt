package com.hika.mirodaily.core.helpers

import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.loopUntil
import com.hika.mirodaily.core.ASReceiver
import kotlinx.coroutines.delay

suspend fun destructivelyClickOnceAppearsText(keyword: String, durationMillis: Long = 1000, afterClickMillis: Long = 3000): Int {
    var location: List<ParcelableSymbol>? = null
    var text: ParcelableText? = null
    if (loopUntil(durationMillis) {
            text = ASReceiver.Companion.getTextInRegion()
            location = text!!.matchSequence(keyword)
            location?.isEmpty() != true
        }){
        for (i in 1..afterClickMillis/200) {
            ASReceiver.Companion.clickLocationBox(location!!.first().boundingBox!!)
            delay(200)
            text = ASReceiver.Companion.getTextInRegion()
            location = text!!.matchSequence(keyword)
            if (location?.isNotEmpty() != true) // 如果已不在，那么
                return i.toInt()
        }
        return -1
    }
    return 0
}