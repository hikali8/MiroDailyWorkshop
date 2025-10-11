package com.hika.mirodaily.ui.ui.fragments.start

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.Job
import android.database.ContentObserver

class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow
    private var job: Job? = null

    // 监听“无障碍是否开启”的系统设置改变
    private var accObserver: ContentObserver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        floatingWindow = FloatingWindow(requireContext(), inflater, overlayRequestLauncher)

        // 按钮绑定
        binding.btnAccessibility.setOnClickListener {
            (activity as MainActivity).enableAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener {
            (activity as MainActivity).requestProjection()
        }
        binding.btnOverlay.setOnClickListener {
            floatingWindow.open()
        }
        binding.btnStart.setOnClickListener(::onStartClick)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // 注册无障碍设置变化监听（从设置页回来能立刻刷新小圆点/按钮）
        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        accObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateUi()
            }
        }.also {
            requireContext().contentResolver.registerContentObserver(uri, false, it)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onStop() {
        super.onStop()
        accObserver?.let {
            requireContext().contentResolver.unregisterContentObserver(it)
        }
        accObserver = null
    }

    override fun onDestroyView() {
        floatingWindow.close()
        super.onDestroyView()
    }

    // 开始：打开悬浮窗 + 启动自动化
    private fun onStartClick(@Suppress("UNUSED_PARAMETER") view: View) {
        floatingWindow.open()

        if (iAccessibilityService?.isProjectionStarted() != true) {
            Log.d("#0x-SF", "Projection is not yet started")
            updateUi()
            return
        }
        job?.cancel()
        job = DailyCheckIn(requireContext(), lifecycleScope, floatingWindow.logger).start()
    }

    // overlay 权限申请回调
    private val overlayRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        floatingWindow.onLaunchResult()
        updateUi()
    }

    // ------------------ UI 状态 & 权限检测 ------------------

    private fun updateUi() {
        val acc = isAccessibilityEnabledExact()      // 精确检测：必须是你的无障碍服务组件
        val overlay = isOverlayEnabled()
        val projection = iAccessibilityService?.isProjectionStarted() == true

        // 三个小圆点（灰/绿）
        binding.dotAccessibility.setImageResource(
            if (acc) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dotOverlay.setImageResource(
            if (overlay) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dotProjection.setImageResource(
            if (projection) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )

        // “开启测试”按钮：仅要求 无障碍+悬浮窗 就绪即可点
        val ready = acc && overlay
        (binding.btnStart as MaterialButton).apply {
            isEnabled = ready
            if (ready) {
                text = "立即开启"
                setBackgroundColor(requireContext().getColor(R.color.brand_purple))
                setTextColor(requireContext().getColor(R.color.white))
            } else {
                text = "等待权限"
                setBackgroundColor(requireContext().getColor(R.color.btn_disabled_bg))
                setTextColor(requireContext().getColor(R.color.btn_disabled_text))
            }
        }
    }

    /** 精确判断无障碍：必须包含“包名 + 服务类名”的完整扁平化字符串 */
    private fun isAccessibilityEnabledExact(): Boolean {
        val cr = requireContext().contentResolver
        val enabled = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) return false

        val enabledServices = Settings.Secure.getString(
            cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // 与 core 中声明的 Service 完整组件名保持一致
        val target = ComponentName(
            com.hika.mirodaily.core.AccessibilityPackageName,
            com.hika.mirodaily.core.AccessibilityClassName
        ).flattenToString()

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
        while (splitter.hasNext()) {
            if (splitter.next().equals(target, ignoreCase = true)) return true
        }
        return false
    }

    private fun isOverlayEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(requireContext())
        } else true
    }
}
