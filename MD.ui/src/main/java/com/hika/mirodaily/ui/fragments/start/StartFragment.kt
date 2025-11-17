package com.hika.mirodaily.ui.ui.fragments.start

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.Job
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.hika.core.loopUntil
import com.hika.core.toastLine
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.core.START_BROADCAST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow

    // 1. When create the fragment, this must be preceding all of the logic
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)

        // 1-3卡片按钮绑定
        binding.btnAccessibility.setOnClickListener {
            openAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener {
            lifecycleScope.launch { requestProjection() }
        }
        binding.btnOverlay.setOnClickListener {
            floatingWindow.open()
        }
        floatingWindow = FloatingWindow(requireContext(), inflater, onOverlaySettingResult)

        // 4-5卡片及按钮
        setRecordCard()
        setStartCard()

        return binding.root
    }

    // 1.1. 打开无障碍设置
    fun openAccessibilitySetting() {
        if (isAccessibilitySettingEnabled() == true) {
            toastLine("无障碍设置已启用，仍旧打开无障碍设置以便调试时重新开启", context)
        }
        else{
            toastLine("请启用无障碍服务", context, true)
        }
        onAccessibilitySettingResult.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    // 在弹出的无障碍设置窗关闭后会调用
    private val onAccessibilitySettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isAccessibilitySettingEnabled() == true)
            toastLine("无障碍设置已启用", context)
        else
            toastLine("无障碍设置启用失败", context)
        updateUi()
    }

    // 检查无障碍设置是否启用，如果检查失败则为null
    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            context?.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    // 1.2. 请求投屏权限（媒体投影）
    suspend fun requestProjection() {
        if (isAccessibilitySettingEnabled() != true){
            toastLine("无障碍设置未启用，请先启用它", context, true)
            return
        }
        if (iAccessibilityService == null){
            // 无障碍服务未连接到应用
            toastLine("正在通知无障碍服务绑定此应用...此后才可以为它启用投屏...", context, true)
            startReceiver()
            accessibilityBindBroadcast()
            delay(3000)

            if (!loopUntil(3000) { iAccessibilityService != null }){
                toastLine("服务未能绑定应用.", context)
                return
            }
        }
        if (iAccessibilityService!!.isProjectionStarted()) {
            toastLine("投屏权限已经授予。", context)
            return
        }
        toastLine("请在新开的窗口中授予“整个屏幕”的投屏权限", context, true)
        onProjectionRequestingResult.launch(Intent().apply {
            setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
        })
    }

    // 在打开的投屏申请活动从栈顶上关闭后会调用
    private val onProjectionRequestingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (iAccessibilityService?.isProjectionStarted() == true)
            toastLine("投屏权限授予成功", context)
        else
            toastLine("投屏权限授予失败", context, true)
        updateUi()
    }

    // 打开接收器，接收无障碍服务传来的暴露接口。因为无障碍服务接收了系统的控制绑定，是不能再*被*自己的应用绑定的。能主动绑其他服务。
    private fun startReceiver() {
        val res = context?.startService(Intent(context,
            com.hika.mirodaily.core.ASReceiver::class.java)
        )
    }

    // 发出约定的广播，无障碍服务就应该连接接收器。
    private fun accessibilityBindBroadcast() {
        requireContext().sendBroadcast(Intent(START_BROADCAST).apply {
            setPackage(AccessibilityPackageName)
            putExtra("timestamp", System.currentTimeMillis())
        })
    }


    // 1.3. 悬浮窗：全部在FloatingWindow.kt中。floatingWindow.blahblah

    // 当发起的悬浮窗设置窗关闭后就会调用此函数，只能在Activity或Fragment在内存中创建时就初始化，定义在外部
    private val onOverlaySettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        floatingWindow.onLaunchResult()
        updateUi()
    }


    // 4-5. 4-5卡片：下拉框Spinner选择对应过程执行
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
            // ensure if the conditions are all satisfied.
            if (ensurePermissions() == false)
                return@setOnClickListener

            // when be clicked, check the list where it is at, then go to the target app and meanwhile start record.
//            when(selectedApp){
//                "原神" -> {
//
//                }
//                "崩铁" -> {
//
//                }
//                "绝区零" -> {
//
//                }
//            }

            // but we at first test the motion recording.
            Log.w("#0x-SF", "start recording....")
            lifecycleScope.launch(Dispatchers.IO) {
                // Heavy work
                val motions = iAccessibilityService?.recordMotions()
                    ?: return@launch
                Log.w("#0x-SF", motions.toString())
            }

//            Log.w("#0x-SF", "start recording....")
//            Handler(Looper.myLooper()!!).post {
//                val motions = iAccessibilityService?.recordMotions()
//                    ?: return@post
//                Log.w("#0x-SF", motions.toString())
//            }
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
        binding.btnStart.setOnClickListener(::onStartClick)
    }

    fun ensurePermissions(): Boolean{
        if (isAccessibilitySettingEnabled() != true ||
            !floatingWindow.isOverlayPermitted() ||
            iAccessibilityService?.isProjectionStarted() != true) {
            toastLine("缺少权限。请确保无障碍设置、悬浮显示权限、投屏权限都已开启", context, true)
            return false
        }
        return true
    }


    // 2. UI 更新

    // 监听“无障碍是否开启”的系统设置改变
    //Hikali8: it's unnecessary, actually onResume() would be executed everytime the view was showing
//    private var accObserver: ContentObserver? = null

//    override fun onStart() {
//        super.onStart()
//        // 注册无障碍设置变化监听（从设置页回来能立刻刷新小圆点/按钮）
//        val uri = Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
//        accObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
//            override fun onChange(selfChange: Boolean) {
//                updateUi()
//            }
//        }.also {
//            requireContext().contentResolver.registerContentObserver(uri, false, it)
//        }
//    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

//    override fun onStop() {
//        super.onStop()
//        accObserver?.let {
//            requireContext().contentResolver.unregisterContentObserver(it)
//        }
//        accObserver = null
//    }

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

    // ------------------ UI 状态 & 权限检测 ------------------
    private fun updateUi() {
        val acc = isAccessibilitySettingEnabled() == true      // 精确检测：必须是你的无障碍服务组件
        val overlay = floatingWindow.isOverlayPermitted()
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

    // Hikali8: 这个代码不是必要的，就用 isAccessibilitySettingEnabled() 吧
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
}
