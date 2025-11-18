package com.hika.mirodaily.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.ui.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 这里用 camelCase：navView（不是 nav_view）
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // 你的主题是 NoActionBar，就不用 setupActionBarWithNavController 了
        navView.setupWithNavController(navController)
    }

    override fun onDestroy() {
        ASReceiver.instance.get()?.onDestroy()
        super.onDestroy()
    }
}
