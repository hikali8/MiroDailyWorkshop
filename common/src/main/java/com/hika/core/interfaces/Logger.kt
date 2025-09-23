package com.hika.core.interfaces



enum class Level{ Debug, Info, Warn, Erro }

abstract class Logger {
    abstract val maxLines: Int  // maximal visible lines

    abstract fun println(text: String, color: Level = Level.Debug)

    abstract fun Level.getColor(): Int


    operator fun invoke(text: String, color: Level = Level.Debug) = println(text, color)
}
