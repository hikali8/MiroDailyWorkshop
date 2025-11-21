package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

abstract class AccessibilityServicePart5_ScriptReplay : AccessibilityServicePart4_ScreenWatching() {
    // 5. Script Replay
    enum class Type{
        Down, Up, Move, NEXT, wait
    }

    var job: Job? = null
    abstract inner class IAccessibilityExposed_Part5: IAccessibilityExposed_Part4(){
        override fun replayScript(script: String) {
            if (job?.isActive == true){
                Log.e("#0x-AS5", "已经有脚本处于执行中")
                return
            }
            job = coroutineScope.launch(Dispatchers.IO) {
                rotation = getSystemService(DisplayManager::class.java)
                    .displays.first().rotation

                val extracted = extractScript(script)
//                Log.d("#0x-AS5", "解开是：" + extracted.toString())
                for ((time, description) in extracted)
                    coroutineScope.launch(Dispatchers.IO) {
                        delay(time)
                        dispatchGesture(description, null, null)
                    }
            }
        }

        override fun stopReplay() {
            if (job?.isActive == true) {
                Log.d("#0x-AS5", "已终止重放")
                job?.cancel()
            } else
                Log.d("#0x-AS5", "未找到重放线程")
        }
    }

    private var rotation by Delegates.notNull<Int>()
    private fun rotateCoordinate(x: Float?, y: Float?): Pair<Float?, Float?> {
        return when (rotation) {
            Surface.ROTATION_0 ->
                Pair(x, y)
            Surface.ROTATION_90 ->
                Pair(y, if (x == null) null else height - x)
            Surface.ROTATION_180 ->
                Pair(if (x == null) null else width - x, if (y == null) null else height - y)
            Surface.ROTATION_270 ->
                Pair(if (y == null) null else width - y, x)
            else -> throw Exception("Unknown rotation: $rotation")
        }
    }


    val maxDuration = GestureDescription.getMaxGestureDuration()
    private fun extractScript(script: String): MutableList<Pair<Long, GestureDescription>> {
        val gestureDescriptions = mutableListOf<Pair<Long, GestureDescription>>()

        var startTime: Long = 0

        var path = Path()
        var endX = 0f
        var endY = 0f

        var duration: Long = 0
        var type: Type? = null

        val doo = {
            if (duration > maxDuration){
                throw Exception("Duration too long: $duration, operation: ${type?.name}")
            }
            // StrokeDescription.startTime is very unusable, easily causing crash
            gestureDescriptions.add(Pair(startTime,
                GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(
                    path, 0, duration
                )).build()))
            startTime += duration
            path = Path()
            duration = 0
        }

        for (line in script.lineSequence()){
            val cols = line.split(',')
            when (cols[0]) {
                Type.Down.name ->{
                    if (type == Type.Down)
                        throw Exception("Ununderstood Branch")
                    type = Type.Down

                    val x = cols[2].run {
                        if (this == "") null else this.toFloat()
                    }
                    val y = cols.getOrNull(3)?.toFloat()
                    val p = rotateCoordinate(x, y)
                    p.first?.apply { endX = this }
                    p.second?.apply { endY = this }

                    path.moveTo(endX, endY)
                }
                Type.Up.name -> {
                    if (type == Type.Down)
                        doo()
                    type = Type.Up
                }
                Type.Move.name -> {
                    if (type == Type.Down)
                        doo()
                    type = Type.Move
                    path.moveTo(endX, endY)

                    duration = cols[1].toLong()
                    val x = cols[2].run {
                        if (this == "") null else this.toFloat()
                    }
                    val y = cols.getOrNull(3)?.toFloat()
                    val p = rotateCoordinate(x, y)
                    p.first?.apply { endX = this }
                    p.second?.apply { endY = this }

                    path.lineTo(endX, endY)
                    doo()
                }
                Type.NEXT.name -> {
                    if (type == Type.Down)
                        doo()
                    type = Type.NEXT

                    startTime = cols[1].toLong()
                }
                Type.wait.name -> {
                    val dur = cols[1].toLong()

                    when (type) {
                        Type.Down -> {
                            duration += dur
                        }
                        Type.Up -> {
                            startTime += dur
                        }
                        Type.Move -> {
                            path.moveTo(endX, endY)
                            duration = dur
                            doo()
                        }
                        Type.NEXT -> {
                            startTime += dur
                        }
                        else -> throw Exception("Ununderstood Branch")
                    }
                }
                else -> throw Exception("Unknown instruction: " + cols[0])
            }
        }

        if (type == Type.Down)
            doo()
        return gestureDescriptions
    }


    override fun onMainProgramDisconnected(){
        iAccessibilityExposed.stopReplay()
        super.onMainProgramDisconnected()
    }
}