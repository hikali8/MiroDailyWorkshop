package com.hika.mirodaily.ui.ui.fragments.start

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hika.core.loopUntil
import com.hika.core.toastLine
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.core.START_BROADCAST
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.launch

class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)

        // 先初始化悬浮窗对象（避免按钮点击时对象未初始化）
        floatingWindow = FloatingWindow(requireContext(), inflater, onOverlaySettingResult)

        // 1-3卡片按钮绑定
        binding.btnAccessibility.setOnClickListener {
            updateUi()
            openAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener {
            updateUi()
            lifecycleScope.launch { requestProjection() }
        }
        binding.btnOverlay.setOnClickListener {
            updateUi()
            floatingWindow.open()
        }

        // 跳到 Config（脚本/分组/计划都在 Config 页）
        binding.btn4.setOnClickListener {
            findNavController().navigate(R.id.navigation_config)
        }

        updateUi()
        return binding.root
    }

    // 1.1 打开无障碍设置
    private fun openAccessibilitySetting() {
        if (isAccessibilitySettingEnabled() == true) {
            toastLine("无障碍设置已启用，仍旧打开无障碍设置以便调试时重新开启", context)
        } else {
            toastLine("请启用无障碍服务", context, true)
        }
        onAccessibilitySettingResult.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private val onAccessibilitySettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isAccessibilitySettingEnabled() == true)
            toastLine("无障碍设置已启用", context)
        else
            toastLine("无障碍设置启用失败", context)
        updateUi()
    }

    // 检查无障碍设置启用，如果检查失败则为null
    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    // 1.2 请求投屏权限
    private suspend fun requestProjection() {
        if (isAccessibilitySettingEnabled() != true) {
            toastLine("无障碍设置未启用，请先启用它", context, true)
            return
        }
        if (iAccessibilityService == null) {
            toastLine("正在通知无障碍服务绑定此应用...此后才可以为它启用投屏...", context, true)
            startReceiver()
            accessibilityBindBroadcast()

            if (!loopUntil(5000) { iAccessibilityService != null }) {
                toastLine("服务未能绑定应用.", context)
                return
            }
        }
        if (iAccessibilityService!!.isProjectionStarted()) {
            toastLine("投屏权限已经授予。", context)
            return
        }
        onProjectionRequestingResult.launch(Intent().apply {
            setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
        })
        toastLine("请在新开的窗口中授予“整个屏幕”的投屏权限", context, true)
    }

    private val onProjectionRequestingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (iAccessibilityService?.isProjectionStarted() == true)
            toastLine("投屏权限授予成功", context)
        else
            toastLine("投屏权限授予失败", context, true)
        updateUi()
    }

    // 打开接收器，接收无障碍服务传来的暴露接口
    private fun startReceiver() {
        context?.startService(Intent(context, ASReceiver::class.java))
    }

    // 发出广播，无障碍服务就应该连接接收器
    private fun accessibilityBindBroadcast() {
        requireContext().sendBroadcast(Intent(START_BROADCAST).apply {
            setPackage(AccessibilityPackageName)
            putExtra("timestamp", System.currentTimeMillis())
        })
    }

    // 悬浮窗权限设置返回
    private val onOverlaySettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        floatingWindow.onLaunchResult()
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onDestroyView() {
        floatingWindow.close()
        super.onDestroyView()
    }

    // ⬇️UI 状态 & 权限检测
    private fun updateUi() {
        val acc = isAccessibilitySettingEnabled() == true
        val projection = iAccessibilityService?.isProjectionStarted() == true
        val overlay = floatingWindow.isOverlayPermitted()

        binding.dot1.setImageResource(if (acc) R.drawable.dot_state_ok else R.drawable.dot_state_grey)
        binding.dot2.setImageResource(if (projection) R.drawable.dot_state_ok else R.drawable.dot_state_grey)
        binding.dot3.setImageResource(if (overlay) R.drawable.dot_state_ok else R.drawable.dot_state_grey)

        // btn4：固定为“打开配置页（脚本/计划）”
        binding.btn4.apply {
            isEnabled = true
            text = "打开配置页（脚本/计划）"
            setBackgroundColor(requireContext().getColor(R.color.brand_purple))
            setTextColor(requireContext().getColor(R.color.white))
        }
    }
}