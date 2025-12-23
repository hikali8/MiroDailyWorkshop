package com.hika.core.aidl.accessibility

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class DetectedObject(
    val objectName: String,
    val regionBox: Rect,
    val confidence: Float
) : Parcelable{
    override fun toString(): String {
        return "obj $objectName: confidence $confidence, location $regionBox"
    }
}
