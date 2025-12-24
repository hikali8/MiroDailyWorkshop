package com.hika.mirodaily.core.game_labors.genshin;

import com.hika.core.interfaces.Logger
import com.hika.mirodaily.core.ASReceiver
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

class FinishTask(val logger: Logger): ITask {
    override suspend fun start(){
        ASReceiver.click(UIBtn.MAP.x, UIBtn.MAP.y)
        logger("开始探测地区选择图标...")
        delay(1000)
        logger("成功探测到地区选择图标. 点击.")
        ASReceiver.click(UIBtn.MAP_REGION_SELECT.x, UIBtn.MAP_REGION_SELECT.y)
        logger("开始探测文字：蒙德 ...")
        delay(2000)
        logger("成功探测到文字：蒙德 . 点击.")
        ASReceiver.click(UIBtn.MAP_MODSTADT.x, UIBtn.MAP_MODSTADT.y)
        logger("开始探测确认传送按钮图标...")
        delay(1000)
        logger("成功探测到确认传送按钮图标. 点击.")
        ASReceiver.click(UIBtn.MAP_TELEPORT_CONFIRM.x, UIBtn.MAP_TELEPORT_CONFIRM.y)
        logger("开始探测日落湖神像点图标...")
        delay(1000)
        logger("成功探测到日落湖神像点图标. 点击.")
        ASReceiver.click(UIBtn.MAP_SUNSETLAKE.x, UIBtn.MAP_SUNSETLAKE.y)
        // 现在在目标点。开始移动。
    }
}
