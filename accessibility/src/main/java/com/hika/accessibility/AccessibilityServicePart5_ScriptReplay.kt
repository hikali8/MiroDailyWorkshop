package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class AccessibilityServicePart5_ScriptReplay : AccessibilityServicePart4_ScreenWatching() {
    // 5. Script Replay
    enum class Type{
        Down, Up, Move, NEXT, wait
    }

    var job: Job? = null
    abstract inner class IAccessibilityExposed_Part5: IAccessibilityExposed_Part4(){
        override fun replayScript(script: String) {
            job = coroutineScope.launch {
                for (description in extractScript(script))
                    coroutineScope.launch {
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

    val maxStrokeCount = GestureDescription.getMaxStrokeCount()
    val maxDuration = GestureDescription.getMaxGestureDuration()
    private fun extractScript(script: String): MutableList<GestureDescription> {
        val gestureDescriptions = mutableListOf<GestureDescription>()

        var builder = GestureDescription.Builder()
        var gestureCount = 0
        var startTime: Long = 0

        var path = Path()
        var endX = 0f
        var endY = 0f

        var currentTime: Long = 0
        var duration: Long = 0
        var type: Type? = null

        val doo = {
            if (duration > maxDuration){
                throw Exception("Duration too long: $duration, type: ${type?.name}")
            }

            if (gestureCount >= maxStrokeCount || currentTime - startTime + duration > maxDuration) {
                gestureDescriptions.add(builder.build())
                builder = GestureDescription.Builder()
                startTime = currentTime
                gestureCount = 0
            }
            builder.addStroke(GestureDescription.StrokeDescription(
                path, currentTime, duration
            ))
            gestureCount += 1
            currentTime += duration
            path = Path()
            duration = 0
        }

        for (line in script.lineSequence()){
            val cols = line.split(',')
            when (cols[0]) {
                Type.Down.name ->{
                    if (type == Type.Down)
                        throw Exception("Ununderstood Branch")
                    endX = cols[2].toFloat()
                    endY = cols[3].toFloat()
                    type = Type.Down
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

                    duration = cols[1].toLong()
                    val x = cols[2].toFloat()
                    val y = cols[3].toFloat()
                    type = Type.Move

                    path.moveTo(endX, endY)
                    path.lineTo(x, y)
                    endX = x
                    endY = y
                    doo()
                }
                Type.NEXT.name -> {
                    if (type == Type.Down)
                        doo()
                    val time = cols[1].toLong()
                    currentTime = time
                    startTime = time
                    type = Type.NEXT
                }
                Type.wait.name -> {
                    val dur = cols[1].toLong()
                    when (type) {
                        Type.Down -> {
                            duration += dur
                        }
                        Type.Up -> {
                            currentTime += dur
                        }
                        Type.Move -> {
                            path.moveTo(endX, endY)
                            duration = dur
                            doo()
                        }
                        Type.NEXT -> {
                            currentTime += dur
                            startTime = currentTime
                        }
                        else -> throw Exception("Ununderstood Branch")
                    }
                }
                else -> throw Exception("Unknown instruction: " + cols[0])
            }
        }

        if (type == Type.Down)
            doo()
        gestureDescriptions.add(builder.build())

        return gestureDescriptions
    }
}