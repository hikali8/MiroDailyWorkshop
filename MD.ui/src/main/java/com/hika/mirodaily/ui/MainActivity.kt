package com.hika.mirodaily.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.ui.chat.GameAiChatBottomSheet
import com.hika.mirodaily.ui.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    override fun onDestroy() {
        ASReceiver.instance.get()?.onDestroy()
        super.onDestroy()
    }
}
