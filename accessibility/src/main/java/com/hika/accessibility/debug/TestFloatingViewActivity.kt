package com.hika.accessibility.debug

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.hika.accessibility.AccessibilityCoreService
import com.hika.accessibility.databinding.FloatingTestViewBinding
import com.hika.core.toastLine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


class TestFloatingViewActivity : ComponentActivity() {
    // 1. set window manager and window layout params
    private val myWindowManager: WindowManager by lazy {
        this.getSystemService(WindowManager::class.java)
    }

    val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SECURE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 500

        // 窗口类型
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

    val binding: FloatingTestViewBinding by lazy {
        FloatingTestViewBinding.inflate(layoutInflater)
    }

    private var updateJob: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("#0x-TA", "onCreate TestFloatingViewActivity")
        super.onCreate(savedInstanceState)

        // 设置拖拽功能
        binding.floatingContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录初始位置
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 计算偏移量并更新位置
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(binding.root, layoutParams)
                    true
                }
                else -> false
            }
        }

        open()
    }

    override fun onDestroy() {
        Log.d("#0x-TA", "onDestroy TestFloatingViewActivity")
        myWindowManager.removeView(binding.root)
        super.onDestroy()
    }


    // 2. Open Floating Window
    // params used for dragging
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    fun open() {
        if (isFloatingWindowOpen()) {
            // 已经显示，不需要重复添加
            toastLine("悬浮窗已显示")
            return
        }

        if (!hasOverlayPermission()) {
            // request overlay permission
            Log.d("#0xTA", "Trying to request floating permission")
            requestOverlayPermission()
            return
        }

        Log.d("#0xTA", "Trying to start floating window")

        // 添加视图到WindowManager
        try {
            myWindowManager.addView(binding.root, layoutParams)
            updateJob = lifecycleScope.launch {
                while (true){
                    updateFrame()
                    delay(500)
                }
            }
            toastLine("悬浮窗已显示")
        } catch (e: Exception) {
            e.printStackTrace()
            toastLine("显示悬浮窗失败: ${e.message}")
        }
    }

    val imageHandler by lazy{
        AccessibilityCoreService.instance.get()?.imageHandler
            ?: throw Exception("ImageHandler Uninitialized")
    }

    fun updateFrame(){
        val recognizable = imageHandler.getRecognizable()
        if (recognizable == null)
            return
        val bitmap = nv21ToBitmap(recognizable.imageNV21_Array,
            recognizable.width,
            recognizable.height)

        if (bitmap == null)
            return

//        val bitmap = createBitmap(recognizable.width, recognizable.height, Bitmap.Config.ARGB_8888)
//
//        val buffer = recognizable.imageBuffer
//
//        // 创建一个新buffer，容量为原buffer剩余字节数
//        val newBuffer = ByteBuffer.allocate(buffer.remaining())
//
////        buffer.position(buffer.position() + 1)
//        buffer.limit(buffer.limit() - 1)
//
//        // 将原buffer从当前位置开始的数据复制到新buffer
////        newBuffer.put(0x00)
//        newBuffer.put(buffer)
//
//
//        newBuffer.flip();
//
//        bitmap.copyPixelsFromBuffer(newBuffer)

        val canvas = binding.textureView.lockCanvas()
        canvas?.let {
            // 创建变换矩阵
            val matrix = Matrix()

            // 计算缩放比例
            val scale = calculateScale(bitmap.width, bitmap.height,
                canvas.width, canvas.height)

            // 方法1: 等比例缩放并居中
            matrix.setScale(scale, scale)
            matrix.postTranslate(
                (canvas.width - bitmap.width * scale) / 2,
                (canvas.height - bitmap.height * scale) / 2
            )

            it.drawBitmap(bitmap, matrix, null)
            binding.textureView.unlockCanvasAndPost(it)
        }

//        withContext(Dispatchers.Main) {
//            bitmap?.let { binding.textureView.setBitmap(it) }
//        }
    }

    private fun calculateScale(
        imgWidth: Int,
        imgHeight: Int,
        viewWidth: Int,
        viewHeight: Int
    ): Float {
        val scaleX = viewWidth.toFloat() / imgWidth
        val scaleY = viewHeight.toFloat() / imgHeight
        return minOf(scaleX, scaleY)  // 保持宽高比的缩放
    }

    private var reusableBitmap: Bitmap? = null

    fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int): Bitmap?{
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val stream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, stream)
        val jpegData = stream.toByteArray()

        val options = BitmapFactory.Options().apply {
            inMutable = true
            reusableBitmap?.apply {
                inBitmap = this
            }
        }

        reusableBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size, options)

        return reusableBitmap
    }

    // things else

    private fun isFloatingWindowOpen() = binding.root.isAttachedToWindow

    private fun hasOverlayPermission() = Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${this.packageName}".toUri()
        )
        overlayRequestLauncher.launch(intent)
    }

    private val overlayRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 不管结果如何，我们都尝试显示悬浮窗
        // 因为用户可能已经授权，只是通过返回键返回
        if (hasOverlayPermission()) {
            open()
        } else {
            // 可以在这里添加提示，告诉用户需要权限
        }
    }
}