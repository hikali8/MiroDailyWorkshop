package com.hika.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.hika.core.getRotation
import com.hika.core.rotateXYto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class AccessibilityServicePart5_ScriptReplay : AccessibilityServicePart4_ScreenWatching() {
    // 5. Script Replay
    var job: Job? = null
    abstract inner class IAccessibilityExposed_Part5: IAccessibilityExposed_Part4(){
        override fun replayScript(script: String) {
            if (job?.isActive == true){
                Log.e("#0x-AS5", "已经有脚本处于执行中")
                return
            }

            job = coroutineScope.launch(Dispatchers.IO) {
                val rotation = getRotation(this@AccessibilityServicePart5_ScriptReplay)
                val gestures = ScriptReplayer().extractScript(script)
                Log.d("#0x-AS5", "解开是：" + gestures.toString())
                val screen = screenSize
                for (gesture in gestures)
                    coroutineScope.launch(Dispatchers.IO) {
                        delay(gesture.startTime)
                        val builder = GestureDescription.Builder()
                        var startTime = 0L
                        for (stroke in gesture.strokes){
                            val path = Path()
                            stroke.points.removeFirstOrNull()?.apply {
                                val rotated = rotateXYto(x, y, screen.x, screen.y, rotation)
                                path.moveTo(rotated.x, rotated.y)
                            } ?: continue
                            for (point in stroke.points){
                                val rotated = rotateXYto(point.x, point.y, screen.x, screen.y, rotation)
                                path.lineTo(rotated.x, rotated.y)
                            }
                            builder.addStroke(GestureDescription.StrokeDescription(
                                path, startTime,
                                stroke.duration - 1
                            ))
                            if (stroke.duration > 3000L || stroke.duration < 0L){
                                throw Exception("wtf are you playing?")
                            }

                            startTime += stroke.duration
                        }
                        dispatchGesture(builder.build(), null, null)
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

    override fun onResize() { }

    class ScriptReplayer {
        enum class Type{ Down, Move, Up, wait, NEXT }

        /**
         * workflow:
         *   Script: Down, wait, Move, wait, Up, wait, Down, .... NEXT, wait, wait, Down, wait, Up, ...
         *     to
         *   TimePath: t1--p1--t2--p2
         *     to
         *   Gesture: t1--(pa1,pa2,...)d1--(pb1,pb2,...)d2--...
         */
        class TimePoint(var preTime: Long = 0, var x: Float = -1f, var y: Float = -1f){
            fun copy() = TimePoint(preTime, x, y)
        }
        class TimePath(): ArrayList<TimePoint>(){
            constructor(timePoint: TimePoint) : this() {
                add(timePoint)
            }
        }

        class Stroke(val points: ArrayList<PointF>, var duration: Long = 0){
            constructor(point: PointF, duration: Long = 0): this(arrayListOf(point), duration)
            // only x, y will be stored
            constructor(point: TimePoint, duration: Long = 0): this(PointF(point.x, point.y), duration)
        }
        class Gesture(var startTime: Long, val strokes: ArrayList<Stroke> = arrayListOf()){
            constructor(startTime: Long, stroke: Stroke): this(startTime, arrayListOf(stroke))
        }

        // return: mutableListOf(gesture)
        fun extractScript(script: String) : ArrayList<Gesture> {
            val timePaths = ArrayList<TimePath>()
            // when the x, y of the point is unknown
            var unDownTimePoint: TimePoint? = null
            for (line in script.lineSequence()){
                val cols = line.split(',')
                val type = Type.valueOf(cols[0])
                when (type){
                    Type.Down -> {
                        val x = cols[2].takeIf { it != "" } ?.toFloat()
                            ?: timePaths.last().last().x
                        val y = cols.getOrNull(3)?.toFloat()
                            ?: timePaths.last().last().y
                        timePaths.add(TimePath(unDownTimePoint?.apply {
                            if (preTime < 0) preTime = countTotalTime(timePaths.last())
                            this.x = x
                            this.y = y
                        } ?: TimePoint(
                            if (timePaths.isEmpty()) 0 else
                                countTotalTime(timePaths.last()),
                            x,
                            y
                        )))
                        unDownTimePoint = null
                    }
                    Type.Move -> {
                        val timePath = timePaths.last()
                        timePath.add(TimePoint(
                            cols[1].toLong(),
                            cols[2].takeIf { it != "" } ?.toFloat()
                                ?: timePath.last().x,
                            cols.getOrNull(3)?.toFloat()
                                ?: timePath.last().y
                        ))
                    }
                    Type.Up -> unDownTimePoint = TimePoint(-1)
                    Type.wait -> {
                        val time = cols[1].toLong()
                        val lastPath = timePaths.last()
                        if (unDownTimePoint != null){
                            if (unDownTimePoint.preTime < 0)
                                unDownTimePoint.preTime = countTotalTime(timePaths.last())
                            unDownTimePoint.preTime += time
                        } else {
                            val point = lastPath.last()
                            if (lastPath.size > 1
                                && lastPath[lastPath.size - 2].run { x == point.x && y == point.y })
                                point.preTime += time
                            else
                                lastPath.add(TimePoint(time, point.x, point.y))
                        }
                    }
                    Type.NEXT -> unDownTimePoint = TimePoint(cols[1].toLong())
                }
            }

            return packTimePaths(timePaths)
        }

        inline fun countTotalTime(timePath: TimePath) = timePath.sumOf { it.preTime }



        private val maxDuration = GestureDescription.getMaxGestureDuration()    // of a gesture
        private val maxCount = GestureDescription.getMaxStrokeCount()    // of a gesture
        /**
         *   TimePath: t1--p1--t2--p2
         *     to
         *   Gesture: t1--(pa1,pa2,...)d1--(pb1,pb2,...)d2--...
         */
        fun packTimePaths(timePaths: ArrayList<TimePath>): ArrayList<Gesture>{
            val gestures = ArrayList<Gesture>()
            for (timePath in timePaths){
                val gesture = timePath.removeFirstOrNull() ?.run {
                    Gesture(preTime, Stroke(this) )
                } ?: continue
                var last = gesture.strokes.last()
                var lastPretime = -1L
                for (point in timePath){
                    if (point.preTime.run{
                            this != 0L && lastPretime.toFloat() / this in 0.9..1.1
                        }){
                        last.points.add(PointF(point.x, point.y))
                        last.duration += lastPretime
                    } else {
                        if (lastPretime == -1L) {
                            last.points.add(PointF(point.x, point.y))
                        } else {
                            last = Stroke(arrayListOf(
                                last.points.last(), PointF(point.x, point.y)
                            ))
                            gesture.strokes.add(last)
                        }
                        last.duration = point.preTime
                        lastPretime = point.preTime
                    }
                }
                if (gesture.strokes.size > maxCount)
                    throw Exception("Too much strokes, n=${gesture.strokes.size}, maximum=$maxCount")
                for (stroke in gesture.strokes){
                    val points = stroke.points
                    if (points.size == 2 &&  points[0] == points[1])
                        points.removeAt(1)
                }
                gestures.add(gesture)
            }
            return gestures
        }

        // referring this, maybe able to reduce strokes
        fun shrinkPoints(timePath: TimePath, targetStroke: Int): TimePath {
            if (timePath.size < targetStroke)
                return timePath
            val timePath1 = TimePath()
            var last: TimePoint? = null
            var totalDuration = 0L

            val interval = ((timePath.size - 1).toDouble() / targetStroke).roundToInt()
            for ((index, point) in timePath.withIndex()){
                if (index == 0 || (index % interval == 1 && timePath1.size <= targetStroke)){
                    last = point.copy()
                    timePath1.add(last)
                    totalDuration += last.preTime
                }else{
                    last!!.preTime += point.preTime
                    totalDuration += point.preTime
                    last.x = point.x
                    last.y = point.y
                }
            }
            if (totalDuration > maxDuration){
                throw Exception("Duration Too Long: $totalDuration")
            }
            return timePath1
        }
    }

    override fun onMainProgramDisconnected(){
        iAccessibilityExposed.stopReplay()
        super.onMainProgramDisconnected()
    }
}