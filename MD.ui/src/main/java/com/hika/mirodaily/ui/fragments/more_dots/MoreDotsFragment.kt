package com.hika.mirodaily.ui.ui.fragments.more_dots

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.hika.core.toastLine
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.chat.GameAiChatBottomSheet
import com.hika.mirodaily.ui.databinding.FragmentMoreDotsBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow

private const val DebugViewClassName = "com.hika.accessibility.debug.TestFloatingViewActivity"

class MoreDotsFragment : Fragment() {

    private var _binding: FragmentMoreDotsBinding? = null
    private val binding get() = _binding!!

    private var floatingWindow: FloatingWindow? = null

    // 从悬浮窗权限页面返回后：尝试直接显示悬浮窗
    private val onOverlaySettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        floatingWindow?.onLaunchResult()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreDotsBinding.inflate(inflater, container, false)
        floatingWindow = FloatingWindow(requireContext(), inflater, onOverlaySettingResult)

        // 自动生成按钮
        renderFeatures(buildFeatureList())

        return binding.root
    }

    private fun buildFeatureList(): List<FeatureSpec> = listOf(
        // --- 权限 ---
        FeatureSpec(
            section = "权限",
            title = "打开无障碍设置",
            desc = "用于启用辅助功能服务"
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        FeatureSpec(
            section = "权限",
            title = "申请投屏权限（Projection）",
            desc = "调用 ProjectionRequesterActivity"
        ) {
            startActivity(Intent().apply {
                setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
            })
        },

        // 悬浮窗
        FeatureSpec(
            section = "悬浮窗",
            title = "打开悬浮窗权限设置",
            desc = "授权后才能显示 Overlay"
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${requireContext().packageName}".toUri()
            )
            toastLine("请打开米洛工坊的悬浮窗权限...", context, true)
            onOverlaySettingResult.launch(intent)
        },
        FeatureSpec(
            section = "悬浮窗",
            title = "显示悬浮窗（Overlay）",
            desc = "显示/拖动悬浮日志窗口"
        ) {
            floatingWindow?.open()
        },

        // 工具
        FeatureSpec(
            section = "工具",
            title = "打开 AI 对话",
            desc = "底部弹窗形式的对话框"
        ) {
            GameAiChatBottomSheet().show(parentFragmentManager, GameAiChatBottomSheet.TAG)
        },
        FeatureSpec(
            section = "工具",
            title = "投屏排错视图（Debug View）",
            desc = "打开 accessibility 模块的测试视图"
        ) {
            startActivity(Intent().apply {
                setClassName(AccessibilityPackageName, DebugViewClassName)
            })
        },

        // 页面
        FeatureSpec(
            section = "页面",
            title = "回到开始页",
            desc = "StartFragment"
        ) {
            findNavController().navigate(R.id.navigation_start)
        },
        FeatureSpec(
            section = "页面",
            title = "打开配置页",
            desc = "ConfigFragment"
        ) {
            findNavController().navigate(R.id.navigation_config)
        },
    )

    private fun renderFeatures(specs: List<FeatureSpec>) {
        val container = binding.featureContainer
        container.removeAllViews()

        val sectionOrder = specs.map { it.section }.distinct()
        for (section in sectionOrder) {
            addSectionTitle(container, section)

            specs.filter { it.section == section }.forEach { spec ->
                addFeatureButton(container, spec)
            }
        }
    }

    private fun addSectionTitle(parent: LinearLayout, title: String) {
        val tv = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setPadding(0, dp(10), 0, dp(6))
        }
        parent.addView(tv)
    }

    private fun addFeatureButton(parent: LinearLayout, spec: FeatureSpec) {
        val btn = MaterialButton(requireContext()).apply {
            text = spec.title
            isAllCaps = false
            setOnClickListener { spec.onClick() }
        }

        val lpBtn = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
        }
        parent.addView(btn, lpBtn)

        if (!spec.desc.isNullOrBlank()) {
            val descTv = TextView(requireContext()).apply {
                text = spec.desc
                textSize = 12f
                alpha = 0.75f
                setPadding(dp(8), dp(2), dp(8), dp(6))
            }
            parent.addView(descTv)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        floatingWindow?.close()
        floatingWindow = null
        _binding = null
        super.onDestroyView()
    }

    data class FeatureSpec(
        val section: String,
        val title: String,
        val desc: String? = null,
        val onClick: () -> Unit
    )
}