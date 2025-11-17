package com.hika.core.aidl.accessibility

import android.os.Parcelable
import android.view.MotionEvent
import kotlinx.parcelize.Parcelize

@Parcelize
class ParcelableMotion(val action: Int, val timestamp: Long, val x: Float, val y: Float): Parcelable{
    override fun toString() = "Motion: " + MotionEvent.actionToString(action) +
                              ", ${timestamp}, ${x}, ${y}. " +
                              super.toString()
}