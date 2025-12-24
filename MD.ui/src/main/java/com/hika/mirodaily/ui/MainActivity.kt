package com.hika.mirodaily.ui

import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hika.core.toastLine
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.game_labors.genshin.EnterGame
import com.hika.mirodaily.core.game_labors.genshin.FinishTask
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.chat.GameAiChatBottomSheet
import com.hika.mirodaily.ui.databinding.ActivityMainBinding
import com.hika.mirodaily.ui.floatingWindow.FloatingWindow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    public lateinit var floatingWindow: FloatingWindow

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 顶栏
        setSupportActionBar(binding.topBar)
        binding.topBar.setOnMenuItemClickListener {
            val permissions = arrayOf(
                isAccessibilitySettingEnabled() == true,
                iAccessibilityService?.isProjectionStarted() == true,
                floatingWindow.isOverlayPermitted())
            if (permissions.all { it })
                lifecycleScope.launch {
                    floatingWindow.clear()
                    floatingWindow.open()
                    val logger = floatingWindow.logger
                    EnterGame(this@MainActivity, floatingWindow, logger)
                        .launch(FinishTask(floatingWindow, logger))
                }
            else {
                var s = "没有权限: "
                for ((i, p) in permissions.withIndex())
                    if (p == false)
                        s += (i + 1).toString() + ", "
                s += '.'
                toastLine(s)
            }
            true
        }

        // 底部导航
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        // 悬浮 AI 小球点击事件：弹出 AI 对话 BottomSheet
        binding.ivAiBall.setOnClickListener {
            GameAiChatBottomSheet().show(
                supportFragmentManager,
                GameAiChatBottomSheet.TAG
            )
        }
    }


    fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_start_more, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        floatingWindow.clear()
        ASReceiver.instance.get()?.onDestroy()
        super.onDestroy()
    }
}
