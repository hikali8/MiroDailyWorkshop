package com.hika.mirodaily.core.game_labors.genshin

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hika.core.interfaces.FloatingWindowControll
import com.hika.core.interfaces.Level
import com.hika.core.interfaces.Logger
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.helpers.destructivelyClickOnceAppearsText
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 进入游戏，我们需要找到按钮位置

class EnterGame(val context: FragmentActivity, val fWindowControll: FloatingWindowControll, val logger: Logger) {
    val scope = context.lifecycleScope

    val packageName = "com.miHoYo.Yuanshen"
    val className = "com.miHoYo.GetMobileInfo.MainActivity"

    //1. Entry: Open the app
    val intent = Intent(Intent.ACTION_MAIN).apply {
        // what name of app to go in
        setClassName(packageName, className)
        setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    fun launch(task: ITask): Job {
        val job = scope.launch {
            iAccessibilityService?.clearClassNameListeners()
            if(!ASReceiver.listenToActivityClassNameAsync(className))
                logger("Failed to hear class name, suppose it's already entered.")
            fWindowControll.open()
            logger("原神")
            delay(1000)
            // 进入游戏。
            // “点击进入”需要被点击。等待30s
            logger("'点击进入' 需要被点击.")
            delay(1000)
            fWindowControll.hide()
            destructivelyClickOnceAppearsText(
                "点击进入",
                30000,
                10000
            ).let {
                fWindowControll.open()
                if (it > 0) logger("'点击进入' 已点击消失. 开始探测主界面...")
                else if (it == 0) logger("未见 '点击进入'. 开始探测主界面...")
                else {
                    logger("已点击 '点击进入' ，但未见消失.")
                    return@launch
                }
            }
            // 延时10秒(9500ms)
            delay(2000)
            for (i in 1..3){
                fWindowControll.open()
                logger("防卡死")
                delay(1000)
                fWindowControll.hide()
                delay(1500)
            }

            // 找到操作框的位置。计算w,a,s,d的坐标。
            val p = iAccessibilityService?.screenSize ?: return@launch
            UIBtn.updateWH(p.x, p.y)
            fWindowControll.screenWidth = p.x
            // 开始任务。
            logger("成功探测到主界面. ")
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