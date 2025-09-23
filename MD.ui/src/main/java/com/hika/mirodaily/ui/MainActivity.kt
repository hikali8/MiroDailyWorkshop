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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hika.mirodaily.core.AccessibilityClassName
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.core.Helpers
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

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_start, R.id.navigation_config, R.id.navigation_more_dots
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    // 1. Enable the Accessibility Setting
    fun enableAccessibilitySetting() {
        // 1.1 检查无障碍服务是否已能用
        if (isAccessibilitySettingEnabled() == true) {
            Toast.makeText(this, "Accessibility Setting Enabled",
                Toast.LENGTH_SHORT).show()
            return
        }
        // 1.2 open the accessibility settings
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Toast.makeText(this, "请启用无障碍服务", Toast.LENGTH_LONG).show()
        //然后我们是要等返回窗口后，才通知：已经打开了权限，或者复选框自己检测设置之类
    }

    private fun isAccessibilitySettingEnabled(): Boolean? {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(ComponentName(AccessibilityPackageName,
            AccessibilityClassName
        ).flattenToString())
    }

    // 2. Request Media Projection Permission while Accessibility Service initialized
    fun requestProjection(){
        if (iAccessibilityService != null){
            launchProjection()
            return
        }
        Toast.makeText(this, "Binding to accessibility service...",
            Toast.LENGTH_SHORT).show()
        Log.d("#0x-MA","Not receiving accessibility connection. Trying to bind " +
                "accessibility service...")
        Log.d("#0x-MA", "Before that we will open the service connector first.")
        startConnector()

        accessibilityBindBroadcast()
        lifecycleScope.launch {
            if ( Helpers.loopUntil { iAccessibilityService != null } ){
                launchProjection()
            }
            else
                Toast.makeText(this@MainActivity, "Failed to bind accessibility service...",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun startConnector(){
        val intent = Intent(this@MainActivity, com.hika.mirodaily.core.ASReceiver::class.java)
        val res = startForegroundService(intent)
        Log.d("#0x-MA", "Tried to start connector with: $res")
    }

    private fun accessibilityBindBroadcast(){
        val intent = Intent(START_BROADCAST).apply {
            setPackage(AccessibilityPackageName) // 指定目标包名
            putExtra("timestamp", System.currentTimeMillis())
        }
        this.sendBroadcast(intent)
    }

    private fun launchProjection(){
        if (iAccessibilityService!!.isProjectionStarted()){
            Toast.makeText(this, "All permission completed.",
                Toast.LENGTH_SHORT).show()
            return
        }


        val intent = Intent().apply {
            setClassName(AccessibilityPackageName,
                ProjectionRequesterClassName)
        }
        startActivity(intent)
    }
}