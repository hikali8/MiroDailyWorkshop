package com.hika.mirodaily.core.game_labors.genshin;

import android.accessibilityservice.AccessibilityService
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

    val isTesting = false
    val num = if (isTesting) 1L else 10L
    suspend fun f1(){
        delay(100 * num)
        fWindowControll.hide()
        delay(200 * num)
        fWindowControll.open()
    }

    suspend fun f2(){
        delay(100 * num)
        fWindowControll.hide()
        delay(100)
    }

    suspend fun f3(){
        delay(100 * num)
        fWindowControll.open()
    }

    override suspend fun start(){
        logger("开始探测小地图位置...")
        f1()
        logger("成功探测到小地图位置. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP.x, UIBtn.MAP.y)
        f3()
        logger("小地图已消失. 开始探测缩放条加号图标...")
        f1()
        logger("成功探测到缩放条加号图标. 点击 5 次.")
        delay(100 * num)
        fWindowControll.hide()
        for (i in 1..5){
            delay(500)
            ASReceiver.click(UIBtn.MAP_ZOOM_BAR_ADD.x, UIBtn.MAP_ZOOM_BAR_ADD.y)
        }
        fWindowControll.open()
        logger("已完成. 开始探测地区选择图标...")
        f1()
        logger("成功探测到地区选择图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_REGION_SELECT.x, UIBtn.MAP_REGION_SELECT.y)
        f3()
        logger("地区选择图标已消失. 开始探测区域内文字：枫丹 ...")
        f1()
        logger("成功探测到区域内文字：枫丹 . 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_FONTAINE.x, UIBtn.MAP_FONTAINE.y)
        f3()
        logger("区域内文字：枫丹 已消失. 开始探测冒险家协会图标...")
        f1()
        logger("成功探测到冒险家协会图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_FONTAINE_ASSOCIATION.x, UIBtn.MAP_FONTAINE_ASSOCIATION.y)
        f3()
        logger("开始探测探测区域内文字：冒险家协会 ...")
        f1()
        logger("成功探测到区域内文字： 冒险家协会 . 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_TELEPORT_SELECT.x, UIBtn.MAP_TELEPORT_SELECT.y)
        f3()
        logger("区域内文字： 冒险家协会 已消失. 开始探测确认传送按钮图标...")
        f1()
        logger("成功探测到确认传送按钮图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_TELEPORT_CONFIRM.x, UIBtn.MAP_TELEPORT_CONFIRM.y)
        delay(3000)
        fWindowControll.open()
        logger("确认传送按钮图标已消失. 开始探测任务书图标...")
        f1()
        logger("成功探测到任务书图标. 记录任务书坐标. 开始向合成台移动并探测交互图标...")
        f2()


        // 现在在目标点。开始移动。
        var x1 = (UIBtn.WASD.x + UIBtn.WASD.r * 0.03).toFloat()
        var y1 = (UIBtn.WASD.y - UIBtn.WASD.r * 0.9).toFloat()
        ASReceiver.click(x1, y1, 5700)
        delay(2000)
        fWindowControll.open()
        logger("防卡死检测...")
        delay(1000)
        fWindowControll.hide()
        delay(2700)
        fWindowControll.open()
        logger("成功探测到交互图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.INVESTIGATE.x, UIBtn.INVESTIGATE.y)
        f3()
        delay(1000)
        logger("交互图标已消失. 探测到剧情按钮图标, 自动跳过剧情...")
        f2()
        ASReceiver.click(UIBtn.CONVERSATION.x, UIBtn.CONVERSATION.y)
        delay(1000)
        fWindowControll.open()
        logger("剧情按钮图标已消失. 开始探测合成按钮图标...")
        f1()
        logger("成功探测到合成按钮图标. 点击.")
        f2()
        // 这会不点
        ASReceiver.click(UIBtn.CRAFTER_SYNTHESIS.x, UIBtn.CRAFTER_SYNTHESIS.y)
        f3()
        logger("合成图标已消失. 开始探测合成确认按钮图标...")
        f1()
        logger("成功探测到合成确认按钮图标. 按下系统返回键 2 次.")
        for (i in 1..2){
            delay(500)
            iAccessibilityService?.performAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
        delay(3000)
        fWindowControll.open()
        logger("成功探测到任务书图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.BOOK.x, UIBtn.BOOK.y)
        delay(1000)
        f3()
        logger("任务书已消失. 开始探测任务书委托图标...")
        f1()
        logger("成功探测到任务书委托图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.BOOK_COMMISSION.x, UIBtn.BOOK_COMMISSION.y)
        f3()
        logger("任务书委托图标已消失. 开始探测奖励图标...")
        f1()
        logger("成功探测到奖励图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.BOOK_REWARD.x, UIBtn.BOOK_REWARD.y)
        f3()
        logger("奖励领取图标已消失. 按下系统返回键 1 次.")
        iAccessibilityService?.performAction(AccessibilityService.GLOBAL_ACTION_BACK)
        f1()
        logger("成功探测到领取奖励传送图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.BOOK_TELEPORT_REWARD.x, UIBtn.BOOK_TELEPORT_REWARD.y)
        f3()
        logger("领取奖励传送图标已消失. 开始探测确认传送图标...")
        delay(1000)
        f1()
        logger("成功探测到确认传送图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.MAP_TELEPORT_CONFIRM.x, UIBtn.MAP_TELEPORT_CONFIRM.y)
        f3()
        logger("确认传送按钮图标已消失. 开始探测任务书图标...")
        delay(2000)
        f1()
        logger("成功探测到任务书图标. 开始向冒险协会移动并探测交互图标...")
        f2()

        // 向凯瑟琳移动
        x1 = (UIBtn.WASD.x - UIBtn.WASD.r * 0.4).toFloat()
        y1 = (UIBtn.WASD.y - UIBtn.WASD.r * 0.9).toFloat()
        ASReceiver.click(x1, y1, 3200)
        delay(2000)
        fWindowControll.open()
        logger("防卡死检测...")
        delay(1000)
        fWindowControll.hide()
        delay(200)
        fWindowControll.open()
        logger("成功探测到交互图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.INVESTIGATE.x, UIBtn.INVESTIGATE.y)
        f3()
        delay(1000)
        logger("交互图标已消失. 探测到剧情按钮图标, 自动跳过剧情...")
        f2()
        ASReceiver.click(UIBtn.CONVERSATION.x, UIBtn.CONVERSATION.y)
        delay(1000)
        fWindowControll.open()
        logger("剧情按钮图标已消失. 开始探测协会领奖图标...")
        f1()
        logger("成功探测到协会领奖图标. 点击.")
        f2()
        ASReceiver.click(UIBtn.ASSOCIATION_REWARD.x, UIBtn.ASSOCIATION_REWARD.y)
        f3()
        logger("协会领奖图标已消失. 探测到剧情按钮图标, 自动跳过剧情...")
        f2()
        ASReceiver.click(UIBtn.CONVERSATION.x, UIBtn.CONVERSATION.y)
        delay(500)
        ASReceiver.click(UIBtn.CONVERSATION.x, UIBtn.CONVERSATION.y)
        delay(500)
        ASReceiver.click(UIBtn.CONVERSATION.x, UIBtn.CONVERSATION.y)
        delay(1000)
        fWindowControll.open()
        logger("剧情按钮图标已消失. 开始探测主界面图标...")
//        iAccessibilityService?.performAction(AccessibilityService.GLOBAL_ACTION_BACK)
//        delay(1000)
        f1()
        logger("成功探测到任务书图标，回到主界面图标. 任务完成!")

    }

    suspend fun show(){
    }
}
