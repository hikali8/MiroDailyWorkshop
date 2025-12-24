package com.hika.mirodaily.ui.fragments.start

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
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView

class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)

        // 卡片 1：无障碍
        binding.btnAccessibility.setOnClickListener {
            updateUi()
            openAccessibilitySetting()
        }

        // 卡片 2：媒体投影
        binding.btnProjection.setOnClickListener {
            updateUi()
            lifecycleScope.launch { requestProjection() }
        }

        // 卡片 3：悬浮窗
        binding.btnOverlay.setOnClickListener {
            updateUi()
            floatingWindow.open()
        }
        floatingWindow = FloatingWindow(requireContext(), inflater, onOverlaySettingResult)

        // 卡片 4：跳转 Config 页面
        setCard4()

        return binding.root
    }

    /* ---------------- 无障碍 ---------------- */

    private fun openAccessibilitySetting() {
        if (isAccessibilitySettingEnabled() == true) {
            toastLine("无障碍已开启，仍打开设置以便调试", context)
        } else {
            toastLine("请启用无障碍服务", context, true)
        }
        onAccessibilitySettingResult.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private val onAccessibilitySettingResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isAccessibilitySettingEnabled() == true)
                toastLine("无障碍设置已启用", context)
            else
                toastLine("无障碍设置启用失败", context, true)

            updateUi()
        }

    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    /* ---------------- 媒体投影 ---------------- */

    private suspend fun requestProjection() {
        if (isAccessibilitySettingEnabled() != true) {
            toastLine("请先启用无障碍服务", context, true)
            return
        }

        if (iAccessibilityService == null) {
            toastLine("正在绑定无障碍服务...", context, true)
            startReceiver()
            accessibilityBindBroadcast()

            if (!loopUntil(5000) { iAccessibilityService != null }) {
                toastLine("无障碍服务绑定失败", context, true)
                return
            }
        }

        if (iAccessibilityService!!.isProjectionStarted()) {
            toastLine("投屏权限已授予", context)
            return
        }

        onProjectionRequestingResult.launch(
            Intent().apply {
                setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
            }
        )
        toastLine("请在新窗口中授予“整个屏幕”的投屏权限", context, true)
    }

    private val onProjectionRequestingResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (iAccessibilityService?.isProjectionStarted() == true)
                toastLine("投屏权限授予成功", context)
            else
                toastLine("投屏权限授予失败", context, true)

            updateUi()
        }

    private fun startReceiver() {
        context?.startService(Intent(context, ASReceiver::class.java))
    }

    private fun accessibilityBindBroadcast() {
        requireContext().sendBroadcast(
            Intent(START_BROADCAST).apply {
                setPackage(AccessibilityPackageName)
                putExtra("timestamp", System.currentTimeMillis())
            }
        )
    }

    /* ---------------- 悬浮窗 ---------------- */

    private val onOverlaySettingResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            floatingWindow.onLaunchResult()
            updateUi()
        }

    /* ---------------- 卡片 4：跳转 Config ---------------- */

    private fun setCard4() {
        binding.btn4.setOnClickListener {
            updateUi()
            // 直接使用 mobile_navigation.xml 里的 destination id，会导致原Fragment无法返回
//            findNavController().navigate(R.id.navigation_config)

            // 获取Activity中的底部导航栏引用并模拟点击
            val activity = requireActivity() as MainActivity
            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.nav_view)

            // 模拟点击Config按钮（假设ID是 R.id.navigation_config，赋值更改）
            bottomNav.selectedItemId = R.id.navigation_config
        }
    }

    /* ---------------- 生命周期 & UI ---------------- */

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onDestroyView() {
        floatingWindow.close()
        super.onDestroyView()
    }

    private fun updateUi() {
        val acc = isAccessibilitySettingEnabled() == true
        val projection = iAccessibilityService?.isProjectionStarted() == true
        val overlay = floatingWindow.isOverlayPermitted()

        binding.dot1.setImageResource(
            if (acc) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dot2.setImageResource(
            if (projection) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dot3.setImageResource(
            if (overlay) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )

        // 第 4 卡按钮始终可点（配置页独立）
        binding.btn4.apply {
            isEnabled = true
            text = "打开配置页（脚本/计划）"
            setBackgroundColor(requireContext().getColor(R.color.brand_purple))
            setTextColor(requireContext().getColor(R.color.white))
        }
    }
}