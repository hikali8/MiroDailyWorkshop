package com.hika.core.aidl.accessibility

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ParcelableMotion(val action: Int, val timestamp: Long, val x: Float, val y: Float): Parcelable