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
