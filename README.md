MiroDailyWorkshop · 米洛日常工坊，一个基于计算机视觉技术，意图让安卓PVE环境变得更好的项目。



## 🚧施工中 / Under Construction

目前实现了自动米游社签到！改进点：

1. 添加调试信息悬浮窗 

   a. 实现情况：目前有了初级的显示，外观有待打磨，同时考虑把之前能加进来的提示都加进来

2. 设置能暂停自动化的快捷键（音量键+电源键组合键之类的）

3. 添加调试信息存储（日志记录）

4. 添加配置文件读写

5. 完善UI界面

主要问题（优先）：

1. 有概率不识别某些特殊颜色对比下的字符。考虑实现一个debug窗口，显示截图情况。
 + 情况：已实现，观察颜色发现原因是libyuv库大小端序和安卓是相反的，RGBA应为ABGR。
 + 余波：谷歌OCR模块仍有概率漏识别字，考虑叠加多次识别结果，去除重复

2. ncnn加载模型(.bin)失败，加载.param是使用load_param_mem()反而成功。
 + 情况：过了几天load_model又运行成功。可能当时IDE有缓存未清理。成功后大改了代码。
 + 余波：大概率不识别对象，但模型加载成功。考虑在PC上验证模型完备性。修改置信度或最小概率阈值。

3. 实现操作录入与保存
 + 情况：注意到FRep已经实现手势录制与重放，接下来将会考虑参照，同时实现arxiv1801.06503。
 + 2025.11.18: 经过一个星期的尝试，证实用安卓应用捕捉用户在屏幕上的操作是不现实的。需要再做一个PC端adb操作捕获，参照达人的代码，然后拷贝到应用中。操作文件直接用adb push进去就行了。



## 🙌参与者 / Committers

| 参与者                                                  | 起始时间点 |
| :------------------------------------------------------ | :--------- |
| [hikali8](https://github.com/hikali8)                   | 2025.9.9   |
| [OhOopsKazuha](https://github.com/OhOopsKazuha)         | 2025.9.9   |
| [zhishibujinnaozi](https://github.com/zhishibujinnaozi) | 2025.9.9   |



## 工程大体结构和说明 / Project Basic Structure & Description

本工程有两个应用：**accessibility**和**MD.ui**。**accessibility**用于实现无障碍服务，**MD.ui**用于实现主界面。

+ 说明：本来无障碍服务和主界面一起实现，但由于安卓系统会在应用进程终止后（比如手动划除、主动退出、长时间运行产生一些bug等等），清理掉应用的无障碍权限，为了避免每次打开应用都需要重新打开无障碍服务的麻烦，本工程将无障碍服务单独分开了实现，当做一个新应用。由于新应用内只有一个无障碍服务，没有应用入口，仅作为一个功能性独立模块，结构简单，启动后由系统管理，所以基本上不会有终止的情况。同时它和主界面间仅通过有限的接口进行联系，避免出错，也避免了每次启动都需要手动打开无障碍服务开关。

本工程有两个库：**MD.core**和**common**。**MD.core**是主界面所用到的核心代码库，**common**是所有模块的公用代码库。

+ 说明：**MD.core**是实现ui的过程中明显不需要在前台考虑的代码，独立出去以整理结构；**common**是所有模块都要使用的公用代码库，包括常用的简单函数，和应用间交流的接口（AIDL）。

本工程使用两种主要编程语言：**Kotlin**和**C++**。

+ 说明：**Kotlin** 是经由Google改造的简化版Java语言，** C++ **是安卓原生语言。二者都是本工程接触安卓应用层的桥梁，由Google提供。

本工程大部分技术来源：**Google API**文档。

+ 说明：安卓机器学习是通过**Google ML Kit**实现（Google Machine-Learning Kit）（[ML Kit  |  Google for Developers](https://developers.google.cn/ml-kit/guides?hl=zh-cn)），安卓屏幕捕获是通过**Media projection**实现（[Media projection  |  Android media  |  Android Developers](https://developer.android.google.cn/media/grow/media-projection?hl=en)），等等。

本工程仅作学习用途。

