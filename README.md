MiroDailyWorkshop · 米洛日常工坊，一个基于计算机视觉技术，意图让安卓PVE游戏环境自动化测试变得更好的项目。



## 🚧施工中 / Under Construction

目前实现了自动米游社签到！改进点：

1. 添加调试信息悬浮窗 

   a. 实现情况：目前有了初级的显示，外观有待打磨，同时考虑把之前能加进来的提示都加进来

2. 设置能暂停自动化的快捷键（音量键+电源键组合键之类的）

3. 添加调试信息存储（日志记录）

4. 添加配置文件读写

5. 完善UI界面



## 🙌参与者 / Committers

| 参与者                                                  | 起始时间点 |
| :------------------------------------------------------ | :--------- |
| [hikali8](https://github.com/hikali8)                   | 2025.9.9   |
| [OhOopsKazuha](https://github.com/OhOopsKazuha)         | 2025.9.9   |
| [zhishibujinnaozi](https://github.com/zhishibujinnaozi) | 2025.9.9   |



## 工程大体结构和说明 / Project Structure & Descriptions

本工程有两个应用：accessibility和MD.ui。accessibility用于实现无障碍服务，MD.ui用于实现主界面。

+ 说明：本来无障碍服务和主界面一起实现，但由于安卓系统会在应用进程终止后（比如手动划除、主动退出、长时间运行产生一些bug等等），清理掉应用的无障碍权限，为了避免每次打开应用都需要重新打开无障碍服务的麻烦，本工程将无障碍服务单独分开了实现，当做一个新应用。由于新应用内只有一个无障碍服务，没有应用入口，仅作为一个功能性独立模块，结构简单，启动后由系统管理，所以基本上不会有终止的情况。同时它和主界面间仅通过有限的接口进行联系，避免出错，也避免了每次启动都需要手动打开无障碍服务开关。

本工程有两个库：MD.core和common。MD.core是主界面所用到的核心代码库，common是所有模块的公用代码库。

+ 说明：MD.core是实现ui的过程中明显不需要在前台考虑的代码，独立出去以整理结构；common是所有模块都要使用的公用代码库，包括常用的简单函数，和应用间交流的接口（AIDL）。

本工程使用两种主要编程语言：Kotlin和C++。

+ 说明：Kotlin是经由Google改造的简化版Java语言，C++是安卓原生语言。二者都是本工程接触安卓应用层的桥梁，由Google提供。

本工程大部分技术来源：Google API文档。

+ 说明：安卓机器学习是通过Google ML Kit实现（Google Machine-Learning Kit）（[ML Kit  |  Google for Developers](https://developers.google.cn/ml-kit/guides?hl=zh-cn)），安卓屏幕捕获是通过Media projection实现（[Media projection  |  Android media  |  Android Developers](https://developer.android.google.cn/media/grow/media-projection?hl=en)），等等。

本工程仅作学习用途。