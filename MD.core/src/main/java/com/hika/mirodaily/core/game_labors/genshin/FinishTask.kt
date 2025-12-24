package com.hika.mirodaily.core.game_labors.genshin;

import android.graphics.PointF
import com.hika.core.interfaces.FloatingWindowControll
import com.hika.core.interfaces.Logger
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.core.iAccessibilityService
import kotlinx.coroutines.delay

// 合成树脂，然后领奖。
// 1. 走近合成台，方式小地图
// 2. 探测到图标出现，点击图标
// 3. 移至最上方，树脂出现后点击树脂
// 4. 检测到滑动钮出现，按住并拖动到最右侧
// 5. 点击合成，点击外侧右上角的叉1，延时再点直到叉2消失
// 6. 打开日常书，点击奖励，点击叉1，延时再点直到叉2消失
// 7. 打开地图，传送至凯瑟琳旁，走近凯瑟琳
// 8. 探测到图标出现，点击图标
// 9. 连点下方剧情，直至“领取...”出现
// 10. 点击，延时再点
// 以上流程缺乏大世界互动。UI交互容易让人厌倦。

// 改为拾取骗骗花。

// 我们现在没有实现小地图定位，仍旧按照定时前进。

class FinishTask(val fWindowControll: FloatingWindowControll, val logger: Logger): ITask {
    suspend fun f1(){
        delay(1000)
        fWindowControll.hide()
        delay(2000)
        fWindowControll.open()
    }

    suspend fun f2(){
        delay(1000)
        fWindowControll.hide()
    }

    override suspend fun start(){
        logger("开始探测小地图位置...")
        f1()
        logger("成功探测到小地图位置. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP.x, UIBtn.MAP.y)
        fWindowControll.open()
        logger("开始探测缩放条位置...")
        f1()
        logger("成功探测到缩放条位置. 滑动.")
        f2()
        ASReceiver.swipe(UIBtn.MAP_ZOOM_BAR.x, UIBtn.MAP_ZOOM_BAR.y, UIBtn.MAP_ZOOM_BAR.x,
            UIBtn.MAP_ZOOM_BAR.y - UIBtn.MAP_ZOOM_BAR.r)
        fWindowControll.open()
        logger("开始探测地区选择图标...")
        f1()
        logger("成功探测到地区选择图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_REGION_SELECT.x, UIBtn.MAP_REGION_SELECT.y)
        fWindowControll.open()
        logger("开始探测文字：枫丹 ...")
        f1()
        logger("成功探测到文字：枫丹 . 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_FONTAINE.x, UIBtn.MAP_FONTAINE.y)
        fWindowControll.open()
        logger("开始探测冒险家协会图标...")
        f1()
        logger("成功探测到冒险家协会图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_SUNSETLAKE.x, UIBtn.MAP_SUNSETLAKE.y)
        fWindowControll.open()
        logger("开始探测探测文字：冒险家协会 ...")
        f1()
        logger("成功探测到文字： 冒险家协会 . 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_SUNSETLAKE.x, UIBtn.MAP_SUNSETLAKE.y)
        fWindowControll.open()
        logger("开始探测确认传送按钮图标...")
        f1()
        logger("成功探测到确认传送按钮图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_TELEPORT_CONFIRM.x, UIBtn.MAP_TELEPORT_CONFIRM.y)
        fWindowControll.open()
        // 现在在目标点。开始移动。



        delay(1000)
        fWindowControll.hide()
        val p = iAccessibilityService?.screenSize
        val w = p?.x ?: 2780
        val h = (p?.y ?: 1264).toFloat()
        var x1 = w * 0.6f
        var x2 = w * 0.3f
        var y1 = h * 0.5f

        ASReceiver.swipeWithDelay(x1, y1, x2, y1, 100)
        x1 = UIBtn.WASD.x
        y1 = (UIBtn.WASD.y - UIBtn.WASD.r * 0.9).toFloat()
        ASReceiver.click(x1, y1, 2000)
        x2 = UIBtn.S.x
        var y2 = UIBtn.S.y
        ASReceiver.click(x2, y2)
        delay(4000)
        ASReceiver.click(x1, y1, 100)
        ASReceiver.click(x2, y2)
        ASReceiver.click(x1, y1, 4000)
        delay(4000)
        ASReceiver.click(x1, y1, 100)
        ASReceiver.click(x1, y1)
        ASReceiver.click(x1, y1, 4000)
        fWindowControll.open()
    }

    suspend fun show(){
    }
}
