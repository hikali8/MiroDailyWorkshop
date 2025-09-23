package com.hika.mirodaily.core.game_labors.hoyolab

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import com.hika.core.aidl.accessibility.ParcelableSymbol
import com.hika.core.aidl.accessibility.ParcelableText
import com.hika.core.interfaces.Logger
import com.hika.core.interfaces.Level
import com.hika.core.loopUntil
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.R
import com.hika.mirodaily.core.data_extractors.findAll
import com.hika.mirodaily.core.data_extractors.containsAny
import com.hika.mirodaily.core.data_extractors.matchSequence
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/** series of things to do:
 *  1. hoyolab jump.  "com.mihoyo.hyperion.main.HyperionMainActivity" (should not appear)
 *  2. ad page close. "com.mihoyo.hyperion.splash.SplashActivity" -> click "跳过" (should not appear)
 *  3. teen-protection window close. "com.mihoyo.hyperion.main.HyperionMainActivity" -> click "我知道了"
 *  4. another ad page close. "com.mihoyo.hyperion.main.popup.HomePopupDialogActivity" -> click "X" (below "点击了解")
 *  5. another small popout. "com.mihoyo.hyperion.main.HyperionMainActivity" -> click "去看看吧"
 *  ---- Main Page has been open. -----
 *  6.
 */


class DailyCheckIn(val context: Context, val scope: CoroutineScope, val logger: Logger) {
    //0. entry
    fun start(): Job {
        return openApp()
    }

    //1. Open the app
    val intent = Intent(Intent.ACTION_MAIN).apply {
        setClassName("com.mihoyo.hyperion", "com.mihoyo.hyperion.main.HyperionMainActivity")
        setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK)
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    private inline fun openApp(): Job {
        val job = scope.launch {
            iAccessibilityService?.clearClassNameListeners()
            prepareToEnterHyperionMainActivity()
        }

        try {
            context.startActivity(intent)
        }catch (_: ActivityNotFoundException){
            logger("HyperionMainActivity Not Found", Level.Erro)
            job.cancel()
            onTaskFinished()
        }
        return job
    }

    suspend fun prepareToEnterHyperionMainActivity(){
        if(!ASReceiver.listenToActivityClassNameAsync(
                "com.mihoyo.hyperion.main.HyperionMainActivity"))
            logger("Failed to hear class name, suppose it's already entered.")
        logger("HyperionMainActivity")
        delay(500)

        // 1. close ad pages
        val adPages = arrayOf(context.getString(R.string.hyl_我知道了), context.getString(R.string.hyl_去看看吧))
        var location: List<ParcelableSymbol>? = null
        while (loopUntil(3000) {
            val text = ASReceiver.getTextInRegionAsync(null)
            location = text.containsAny(adPages)
            !location.isEmpty()
        }){
            logger("Saw $adPages.")
            ASReceiver.clickLocationBox(location!!.first().boundingBox!!)
            delay(500)
        }
        logger("All ADs closed.")

        findNaviBar()
    }

    // 2. find location of navi-bar and go to the leftest position
    var width by Delegates.notNull<Int>()
    var height by Delegates.notNull<Int>()
    var barHeight by Delegates.notNull<Float>()
    suspend fun findNaviBar(){
        val screenSize = iAccessibilityService?.screenSize
        if (screenSize == null){
            logger("No screen-size gotten.", Level.Erro)
            return
        }
        width = screenSize.x
        height = screenSize.y

        // swipe upward
        ASReceiver.swipe(
            PointF(width / 2F, height / 2F),
            PointF(width / 2F, height * 0.2F)
        )
        delay(500)

        // swipe downward
        ASReceiver.swipe(
            PointF(width / 2F, height / 2F),
            PointF(width / 2F, height * 0.8F),
            50
        )
        delay(500)


        // find navi-bar and swipe to the leftest
        val regionLimitation = Rect(0, 0, width, (height * 0.2).toInt())
        val naviWords = arrayOf("崩坏", "原神", "星穹", "铁道", "绝区", "大别野", "大別野", "因缘")
        var text: ParcelableText? = null
        var location: List<ParcelableSymbol>? = null

        if (loopUntil {
            text = ASReceiver.getTextInRegionAsync(regionLimitation)
            location = text.containsAny(naviWords)
            !location.isEmpty()
        }){
            val box = location!!.first().boundingBox!!
            barHeight = (box.top + box.bottom) / 2 .toFloat()
            logger("Saw tab strings. Lastest seen:\n${text!!.text}")
        }else{
            logger("Failed to see any tab strings. Roughly estimate the location.", Level.Erro)
            barHeight = height * 0.1F
            logger("Lastest seen: ${text!!.text}.", Level.Erro)
        }
        // from 10% -> 80%
        ASReceiver.swipe(
            PointF(width * 0.1F, barHeight),
            PointF(width * 0.8F, barHeight),
            50
        )
        logger("swiped")
        delay(500)

        // click the leftest
        ASReceiver.click(width * 0.1F, barHeight)
        delay(200)

        findCheckInButton()
    }

    // 3. find check-in button
    suspend fun findCheckInButton(){
        // wait for "加载中" to disappear
        val keyword = "加载中"

        var location: List<ParcelableSymbol>? = null
        var text: ParcelableText? = null
        loopUntil(5000) {
            text = ASReceiver.getTextInRegionAsync()
            location = text!!.matchSequence(keyword)
            location!!.isEmpty()
        }

        logger("swip downward to find check-in button.")
        // swipe downward
        ASReceiver.swipe(
            PointF(width / 2F, height / 2F),
            PointF(width / 2F, height * 0.8F),
            80
        )
        delay(500)

        // click check-in button
        val hyl_签到 = context.getString(R.string.hyl_签到)
        val upperScreen = Rect(0, 0, width, height / 2)

        if(loopUntil {
            text = ASReceiver.getTextInRegionAsync(upperScreen)
            location = text.matchSequence(hyl_签到)
            !location.isEmpty()
        }){
            logger("Saw $hyl_签到. Click the button. Params condition:")
            logger(text!!.text)

            scope.launch {
                if(!ASReceiver.listenToActivityClassNameAsync(
                        "com.mihoyo.hyperion.web2.MiHoYoWebActivity"))
                    return@launch
                delay(4000)
                // click all of 天
                val hyl_天 = context.getString(R.string.hyl_天)
                var locations: List<List<ParcelableSymbol>>? = null
                if (loopUntil(interval = 200) {
                    val text = ASReceiver.getTextInRegionAsync()
                    locations = text.findAll(hyl_天)
                    !locations.isEmpty()
                }){
                    var str = "Saw $hyl_天. Click each of them, and go back, go to the next tab. locations: $locations\n"
                    str += "click: "
                    assert(locations != null)
                    for (location in locations!!){
                        ASReceiver.clickLocationBox(location.first().boundingBox!!)
                        str += location.first().boundingBox!!.toString()
                        delay(50)
                    }
                    logger(str, Level.Warn)
                }else{
                    logger("Not Saw $hyl_天. Go back and go to the next tab.", Level.Erro)
                }
                delay(1000)

                context.startActivity(intent)
                delay(500)

                goToNextTab()
            }

            ASReceiver.clickLocationBox(location!!.first().boundingBox!!)
            return
        }else{
            logger("Not Saw $hyl_签到. Go to the next tab. Params condition:")
            logger(text!!.text)
        }
        goToNextTab()
    }

    // 4. go to the next tab.
    var leftTabs = 10   // At most 10 tabs it can go
    suspend fun goToNextTab(){
        if (leftTabs < 1) {
            logger("Check-in finished")
            return
        }
        leftTabs--

        // swipe up
        ASReceiver.swipe(
            PointF(width / 2F, height / 2F),
            PointF(width / 2F, height * 0.2F)
        )
        delay(500)

        // swipe left
        ASReceiver.swipe(
            PointF(width * 0.8F, barHeight),
            PointF(width * 0.2F, barHeight)
        )
        delay(500)

        findCheckInButton()
    }


    //5. Clean-Up: On Task finished
    private fun onTaskFinished(){
        iAccessibilityService?.clearClassNameListeners()
        iAccessibilityService?.cancelAllTextGetting()
    }
}