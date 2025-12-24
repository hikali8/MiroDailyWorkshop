package com.hika.mirodaily.core.game_labors.genshin

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.interfaces.Level
import com.hika.core.interfaces.Logger
import com.hika.core.loopUntil
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.data_extractors.matchSequence
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 进入游戏，我们需要找到按钮位置

class EnterGame(val context: Context, val scope: CoroutineScope, val logger: Logger) {
    val packageName = "com.miHoYo.Yuanshen"
    val className = "com.miHoYo.GetMobileInfo.MainActivity"

    //1. Entry: Open the app
    val intent = Intent(Intent.ACTION_MAIN).apply {
        // what name of app to go in
        setClassName(packageName, className)
        setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    fun start(task: ITask): Job {
        val job = scope.launch {
            iAccessibilityService?.clearClassNameListeners()
            if(!ASReceiver.listenToActivityClassNameAsync(className))
                logger("Failed to hear class name, suppose it's already entered.")
            logger("Genshin")

            delay(1000)
            // 进入游戏。
            // “点击进入”需要被点击。等待30s
            val keyword = "点击进入"
            var location: List<ParcelableSymbol>? = null
            var text: ParcelableText? = null
            if (loopUntil(30000) {
                    text = ASReceiver.getTextInRegion()
                    location = text!!.matchSequence(keyword)
                    location?.isNotEmpty() != true
                }){
                logger("click the '点击进入'.")
                ASReceiver.clickLocationBox(location!!.first().boundingBox!!)
            }else{
                logger("Not see '点击进入'.")
            }
            // 延时5秒
            delay(5000)
            // 找到操作框的位置。计算w,a,s,d的坐标。
            val p = iAccessibilityService?.screenSize ?: return@launch
            UIBtn.updateCoordinate(p.x, p.y)
            // 开始任务。
            task.start()
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



    //5. Clean-Up: On Task finished
    private fun onDestroy(){
        iAccessibilityService?.stopReplay()
        iAccessibilityService?.clearClassNameListeners()
    }
}