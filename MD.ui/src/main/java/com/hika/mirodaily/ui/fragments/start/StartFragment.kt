package com.hika.mirodaily.ui.ui.fragments.start


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.R
import kotlinx.coroutines.Job

class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null
    private var _floatView: View? = null

    // These properties above are only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val floatView get() = _floatView!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root = binding.root
        _floatView = layoutInflater.inflate(R.layout.floating_window, null)

        // bind functions for buttons
        binding.btnAccessibility.setOnClickListener{
            (activity as MainActivity).enableAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener{
            (activity as MainActivity).requestProjection()
        }
        binding.btnStart.setOnClickListener(::onStartClick)

        return root
    }

    override fun onDestroyView() {
        requireContext().getSystemService(WindowManager::class.java)
            .removeView(_floatView)
        super.onDestroyView()
        _binding = null
        _floatView = null
    }

    // Open Floating window and start automation.
    private fun onStartClick(view: View){
        // floating window, but later will be used to show Logs
        if (!isFloatingWindowShowing()){
            Log.d("#0xSF", "Trying to start floating window")
            // request overlay window
            if (!hasOverlayPermission()) {
                Log.d("#0xSF", "Trying to request floating permission")
                requestOverlayPermission()
            }
            else
                showFloatingWindow()
        }

        if (iAccessibilityService?.isProjectionStarted() != true){
            Log.d("#0x-SF", "Projection not yet started")
            return
        }
        job?.cancel()
        job = DailyCheckIn(requireContext(), lifecycleScope).openApp()
    }

    private fun isFloatingWindowShowing(): Boolean {
        return _floatView != null && floatView.isAttachedToWindow
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context?.packageName}".toUri()
        )
        overlayRequestLauncher.launch(intent)
    }

    private val overlayRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 不管结果如何，我们都尝试显示悬浮窗
        // 因为用户可能已经授权，只是通过返回键返回
        if (hasOverlayPermission()) {
            showFloatingWindow()
        } else {
            // 可以在这里添加提示，告诉用户需要权限
        }
    }

    private fun hasOverlayPermission() = Settings.canDrawOverlays(context)


    // 用于拖拽的变量
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var job: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingWindow() {
        if (isFloatingWindowShowing()) {
            // 已经显示，不需要重复添加
            Toast.makeText(context, "悬浮窗已显示", Toast.LENGTH_SHORT).show()
            return
        }

        // 设置LayoutParams
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SECURE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 500
        }

        // 添加视图到WindowManager
        try {
            requireContext().getSystemService(WindowManager::class.java)
                .addView(floatView, layoutParams)

            // 设置关闭按钮点击事件
            floatView.findViewById<Button>(R.id.btnClose).setOnClickListener {
                hideFloatingWindow()
            }

            // 设置拖拽功能
            floatView.findViewById<View>(R.id.floatWindowContainer).setOnTouchListener { v, event ->
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
                        requireContext().getSystemService(WindowManager::class.java)
                            .updateViewLayout(floatView, layoutParams)
                        true
                    }
                    else -> false
                }
            }

            Toast.makeText(requireContext(), "悬浮窗已显示", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "显示悬浮窗失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideFloatingWindow() {
        if (isFloatingWindowShowing()) {
            try {
                requireContext().getSystemService(WindowManager::class.java)
                    .removeView(floatView)
                Toast.makeText(requireContext(), "悬浮窗已隐藏", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "隐藏悬浮窗失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "没有悬浮窗显示", Toast.LENGTH_SHORT).show()
        }
    }
}