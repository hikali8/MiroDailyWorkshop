package com.hika.core.aidl.accessibility

import android.os.Parcelable


// Simplified version of ContactMethod: reply
class IReplyMethod<T: Parcelable>(val method: (Boolean, Array<T>) -> Unit): (Boolean, Array<T>) -> Unit{

    constructor(method: (Boolean) -> Unit): this({ b, _ -> method(b) })

    // lambda version invoker, reason why we simplify this aidl interface
    override operator fun invoke(isSure: Boolean, parcelables: Array<T>)
        = method(isSure, parcelables)

    // overloads enumeration
    inline operator fun <reified R> invoke(isSure: Boolean)
        = method(isSure, emptyArray<R>() as Array<T>)
}