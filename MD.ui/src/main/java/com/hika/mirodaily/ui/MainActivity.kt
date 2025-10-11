package com.hika.mirodaily.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.hika.core.loopUntil
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.core.START_BROADCAST
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

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


    // 1. 打开无障碍设置
    fun enableAccessibilitySetting() {
        if (isAccessibilitySettingEnabled() == true) {
            Toast.makeText(this, "Accessibility Setting Enabled", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "请启用无障碍服务", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(
            ComponentName(AccessibilityPackageName, AccessibilityClassName).flattenToString()
        )
    }

    // 2. 请求投屏权限（媒体投影）
    fun requestProjection() {
        if (iAccessibilityService != null) {
            launchProjection()
            return
        }
        Toast.makeText(this, "Binding to accessibility service...", Toast.LENGTH_SHORT).show()
        Log.d("#0x-MA", "Not receiving accessibility connection. Try to bind service...")

        startConnector()
        accessibilityBindBroadcast()

        lifecycleScope.launch {
            if (loopUntil { iAccessibilityService != null }) {
                launchProjection()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to bind accessibility service...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startConnector() {
        val intent = Intent(this@MainActivity, com.hika.mirodaily.core.ASReceiver::class.java)
        val res = startForegroundService(intent)
        Log.d("#0x-MA", "Tried to start connector with: $res")
    }

    private fun accessibilityBindBroadcast() {
        val intent = Intent(START_BROADCAST).apply {
            setPackage(AccessibilityPackageName)
            putExtra("timestamp", System.currentTimeMillis())
        }
        this.sendBroadcast(intent)
    }

    private fun launchProjection() {
        if (iAccessibilityService!!.isProjectionStarted()) {
            Toast.makeText(this, "All permission completed.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent().apply {
            setClassName(AccessibilityPackageName, ProjectionRequesterClassName)
        }
        startActivity(intent)
    }
}
