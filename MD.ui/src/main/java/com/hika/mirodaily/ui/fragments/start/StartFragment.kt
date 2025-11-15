package com.hika.mirodaily.ui.ui.fragments.start

import android.content.ComponentName
import android.content.Intent
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
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.Job
import android.database.ContentObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.hika.core.loopUntil
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.core.START_BROADCAST
import kotlinx.coroutines.launch

class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow

    // 1. When create the fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        floatingWindow = FloatingWindow(requireContext(), inflater, overlayRequestLauncher)

        // 1-3按钮绑定
        binding.btnAccessibility.setOnClickListener {
            enableAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener {
            requestProjection()
        }
        binding.btnOverlay.setOnClickListener {
            floatingWindow.open()
        }
        binding.btnStart.setOnClickListener(::onStartClick)

        // 4-5按钮
        setRecordCard()
        setStartCard()

        return binding.root
    }


    val appList = listOf("原神", "崩铁", "绝区零")
    var selectedApp = ""
    val processesList = listOf("手势1", "手势2")
    var selectedGesture = ""
    fun setRecordCard(){
        // to record
        binding.spinnerApps.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, appList)
        binding.spinnerApps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedApp = appList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedApp = ""
            }
        }
        binding.btnRecord.setOnClickListener {
            // when be clicked, check the list where it is at, then go to the target app and meanwhile start record.
            when(selectedApp){
                "原神" -> {

                }
                "崩铁" -> {

                }
                "绝区零" -> {

                }
            }
        }
    }

    fun setStartCard(){
        // to proceed
        binding.spinnerTargets.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, processesList)
        binding.spinnerTargets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedGesture = processesList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedGesture = ""
            }
        }
    }

    // 1.1. 打开无障碍设置
    fun enableAccessibilitySetting() {
        if (isAccessibilitySettingEnabled() == true) {
            Toast.makeText(context, "Accessibility Setting Enabled", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(context, "请启用无障碍服务", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    // 1.2. 请求投屏权限（媒体投影）
    fun requestProjection() {
        if (iAccessibilityService != null) {
            launchProjection()
            return
        }
        Toast.makeText(context, "Binding to accessibility service...", Toast.LENGTH_SHORT).show()
        Log.d("#0x-MA", "Not receiving accessibility connection. Try to bind service...")

        startConnector()
        accessibilityBindBroadcast()

        lifecycleScope.launch {
            if (loopUntil { iAccessibilityService != null }) {
                launchProjection()
            } else {
                Toast.makeText(
                    context,
                    "Failed to bind accessibility service...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startConnector() {
        val intent = Intent(context, com.hika.mirodaily.core.ASReceiver::class.java)
        val res = context?.startForegroundService(intent)
        Log.d("#0x-MA", "Tried to start connector with: $res")
    }

    private fun accessibilityBindBroadcast() {
        val intent = Intent(START_BROADCAST).apply {
            setPackage(AccessibilityPackageName)
            putExtra("timestamp", System.currentTimeMillis())
        }
        requireContext().sendBroadcast(intent)
    }

    private fun launchProjection() {
        if (iAccessibilityService!!.isProjectionStarted()) {
            Toast.makeText(context, "All permission completed.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent().apply {
            setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
        }
        startActivity(intent)
    }

    // 2. UI updating

    // 监听“无障碍是否开启”的系统设置改变
    private var accObserver: ContentObserver? = null

    override fun onStart() {
        super.onStart()
        // Hikali8: it's unnecessary, actually onResume() would be executed everytime the view was showing
//        // 注册无障碍设置变化监听（从设置页回来能立刻刷新小圆点/按钮）
//        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
//        accObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
//            override fun onChange(selfChange: Boolean) {
//                updateUi()
//            }
//        }.also {
//            requireContext().contentResolver.registerContentObserver(uri, false, it)
//        }
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
    private var job: Job? = null
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
        val acc = isAccessibilitySettingEnabled() == true      // 精确检测：必须是你的无障碍服务组件
        val overlay = isOverlayEnabled()
        val projection = iAccessibilityService?.isProjectionStarted() == true

        // 三个小圆点（灰/绿）
        binding.dot1.setImageResource(
            if (acc) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dot2.setImageResource(
            if (projection) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )
        binding.dot3.setImageResource(
            if (overlay) R.drawable.dot_state_ok else R.drawable.dot_state_grey
        )

        // “开启测试”按钮：仅要求 无障碍+悬浮窗 就绪即可点
        val ready = acc && overlay
        binding.btnStart.apply {
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

    // Hikali8: it's unnecessary, actually you can just use StringList.contains()
//    /** 精确判断无障碍：必须包含“包名 + 服务类名”的完整扁平化字符串 */
//    private fun isAccessibilityEnabledExact(): Boolean {
//        val cr = requireContext().contentResolver
//        val enabled = Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
//        if (!enabled) return false
//
//        val enabledServices = Settings.Secure.getString(
//            cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
//        ) ?: return false
//
//        // 与 core 中声明的 Service 完整组件名保持一致
//        val target = ComponentName(
//            com.hika.mirodaily.core.AccessibilityPackageName,
//            com.hika.mirodaily.core.AccessibilityClassName
//        ).flattenToString()
//
//        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
//        while (splitter.hasNext()) {
//            if (splitter.next().equals(target, ignoreCase = true)) return true
//        }
//        return false
//    }

    private fun isOverlayEnabled(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }
}
