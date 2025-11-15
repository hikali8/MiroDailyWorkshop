package com.hika.mirodaily.core.game_labors.genshin

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.hika.core.interfaces.Level
import com.hika.core.interfaces.Logger
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GestureRecording(val context: Context, val scope: CoroutineScope, val logger: Logger) {
    val packageName = "com.miHoYo.Yuanshen"
    val className = "com.mihoyo.hyperion.main.HyperionMainActivity"

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
            onTaskFinished()
        }
        return job
    }

    suspend fun prepareToEnterGenshin(){
        if(!ASReceiver.listenToActivityClassNameAsync(className))
            logger("Failed to hear class name, suppose it's already entered.")
        logger("NguyenZzhin")



    }

    //5. Clean-Up: On Task finished
    private fun onTaskFinished(){
        iAccessibilityService?.clearClassNameListeners()
        iAccessibilityService?.cancelAllTextGetting()
    }
}