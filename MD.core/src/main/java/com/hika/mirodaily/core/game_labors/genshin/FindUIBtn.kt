package com.hika.mirodaily.core.game_labors.genshin

import android.util.Log

const val MEASURE_W = 2780.0
const val MEASURE_H = 1264.0

enum class UIBtn(val xScale: Double, val yScale: Double, val rScale: Double,
                 var x: Float = -1f, var y: Float = -1f, var r: Float = -1f) {
    WASD(511 / MEASURE_W, 966 / MEASURE_H, 173 / MEASURE_W),
    S(2437 / MEASURE_W, 1088 / MEASURE_H, 89 / MEASURE_W),
    I(1681 / MEASURE_W, 631 / MEASURE_H, 31 / MEASURE_W),
    MAP(350 / MEASURE_W, 183 / MEASURE_H, 159 / MEASURE_W),
    MAP_ZOOM_BAR(175/ MEASURE_W, 632/ MEASURE_H, 113/ MEASURE_W),
    MAP_REGION_SELECT(2559 / MEASURE_W, 1172 / MEASURE_H, 46 / MEASURE_W),
    MAP_TELEPORT_CONFIRM(2019 / MEASURE_W, 1158 / MEASURE_H, 32 / MEASURE_W),
    MAP_POINT_SELECT(1811 / MEASURE_W, 755 / MEASURE_H, 32 / MEASURE_W),
    MAP_MODSTADT(1857 / MEASURE_W, 319 / MEASURE_H, 50 / MEASURE_W),
    MAP_FONTAINE(1856 / MEASURE_W, 624 / MEASURE_H, 40 / MEASURE_W),

    MAP_SUNSETLAKE(1739 / MEASURE_W, 402 / MEASURE_H, 26 / MEASURE_W),
    MAP_ASSOCIATION(1444 / MEASURE_W, 607 / MEASURE_H, 30 / MEASURE_W)
    ;
    companion object {
        fun updateWH(width: Int, height: Int){
            Log.i("#0x-", "updateCoordinate $width x $height")
            for (uib in UIBtn.entries){
                uib.x = (width * uib.xScale).toFloat()
                uib.y = (height * uib.yScale).toFloat()
                uib.r = (width * uib.rScale).toFloat()
            }
        }
    }
}


