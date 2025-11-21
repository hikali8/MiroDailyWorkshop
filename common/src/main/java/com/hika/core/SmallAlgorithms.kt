package com.hika.core

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
