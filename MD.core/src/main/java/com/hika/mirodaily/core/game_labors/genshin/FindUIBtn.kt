package com.hika.mirodaily.core.game_labors.genshin

const val MEASURE_W = 2780.0
const val MEASURE_H = 1264.0

enum class UIBtn(val xScale: Double, val yScale: Double, val rScale: Double,
                 var x: Float = -1f, var y: Float = -1f, var r: Float = -1f) {
    WASD(256 / MEASURE_W, 1115 / MEASURE_H, 81 / MEASURE_W),
    S(2608 / MEASURE_W, 1176 / MEASURE_H, 44 / MEASURE_W),
    I(2229 / MEASURE_W, 946 / MEASURE_H, 16 / MEASURE_W),
    MAP(177 / MEASURE_W, 92 / MEASURE_H, 79 / MEASURE_W),
    MAP_REGION_SELECT(2670 / MEASURE_W, 1219 / MEASURE_H, 22 / MEASURE_W),
    MAP_MODSTADT(2318 / MEASURE_W, 160 / MEASURE_H, 30 / MEASURE_W),
    MAP_SUNSETLAKE(2260 / MEASURE_W, 202 / MEASURE_H, 12 / MEASURE_W),
    MAP_TELEPORT_CONFIRM(2400 / MEASURE_W, 1212 / MEASURE_H, 16 / MEASURE_W);
    companion object {
        fun updateCoordinate(curWidth: Int, curHeight: Int){
            for (uib in UIBtn.entries){
                uib.x = (curWidth * uib.xScale).toFloat()
                uib.y = (curHeight * uib.yScale).toFloat()
                uib.r = (curWidth * uib.rScale).toFloat()
            }
        }
    }
}


