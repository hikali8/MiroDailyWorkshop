package com.hika.mirodaily.core.helpers

import android.graphics.Rect
import android.graphics.Region
import android.util.Log
import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.loopUntil
import com.hika.mirodaily.core.ASReceiver
import kotlinx.coroutines.delay

// 一旦某文本出现，点击直至其消失
suspend fun destructivelyClickOnceAppearsText(
    keyword: String, durationMillis: Long = 1000,
    afterClickMillis: Long = 3000,
    region: Rect? = null): Int {
    var location: List<ParcelableSymbol>? = null
    var text: ParcelableText?
    if (loopUntil(durationMillis) {
            text = ASReceiver.getTextInRegion(region)
//            Log.i("#0x-OS", "captured: ${text?.text}")
            location = text?.matchSequence(keyword)
            location?.isNotEmpty() == true
        }){
        for (i in 1..afterClickMillis/200) {
            val middle = location.run {
                this!![(this.size - 1) / 2]
            }.boundingBox!!
            ASReceiver.clickLocationBox(middle)
            delay(200)
            text = ASReceiver.getTextInRegion(region)
            location = text?.matchSequence(keyword)
            if (location?.isNotEmpty() != true) // 如果已不在，那么
                return i.toInt()
        }
        return -1
    }
    return 0
}


