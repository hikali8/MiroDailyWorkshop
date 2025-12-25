package com.hika.core

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.display.DisplayManager
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

fun getRotation(context: Context) = context.getSystemService(DisplayManager::class.java)
    .displays.first().rotation

// from and to.
fun rotateWHto(width: Int, height: Int, rotation: Int): Point {
    return if (rotation == Surface.ROTATION_0 || rotation == Surface  .ROTATION_180)
        Point(width, height)
    else
        Point(height, width)
}



// origin 0
fun rotateXYto(x: Float, y: Float, zeroW: Int, zeroH: Int, rotation: Int): PointF {
    return when (rotation) {
        Surface.ROTATION_0 ->
            PointF(x, y)
        Surface.ROTATION_90 ->
            PointF(y, zeroH - x)
        Surface.ROTATION_180 ->
            PointF(zeroW - x, zeroH - y)
        Surface.ROTATION_270 ->
            PointF(zeroW - y, x)
        else -> throw Exception("Unknown rotation: $rotation")
    }
}

// origin 0
fun rotateRectTo(rect: Rect, width: Int, height: Int, rotation: Int): Rect{
    return rect.run {
        when (rotation) {
            Surface.ROTATION_0 -> {this}
            Surface.ROTATION_90 ->
                Rect(top, height - right, bottom, height - left)
            Surface.ROTATION_180 ->
                Rect(width - right, height - bottom, width - left, height - top)
            Surface.ROTATION_270 ->
                Rect(width - bottom, left, width - top, right)
            else -> throw Exception("Unknown rotation: $rotation")
        }
    }
}

// orgin rotation
fun rotateRectFrom(rect: Rect, frontW: Int, frontH: Int, rotation: Int): Rect{
    return rect.run {
        when (rotation) {
            Surface.ROTATION_0 -> {this}
            Surface.ROTATION_90 ->
                Rect(frontW - bottom, left, frontW - top, right)
            Surface.ROTATION_180 ->
                Rect(frontW - right, frontH - bottom, frontW - left, frontH - top)
            Surface.ROTATION_270 ->
                Rect(top, frontH - right, bottom, frontH - left)
            else -> throw Exception("Unknown rotation: $rotation")
        }
    }
}