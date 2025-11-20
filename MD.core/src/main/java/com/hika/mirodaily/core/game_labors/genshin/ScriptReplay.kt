package com.hika.mirodaily.core.game_labors.genshin

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.hika.core.interfaces.Level
import com.hika.core.interfaces.Logger
import com.hika.core.toastLine
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScriptReplay(val context: Context, val scope: CoroutineScope, val logger: Logger, val script: String) {
    val packageName = "com.miHoYo.Yuanshen"
    val className = "com.miHoYo.GetMobileInfo.MainActivity"

    //1. Entry: Open the app
    val intent = Intent(Intent.ACTION_MAIN).apply {
        // what name of app to go in
        setClassName(packageName, className)
        setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    fun start(): Job {
        val job = scope.launch {
            iAccessibilityService?.clearClassNameListeners()
            prepareToEnterGenshin()
        }

        try {
            context.startActivity(intent)
        }catch (_: ActivityNotFoundException){
            logger("Yuanshen Not Found", Level.Erro)
            job.cancel()
            onDestroy()
        }
        return job
    }

    suspend fun prepareToEnterGenshin(){
        if(!ASReceiver.listenToActivityClassNameAsync(className))
            logger("Failed to hear class name, suppose it's already entered.")
        logger("Genshin")
        toastLine("开始重放手势...", context, true)
        iAccessibilityService?.replayScript(script)
    }

    //5. Clean-Up: On Task finished
    private fun onDestroy(){
        iAccessibilityService?.stopReplay()
    }
}