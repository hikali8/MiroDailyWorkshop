package com.hika.core.interfaces

interface FloatingWindowControll {
    fun open(): Boolean
    fun hide()
    var screenWidth: Int
    var screenHeight: Int
}