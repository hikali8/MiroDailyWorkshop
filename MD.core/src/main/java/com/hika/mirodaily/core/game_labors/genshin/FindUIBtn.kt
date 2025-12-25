package com.hika.mirodaily.core.game_labors.genshin

import android.util.Log

const val MEASURE_W = 2780.0
const val MEASURE_H = 1264.0

enum class UIBtn(val xScale: Double, val yScale: Double, val rScale: Double) {
    WASD(511, 966, 173),
    S(2437, 1088, 89),
    BOOK(2331, 70, 34),
    INVESTIGATE(1681, 631, 31),
    CONVERSATION(1390, 1190, 22),
    ASSOCIATION_REWARD(1746, 185, 35),

    MAP(350, 183, 159),
    MAP_ZOOM_BAR(175, 632, 113),
    MAP_ZOOM_BAR_ADD(175, 491, 15),
    MAP_REGION_SELECT(2559, 1172, 46),
    MAP_TELEPORT_CONFIRM(2019, 1158, 32),
    MAP_TELEPORT_SELECT(1811, 755, 32),
    MAP_MODSTADT(1857, 319, 50),
    MAP_FONTAINE(1856, 624, 40),

    MAP_SUNSETLAKE(1739, 402, 26),
    MAP_FONTAINE_ASSOCIATION(1444, 607, 30),

    CRAFTER_SYNTHESIS(2263, 1172, 31),
    BOOK_COMMISSION(396, 302, 15),
    BOOK_REWARD(2251, 943, 46),
    BOOK_TELEPORT_REWARD(658, 1085, 25)
    ;

    var x: Float = -1f
    var y: Float = -1f
    var r: Float = -1f

    constructor(_x: Int, _y: Int, _r: Int)
            : this(_x / MEASURE_W, _y / MEASURE_H, _r / MEASURE_W)

    companion object {
        fun updateWH(width: Int, height: Int){
            var w = width
            var h = height
            if (w < h){
                val t = w
                w = h
                h = t
            }
            Log.i("#0x-", "updateCoordinate $w x $h")
            for (uib in UIBtn.entries){
                uib.x = (w * uib.xScale).toFloat()
                uib.y = (h * uib.yScale).toFloat()
                uib.r = (w * uib.rScale).toFloat()
            }
        }
    }
}


