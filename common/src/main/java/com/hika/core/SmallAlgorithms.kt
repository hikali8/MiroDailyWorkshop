package com.hika.core

import android.view.Surface
import kotlin.text.isWhitespace


fun countSpaces(string: String, startFrom: Int, length: Int): Int{
    var count = 0
    val terminal = startFrom + length
    for (i in startFrom until terminal)
        if (string[i].isWhitespace())
            count++
    return count
}

fun <T> List<T>.selectUniformly(count: Int): List<T> {
    require(count > 0 && count <= size) { "Invalid count" }

    return List(count) { i ->
        val index = (i * (size - 1) / (count - 1.0)).toInt()
        this[index]
    }
}

fun rotateCoordinate(x: Float, y: Float, width: Int, height: Int, rotation: Int): Pair<Float, Float> {
    return when (rotation) {
        Surface.ROTATION_0 ->
            Pair(x, y)
        Surface.ROTATION_90 ->
            Pair(y, height - x)
        Surface.ROTATION_180 ->
            Pair(width - x, height - y)
        Surface.ROTATION_270 ->
            Pair(width - y, x)
        else -> throw Exception("Unknown rotation: $rotation")
    }
}
