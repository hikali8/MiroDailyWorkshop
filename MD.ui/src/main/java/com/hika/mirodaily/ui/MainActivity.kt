package com.hika.mirodaily.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.hika.accessibility.AccessibilityCoreService // ← 关键：确保能 import 到

class MainActivity : AppCompatActivity() {

    private var overlayView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= 33) requestPostNotificationsIfNeeded()

        // 2) 电池优化白名单
        requestIgnoreBatteryOptimizationsIfNeeded()

        // 3) 悬浮窗权限
        requestOverlayIfNeeded()

        // 创建低重要度渠道（供前台服务用）
        ensureLowImportanceChannel()
    }

    override fun onResume() {
        super.onResume()
        showOverlay()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    // -------- 权限引导 --------

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.areNotificationsEnabled()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:$packageName"))
                )
            } catch (_: Exception) { }
        }
    }

    private fun requestOverlayIfNeeded() {
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) { }
        }
    }

    private fun ensureLowImportanceChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val id = "autotest"
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(id, "AutoTest", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    // -------- 悬浮窗 --------

    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        val tv = TextView(this).apply {
            text = labelForService()
            setPadding(24, 16, 24, 16)
            setBackgroundColor(0x66000000)
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            setOnClickListener {
                val svc = AccessibilityCoreService.getInstance()
                if (svc != null) {
                    svc.togglePaused()
                    text = labelForService()
                } else {
                    // 无障碍还没启用，帮用户跳设置页
                    try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
                }
            }
            setOnLongClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24; y = 120
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(tv, params)
        overlayView = tv
    }

    private fun removeOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    private fun labelForService(): String {
        val paused = AccessibilityCoreService.getInstance()?.isPaused() ?: false
        return if (paused) "MD • 已暂停（点我恢复）" else "MD • 运行中（点我暂停）"
    }
}
