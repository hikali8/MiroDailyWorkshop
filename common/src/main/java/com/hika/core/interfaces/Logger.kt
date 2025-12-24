package com.hika.core.interfaces



enum class Level{ Debug, Info, Warn, Erro }

interface Logger {
    fun println(text: String, color: Level = Level.Debug)

    fun Level.getColor(): Int


    operator fun invoke(text: String, color: Level = Level.Debug) = println(text, color)
}
