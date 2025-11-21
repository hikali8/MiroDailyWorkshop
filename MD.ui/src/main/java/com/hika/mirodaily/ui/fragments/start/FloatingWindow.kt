package com.hika.mirodaily.ui.fragments.start

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import com.hika.core.interfaces.Level
import com.hika.core.interfaces.Logger
import com.hika.core.toastLine
import com.hika.mirodaily.ui.databinding.FloatingWindowBinding
import java.util.Calendar

class FloatingWindow(val context: Context,
                     inflater: LayoutInflater,
                     val onOverlaySettingResult: ActivityResultLauncher<Intent>) {

    // 1. set window manager and window layout params
    val windowManager get() = context.getSystemService(WindowManager::class.java)

    val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_SECURE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 1500
    }

    private val binding = FloatingWindowBinding.inflate(inflater)


    // 2. Open Floating Window
    // params used for dragging
    private var touchX = 0f
    private var touchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    fun open() {
        if (isFloatingWindowOpen()) {
            // 已经显示，不需要重复添加
            toastLine("悬浮窗已显示", context)
            return
        }

        if (!isOverlayPermitted()) {
            // request overlay permission
            Log.d("#0xSF", "Trying to request floating permission")
            openOverlaySetting()
            return
        }

        Log.d("#0xSF", "Trying to start floating window")

        // 添加视图到WindowManager
        try {
            windowManager.addView(binding.root, layoutParams)
            toastLine("悬浮窗显示", context)
        } catch (e: Exception) {
            e.printStackTrace()
            toastLine("悬浮窗显示失败: ${e.message}", context)
        }

        // 设置拖拽功能
        binding.floatWindow.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录初始位置
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 计算偏移量并更新位置
                    layoutParams.x += (event.rawX - touchX).toInt()
                    layoutParams.y += (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(binding.root, layoutParams)
                    true
                }
                else -> false
            }
        }
    }

    fun isFloatingWindowOpen() = binding.root.isAttachedToWindow

    fun isOverlayPermitted() = Settings.canDrawOverlays(context)

    private fun openOverlaySetting() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        toastLine("请打开米洛工坊的悬浮窗权限...", context, true)
        onOverlaySettingResult.launch(intent)
    }

    fun onLaunchResult(){
        if (isOverlayPermitted()) {
            open()
        } else {
            toastLine("权限未添加", context)
        }
    }

    // 2.1. Reset the coordinate of window
    fun resetCoordinate(x: Int, y: Int){
        layoutParams.x = x
        layoutParams.y = y
        windowManager.updateViewLayout(binding.root, layoutParams)
    }


    // 3. Close floating window (not hide or destroy, which should be done by the parent)
    fun close(){
        if (isFloatingWindowOpen())
            windowManager.removeView(binding.root)
    }


    // 4. println for floating window
    val lineWidth = 40
    val indent = 17  // should not appear
    val logger = object: Logger {
        override val maxLines = 15

        override fun println(text: String, color: Level) {
            // line break
            var text = lineBreak(text, lineWidth, indent)
            // add prefix
            text = getTimestampPrefix() + text

            // get spannable
            var spannableString = SpannableStringBuilder(text)
            // set color
            spannableString.setSpan(BackgroundColorSpan(color.getColor()),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // add previous text
            spannableString = SpannableStringBuilder(binding.logText.text)
                .append('\n')
                .append(spannableString)

            var countedLine = maxLines
            val index = spannableString.indexOfLast {
                if (it == '\n' && --countedLine <= 0)
                    return@indexOfLast true
                false
            }.run { if (this == -1) 0 else this + 1 }

            spannableString.delete(
                0,
                if (index == -1) 0 else index + 1)

            binding.logText.setText(spannableString,
                TextView.BufferType.SPANNABLE)
        }

        override fun Level.getColor() = when(this){
            Level.Info -> Color.BLUE
            Level.Warn -> Color.YELLOW
            Level.Erro -> Color.RED
            else -> Color.TRANSPARENT
        }

    }

    private fun lineBreak(text: String, width: Int, indent: Int): String {
        val innerWidth = width - indent
        if (innerWidth < 1)
            return ""

        val lineBreaker = '\n' + " ".repeat(indent)

        val curText = StringBuilder()
        var curLineWidth = 0
        for (c in text){
            if (c == '\n'){
                curText.append(lineBreaker)
                curLineWidth = 0
                continue
            }

            curLineWidth += if (c.code < 256) 1 else 2
            if (curLineWidth > innerWidth) {
                curText.append(lineBreaker)
                curLineWidth = 0
            }
            curText.append(c)
        }

        return curText.toString()
    }

    private val calendar = Calendar.getInstance()
    private fun getTimestampPrefix(): String {
        // 年份后两位
        val year = (calendar.get(Calendar.YEAR) % 100).toString().padStart(2, '0')
        // 月份（从0开始，需要+1）
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        // 日期
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        // 小时
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        // 分钟
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        // 秒
        val second = calendar.get(Calendar.SECOND).toString().padStart(2, '0')

        // 格式：250923.18.04.23:
        return "$year$month$day.$hour.$minute.$second: "
    }
}

