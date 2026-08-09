MiroDailyWorkshop · 米洛日常工坊，一个意图基于计算机视觉技术，在安卓端实现应用自动化的项目。



## 🚧目前情况 / Currrent Condition

1. 具有调试信息悬浮窗 

   a. 实现情况：目前有了初级的显示，外观有待打磨，同时考虑把之前能加进来的提示都加进来

2. （TODO）设置能暂停自动化的快捷键（音量键+电源键组合键之类的）

3. （TODO）添加调试信息存储（日志记录）

4. 添加配置文件读写

5. （TODO）完善UI界面

主要问题（优先）：

1. 有概率不识别某些特殊颜色对比下的字符。考虑实现一个debug窗口，显示截图情况。
 + 情况：已实现，观察颜色发现原因是libyuv库大小端序和安卓是相反的，RGBA应为ABGR。
 + 余波：谷歌OCR模块仍有概率漏识别字，考虑叠加多次识别结果，去除重复

2. ncnn加载模型(.bin)失败，加载.param是使用load_param_mem()反而成功。
 + 情况：过了几天load_model又运行成功。可能当时IDE有缓存未清理。成功后大改了代码。
 + 余波：大概率不识别对象，但模型加载成功。考虑在PC上验证模型完备性。修改置信度或最小概率阈值。

3. （TODO）实现操作录入与保存
 + 情况：注意到FRep已经实现手势录制与重放，接下来将会考虑参照，同时实现arxiv1801.06503。
 + 2025.11.18: 经过一个星期的尝试，证实用安卓应用捕捉用户在屏幕上的操作是不现实的。需要再做一个PC端adb操作捕获，再参照达人的代码，然后传输到应用中。操作文件直接用adb push进去就行了。



## 🙌参与者 / Committers

| 参与者                                                  | 起始时间点 |
| :------------------------------------------------------ | :--------- |
| [hikali8](https://github.com/hikali8)                   | 2025.9.9   |
| [OhOopsKazuha](https://github.com/OhOopsKazuha)         | 2025.9.9   |
| [zhishibujinnaozi](https://github.com/zhishibujinnaozi) | 2025.9.9   |



## 项目大体结构和说明 / Project Basic Structure & Description

本项目有两个应用：**accessibility**和**MD.ui**。**accessibility**用于实现无障碍服务，**MD.ui**用于实现主界面。

+ 说明：本来无障碍服务和主界面一起实现，但由于安卓系统会在应用进程终止后（比如手动划除、主动退出、长时间运行产生一些bug等等），清理掉应用的无障碍权限，为了避免每次打开应用都需要重新打开无障碍服务的麻烦，本工程将无障碍服务单独分开了实现，当做一个新应用。由于新应用内只有一个无障碍服务，没有应用入口，仅作为一个功能性独立模块，结构简单，启动后由系统管理，所以基本上不会有终止的情况。同时它和主界面间仅通过有限的接口进行联系，避免出错，也避免了每次启动都需要手动打开无障碍服务开关。

本项目有两个库：**MD.core**和**common**。**MD.core**是主界面所用到的核心代码库，**common**是所有模块的公用代码库。

+ 说明：**MD.core**是实现ui的过程中明显不需要在前台考虑的代码，独立出去以整理结构；**common**是所有模块都要使用的公用代码库，包括常用的简单函数，和应用间交流的接口（AIDL）。

本项目使用两种主要编程语言：**Kotlin**和**C++**。

+ 说明：**Kotlin** 是经由Google改造的简化版Java语言，** C++ **是安卓原生语言。二者都是本工程接触安卓应用层的桥梁，由Google提供。

本项目大部分技术来源：**Google API**文档。

+ 说明：安卓机器学习是通过**Google ML Kit**实现（Google Machine-Learning Kit）（[ML Kit  |  Google for Developers](https://developers.google.cn/ml-kit/guides?hl=zh-cn)），安卓屏幕捕获是通过**Media projection**实现（[Media projection  |  Android media  |  Android Developers](https://developer.android.google.cn/media/grow/media-projection?hl=en)），等等。

本项目仅作学习用途。


## 演示视频 / Demo Video:

通过网盘分享的文件：MiroDaily演示视频
链接: https://pan.baidu.com/s/13B4N3GZzd_KT5JFtCzP1Xw?pwd=4mr9 提取码: 4mr9 
--来自百度网盘超级会员v3的分享


## 附文 / Appendix：
### 1. 项目开发文档（部分）
MiroDailyWorkshop（米洛日常工坊）项目开发文档





软件版本：v1.0
文档版本：v1.0
修订日期：2025.12.27
编写人：xxx
审核人：xxx




修订记录
版本号	制定/修改人	时间	修订内容
V1.0	xxx	2025.12.27	新建文档

 
目录
项目基本信息	2
一、开发环境与配置说明	2
二、项目结构与模块划分	3
三、业务功能模块实现说明	4
四、AI智能模块实现说明	6
4.1模块概述	6
4.2模型来源	6
4.3模型训练过程	7
4.4模型部署与调用	7
4.5模型与业务的集成	8
五、测试说明	8
六、部署与上线说明	9
七、项目运行截图与样例展示	10
八、项目总结与未来优化	11
附录	12

 
项目基本信息
本项目名称为MiroDailyWorkshop（米洛日常工坊），定位是面向Android PVE游戏环境的自动化测试与自动化流程平台。本项目基于计算机视觉技术、改善安卓 PVE 环境，以“让重复而机械的流程变得更可控、更省力”为直接目标，把常见的日常任务拆解成可执行步骤，并且把执行过程放在可被观测、可被暂停、可被复用的框架当中，从而让自动化不再只是“黑盒脚本跑一遍”，而是能够被持续迭代与验证的体系化能力。
近期迭代中，本项目进一步把屏幕旋转适配纳入统一坐标体系，并增强脚本重放的执行边界，使自动化在横竖屏切换与不同分辨率下更易保持稳定。
本项目开发团队成员包括xxx（公开仓库需隐藏），项目启动时间为2025-09-09。由于本项目仍处于持续迭代阶段，功能层面已经落地“自动米游社签到”，同时围绕调试悬浮窗、日志存储、配置读写以及操作录入等方向持续补齐可用性能力，使其逐步具备从“单一脚本”走向“可管理脚本集合”的应用形态。同时，本项目补充了与游戏启动及基础日常操作相关的任务入口，使“任务自动化”与“脚本重放”两条路径更便于联调与演示。
一、开发环境与配置说明
本项目主要开发语言选用Kotlin，并在需要高性能图像处理与推理调用的路径上引入C++（通过JNI封装ncnn检测器），同时在数据标注与模型导出等辅助环节保留Python脚本用于训练与验证链路的实现。Android构建体系采用Gradle Kotlin DSL，版本管理使用Version Catalog（libs.versions.toml），其中Kotlin版本为2.0.21，Android Gradle Plugin为8.12.0，编译目标与运行约束在应用侧体现为compileSdk=36、targetSdk=36、minSdk=26，Java编译兼容等级为11，CMake版本在原生构建中配置为3.22.1。
本项目UI侧以AndroidX与Material组件为主线，并且结合Navigation、Lifecycle、Fragment等Jetpack组件组织页面与生命周期逻辑；依赖层面包含kotlinx-coroutines、kotlinx-serialization、OkHttp，以及用于屏幕文本识别的Google ML Kit（text-recognition-chinese）。AI相关依赖在本项目中呈现为两条路径，一条路径是ML Kit的端侧文本识别能力，另一条路径是ncnn端侧推理能力与自定义C++检测器封装，二者共同服务于“屏幕感知—目标定位—动作执行”的自动化闭环。在当前版本，本项目把屏幕旋转与坐标变换能力下沉到common模块，使OCR结果、检测结果以及脚本触点坐标能够在同一套旋转规则下完成对齐。
运行环境侧，本项目默认面向Android 8.0及以上设备，并且依赖无障碍权限、悬浮窗权限与MediaProjection屏幕捕获授权，其中屏幕捕获服务在Android 14及以上还涉及前台服务类型声明与对应权限配置。
二、项目结构与模块划分
本项目根目录下形成了面向Android端主功能的多模块结构，并且把“主界面”“无障碍执行”“核心业务逻辑”“公共通信与数据结构”拆分为清晰边界，从而让权限链路、进程生命周期与跨进程接口更易被控制。目录层级在语义上可概括为MD.ui、accessibility、MD.core、common四个Android模块，同时保留MotionRecord与drafts作为训练与录制的辅助资产集合，前者用于PC侧通过adb录制触摸事件并导出脚本，后者用于目标检测数据集、训练脚本与ncnn 导出验证等工作流沉淀。
表1 MiroDailyWorkshop-master模块分布
模块名称	说明
MD.ui	主界面应用模块，applicationId=com.hika.mirodaily.ui
accessibility	无障碍与屏幕捕获应用模块，applicationId=com.hika.accessibility
MD.core	主界面依赖的核心业务库模块
common	公共库模块，包含AIDL接口、跨模块数据结构以及坐标旋转算法
MotionRecord	Python工具，基于adb getevent录制触摸并导出脚本
drafts	数据集与训练脚本资产，含IconLabeling相关训练与导出验证

本项目模块之间的数据流主要围绕一条主链路展开，即主界面侧发起业务动作或脚本重放请求，经由common模块定义的AIDL接口把请求发送至无障碍应用模块；无障碍模块在需要感知时启动屏幕捕获并产出图像帧，再分别调用OCR与目标检测能力得到文本块或目标框，最终把识别结果返回主界面侧的业务逻辑，由业务逻辑决定后续点击、滑动、等待或终止流程。为了使这一链路具备可组合性，本项目把“动作执行能力”与“感知能力”都收敛在IAccessibilityService这一跨进程接口之内，从而让MD.core与MD.ui的业务代码可以用同一套调用方式完成屏幕读取与手势执行。
近期迭代把旋转适配嵌入识别链路中间层，使区域识别请求会先按当前rotation换算到屏幕捕获坐标系再执行识别，而返回的locationBox再回正到业务侧坐标系，从而减少横竖屏切换带来的坐标漂移。
图1 数据链路图
MD.ui（页面与配置）/ MD.core（业务逻辑）
        │  AIDL（common）
        ▼
accessibility（无障碍服务 + MediaProjection + CV）
        │
        ├─ OCR：ML Kit text-recognition-chinese
        └─ 检测：ncnn + JNI(ncnn_detector) + IconLabeling.param/bin

三、业务功能模块实现说明
本项目业务侧的核心功能一方面落在“可运行的自动化任务”，另一方面落在“可管理、可复用的脚本执行机制”，二者共同决定本项目能否从单次演示走向可持续使用。在自动化任务层面，本项目已经实现“自动米游社签到”，其实现路径并不依赖网络接口直接调用第三方服务，而是采取在目标应用界面内进行页面跳转、关键文本定位、按钮触发与结果校验的方式完成签到流程，从而把任务执行建立在“界面可见即可操作”的一致性基础之上。该功能在代码侧集中体现在MD.core的game_labors/hoyolab/DailyCheckIn.kt中，逻辑会在启动目标Activity后，循环进行页面状态判断，并借助getTextInRegion返回的OCR结果对“知道了”“签到”“签到成功”等关键文本序列进行匹配，同时结合matchSequence对symbols序列进行对齐回算，从而把匹配到的符号或文本块的locationBox作为点击依据并触发点击，以此推动流程进入下一个状态。
在脚本执行机制层面，本项目把脚本定义为一段可解析的字符串序列，并在无障碍侧提供replayScript(script:String)入口完成脚本重放，使其可以承载更通用的“Down/Move/Up/wait/NEXT”手势片段组合。主界面侧在ConfigFragment中实现了脚本管理与执行计划能力，脚本文件存储在getExternalFilesDir(null)所指向的外部文件目录，支持粘贴保存、选择预览与删除操作，同时把“分组—计划”以JSON字符串写入SharedPreferences，以ScriptPlanStore进行序列化与反序列化管理。由于脚本执行具有耗时与中断需求，本项目在执行计划时使用协程在IO线程调度，近期迭代中，replayScript在AIDL层面以同步调用形式对外暴露，无障碍侧会在内部协程执行完脚本后再返回，从而使计划串行执行更适宜以“上一段脚本执行完成”为边界来组织，而不再主要依赖估算总时长与延时等待来推断完成状态。
本项目跨模块接口在common中以AIDL的方式固化，核心接口IAccessibilityService覆盖了屏幕状态监听、目标检测、文本识别、点击滑动、全局按键动作以及脚本重放等能力，其中getObjectInRegion(detectorName,region,confidence)以detectorName支持多检测器扩展，getTextInRegion(region)用于在可选区域内返回文本识别结果，replayScript(script)与stopReplay()用于控制脚本执行生命周期。由于本项目的状态与配置主要通过本地文件与SharedPreferences管理，当前并不存在单独的数据库与表结构设计，业务侧的持久化重点落在脚本文件与计划JSON的一致性维护，并且通过pruneMissingScripts对“磁盘不存在脚本”与“计划引用不存在分组”进行同步清理，以减少配置漂移导致的运行失败。
本项目在业务实现过程中已经暴露出若干典型问题与对应解决路径，其中“安卓应用内捕捉用户触屏操作以生成脚本”被验证为不可行方向，进而推动本项目增加MotionRecord工具，转而通过PC侧adbgetevent录制触摸并导出脚本文件，再通过拷贝或推送方式进入设备端使用。调试层面为了提升可观测性，本项目新增悬浮窗日志能力，由FloatingWindow负责在TYPE_APPLICATION_OVERLAY窗口层展示带时间戳的日志行，使自动化过程中的关键分支与异常更容易被回溯定位。
四、AI智能模块实现说明
本项目AI模块的定位是为自动化提供可靠的“视觉入口”，其目标并不追求泛化到任意场景的视觉理解，而是围绕签到等目标任务的界面元素进行可控的文本与图标识别，使业务逻辑能够在不同分辨率、不同界面状态下仍然通过“识别—定位—动作”完成闭环。
4.1模块概述
本项目AI模块在能力层面由OCR与目标检测两部分组成，OCR负责从屏幕截图中提取中文文本块与其位置框，目标检测负责从屏幕截图中定位与签到相关的关键图标或按钮区域，从而在“只依赖像素输入”的条件下提供可用于点击的坐标信息。OCR路径在accessibility模块内以GoogleOCRer为实现入口，最终调用MLKit的TextRecognition并返回ParcelableText；目标检测路径以NCNNDetector为入口，通过JNI调用原生库ncnn_detector完成推理，并将结果封装为DetectedObject数组返回。为了适配屏幕捕获的图像格式与推理输入约束，本项目在ImageHandler中实现了对ImageReader输出图像的处理逻辑，既支持把图像转换为NV21供OCR使用，也支持裁剪指定Rect区域供检测与识别在局部范围内进行，从而让识别调用在性能与稳定性之间取得更可控的平衡。
近期版本把屏幕旋转纳入识别链路的统一处理范围，getTextInRegion在进入OCR前会读取rotation并把region换算到屏幕捕获坐标系后再执行识别，识别返回的ParcelableText在序列化过程中也会结合屏幕尺寸与rotation对locationBox进行回正，使业务侧能够在横竖屏切换时仍以一致的坐标语义使用识别结果。
4.2模型来源
本项目OCR能力来源于Google ML Kit的text-recognition-chinese组件，该组件以端侧库形式集成并在设备本地完成推理，使用时不依赖外部模型下载与云端调用，从而更适合无障碍服务在前台持续运行的场景。目标检测模型在本项目中以ncnn模型文件的形式随应用打包，模型文件在accessibility/src/main/assets下以IconLabeling.param与IconLabeling.bin的形式存在，并由原生检测器通过AssetManager读取初始化。与该检测模型相对应的训练与验证资产在drafts/DailyCheckInRecognize中保留，数据集以images/train、images/val划分，并且在classes.txt与data.yaml中给出了5个类别标签，标注工具信息在notes.json中记录为LabelStudio，这些信息共同构成了“可回溯的训练来源”。
4.3模型训练过程
本项目在drafts/Daily CheckIn Recognize/train_scripts中提供了训练脚本train.py，并且采用ultralytics YOLO作为训练框架，训练参数在脚本中明确给出为imgsz=640、epochs=200、batch=8、lr0=0.2、lrf=0.1、dropout=0.1、warmup_epochs=3，同时在数据增强方面设置translate=0.3与scale=0.5，并且有意关闭了hsv_h、hsv_s、hsv_v、degrees、shear等增强项以减少颜色与几何扰动带来的不确定性。该训练流程在脚本层面还包含export(format="ncnn")的导出调用，用于把训练得到的模型转换为ncnn可用格式，并且在testNCNN.py中给出了基于ncnn Python绑定与OpenCV的推理验证思路，用于在PC侧对模型输出与坐标变换逻辑进行可视化检查。
4.4模型部署与调用
本项目AI模块以端侧本地部署为主导方式，OCR以ML Kit库调用的形式嵌入accessibility模块，目标检测以ncnn推理库加自定义JNI封装的方式嵌入accessibility模块，两条链路都不依赖Docker、云服务或外部API，因此在离线条件下仍可以完成“屏幕感知—识别—动作执行”的闭环。为了让识别结果与手势执行在不同旋转状态下保持同一套坐标语义，无障碍侧在启动屏幕捕获后保留zeroW、zeroH作为捕获坐标系的基准，并且借助getRotation与rotateWHto把当前屏幕尺寸换算为前台坐标系下的screenSize，后续区域识别与脚本重放都以该screenSize作为换算参考。
目标检测在Kotlin侧由NCNNDetector负责加载ncnn_detector原生库并初始化IconLabeling.param/bin，推理入口为detect(recognizable,confidence)，其中confidence用于在结果过滤时提供更稳定的阈值控制；OCR在Kotlin侧由GoogleOCRer把待识别图像组织为InputImage并调用recognizer.process，识别结果在封装为ParcelableText之前会先调用ParcelableTextBase.setRectRotation写入rotation以及screenSize信息，从而让boundingBox在跨进程序列化时自动完成rotateRectTo换算。对外调用接口统一通过AIDL收敛，业务侧通过IAccessibilityService.getTextInRegion(region)、getObjectInRegion(detectorName,region,confidence)等方法发起请求，并且配合cancelAllTextGetting与cancelAllObjectGetting对未完成的异步任务进行回收；在脚本侧，replayScript在AIDL层面采用非oneway调用，accessibility侧会在内部runBlocking等待重放线程join返回，这使主界面能够把“调用返回”当作脚本结束的边界，同时也要求业务在协程或IO线程中发起调用以避免阻塞主线程。
4.5模型与业务的集成
本项目把AI模块输出作为业务流程的分支条件与坐标来源，并且在关键步骤上尽量采用“识别文本或目标—得到locationBox—再触发点击”的方式，而不是把坐标写死在脚本里，从而在分辨率变化、旋转切换以及界面细节微调时仍能保持较高的可用性。以自动签到为例，DailyCheckIn会先调用iAccessibilityService.screenSize取得当前前台坐标系下的宽高，并据此确定滑动条、上半屏区域等基础布局参数；当需要定位按钮或提示语时，它会借助ASReceiver.getTextInRegion(region)拿到ParcelableText，再通过matchSequence在textBlocks的symbols序列中匹配关键字，并直接取匹配到的ParcelableSymbol.boundingBox交给clickLocationBox随机落点点击，这样既降低了OCR空格与换行带来的索引偏差，也让点击位置更贴近真实用户行为。脚本重放路径则把动作序列作为字符串交给IAccessibilityService.replayScript执行，二者在集成层面形成互补关系，前者偏向“感知驱动的自适应执行”，后者偏向“可复用动作序列的直接执行”，并且都统一依靠同一套AIDL连接与权限链路完成调用。
五、测试说明
在当前版本，屏幕旋转被纳入识别与手势链路的统一处理范围，因此回归测试不仅要覆盖签到主流程本身，还需要在横屏、竖屏以及旋转切换的组合条件下确认getTextInRegion返回的locationBox、以及replayScript换算后的点击路径都能稳定命中目标区域；同时，由于replayScript在AIDL层面改为同步返回，脚本执行的起止边界更清晰，异常卡点也更便于在日志与片段级时间轴上进行定位。
本项目测试策略以端到端联调验证为主，因为核心能力涉及无障碍权限、屏幕捕获授权、跨进程AIDL通信与端侧推理，这类链路往往难以完全通过纯单元测试覆盖。在项目结构中仍保留了标准的Android单元测试与仪器测试目录以满足基本的可扩展性需求，但现阶段更关键的验证集中在真实设备或模拟器上对“连接无障碍服务—启动屏幕捕获—识别文本或目标—执行点击滑动—观察界面变化”的闭环测试。AI模块功能测试在仓库中体现为drafts下的训练集与验证集样例图片、训练脚本与ncnn推理验证脚本，并且在accessibility模块中保留了与识别相关的调试能力，例如通过区域识别与多次识别策略排查漏识别问题。
测试过程中已经遇到并记录了若干典型问题，其中OCR漏识别在特定颜色对比下更易出现，排查后与图像通道顺序及格式转换相关，libyuv大小端序与安卓通道顺序差异导致RGBA/ABGR混淆；目标检测侧也曾出现ncnn加载.bin失败但加载.param成功的异常情况，后续在环境变化后恢复可用，推断与构建缓存或资源加载状态有关。性能层面虽然尚未形成系统的量化压测报告，但识别链路的瓶颈位置相对明确，主要集中在屏幕帧获取、像素格式转换、OCR推理与检测推理四个环节，因此区域裁剪、阈值控制与调用频率控制在当前阶段被作为更直接可行的优化手段。
六、部署与上线说明
本项目部署形态以本地安装为主，安装对象包括MD.ui主界面应用一个APK，承担前台交互与配置管理，以及后台无障碍执行、屏幕捕获与端侧识别能力。由于无障碍服务需要系统层授权，本项目在启动流程上要求先完成无障碍开关启用，再完成悬浮窗权限授予，并在需要屏幕识别时由ProjectionRequesterActivity发起MediaProjection授权请求，授权成功后无障碍侧会以前台服务方式运行并展示通知，以满足系统对屏幕捕获的合规要求。主界面侧通过绑定ASReceiver服务接收来自无障碍侧的IAccessibilityService连接，从而使业务调用具备稳定入口，并且在连接缺失时会以提示方式阻止脚本执行或任务启动，以减少“无效调用导致的误判”。
版本发布在当前代码中以1.0作为主版本标识，更新内容以功能迭代为主线推进，已落地内容覆盖自动签到、悬浮窗日志与脚本管理雏形，待补齐内容聚焦暂停快捷键、日志存储、配置读写完善与UI细节优化等方向。由于本项目仍处于施工阶段，上线策略更偏向功能验证与学习用途，并且依赖真实设备权限配置，因此更适合作为开发团队内部测试与演示使用。
七、项目运行截图与样例展示
本项目运行界面在主界面侧以MainActivity与多个Fragment组织，其中StartFragment负责引导完成权限检查与服务连接流程，并且在需要时可拉起悬浮窗用于实时观察日志输出，ConfigFragment负责脚本文件管理与分组计划执行，形成“准备—执行—回看”的基本闭环。无障碍侧在后台运行AccessibilityCoreService，并在屏幕捕获开启后以通知提示其处于可捕获状态，同时在识别与点击过程中把关键日志输出到悬浮窗，便于确认执行序列是否与预期一致。
 
图2 主界面
 
图3 运行状况截图
AI模块的输入输出在跨进程返回结构上具有明确形式，其中OCR输出以ParcelableText表示，包含多个textBlocks，每个block给出文本内容与locationBox；目标检测输出以DetectedObject表示，包含objectName、locationBox与confidence。为了便于理解，输出结构可被概括为如下形态，其中locationBox用于直接驱动clickLocationBox的坐标选择，而confidence用于在阈值控制下过滤不稳定结果。
八、项目总结与未来优化
本项目阶段性成果集中体现在两点，一点是把“无障碍执行—屏幕捕获—端侧识别—业务驱动点击”的链路打通并用于自动米游社签到，另一点是把“脚本可管理、可重放”的能力在主界面侧以分组与计划方式实现，使自动化从单一流程走向可扩展的动作集合。项目难点主要来自Android权限与生命周期约束、跨进程通信与服务保活、以及端侧识别在复杂UI下的稳定性问题，而当前实现通过拆分无障碍应用、收敛AIDL接口、引入悬浮窗日志与保留训练验证资产等方式，为后续迭代提供了可持续的结构基础。
未来优化方向首先落在识别稳定性与性能控制上，例如通过多次OCR结果叠加与去重降低漏识别概率，通过更合理的裁剪区域与调用节奏降低推理开销，并且在目标检测侧进一步完善模型评估记录与阈值策略，使IconLabeling的可用性具备更可解释的依据。其次落在可用性体验上，例如补齐暂停快捷键、增强日志落盘与回放能力、完善配置文件读写与UI的可视化引导，从而让脚本执行从“可运行”走向“更易用、更可控”。最后落在兼容性上，本项目当前原生库目录以arm64-v8a为主，后续可考虑补齐更多ABI支持与更严谨的构建与发布流程，以覆盖更广泛的设备环境。
附录
本项目代码地址以GitHub仓库为准，地址为https://github.com/hikali8/MiroDailyWorkshop。AI相关外部文档参考以Google ML Kit文档与Android MediaProjection文档为主线，具体以Google for Developers与Android Developers官方说明为准；本项目训练与导出相关脚本、数据集与验证样例均已在仓库drafts目录中保留，可用于复现训练参数、类别定义与端侧推理验证路径。




### 2. 使用说明书（部分）


基于AI视觉识别的安卓游戏自动化测试平台


软件名称：MicroDaily（脚本与自动化管理系统）
软件版本：v1.0
文档版本：v1.0
修订日期：2025.12.25
编写人：xxx
审核人：xxx、xxx

修订记录
版本号	制定/修改人	时间	修订内容
V1.0	xxx	2025.12.25	新建文档
V1.1	xxx	2025.12.27	润色引言，修订系统功能介绍
 
引言
在以“每日循环玩法”为核心的长线运营游戏中（如《原神》），玩家需要反复完成一系列固定任务，如每日委托、树脂消耗、材料采集与合成。以“甜甜花收集”为例：甜甜花属于常见素材，分布广、单次采集价值低，但在烹饪/任务/合成等环节需求频繁，其合成产物可以用于回复游戏体力、精力，具有典型的“高频低价值操作”属性，适合作为日常辅助脚本的示例场景。这类任务具有高频、重复、流程稳定的特点，需要玩家每日上线重复进行这些操作，长期执行这些任务需要投入大量的时间成本、人力操作，容易让玩家产生操作疲劳感，降低玩家的游戏体验。
 
图表 1 原神甜甜花采集路线图（以星落湖为例）
因此，市场上存在对“日常任务辅助工具”的需求：希望借助“工具”，通过流程化、半自动或自动化的方式，降低玩家重复操作成本，提高游戏日常效率，改善体验稳定性（减少漏做、走错路线、重复跑图，“路痴”、新手友好）。

我们希望开发一款这样的“日常辅助工具”，辅助玩家执行游戏日常任务，为大量困顿于日常任务的玩家提供一种解决方案。对上班族、学生党来说，他们未必有充足的时间、精力每天上线游戏完成任务，他们倾向于使用这类工具来代替他们每天上线收集游戏资源用于后续的升级、抽卡等玩法，使他们不落后于大部分玩家水平。对于任务驱动的玩家，他们更喜欢专注于对世界的探索，而对日常重复的操作更容易感受到疲乏，工具可以帮助他们执行这些“枯燥”的但是必须完成的任务。

1.1 编写目的
本文档用于说明 MiroDaily Android 项目的系统功能、运行环境、安装方式及具体操作方法，帮助使用者正确、高效地使用本系统，同时也作为课程设计/综合实训项目的技术说明文档。

1.2 目标读者
普通用户
课程设计评阅教师
对游戏自动化与无障碍技术感兴趣的学习者或二次开发者

1.3 软件适用范围与目标
本软件主要用于：
•	移动端脚本化任务管理
•	自动化操作执行（点击、滑动等）
•	基于屏幕内容的智能识别、决策与辅助操作

项目目标是实现“脚本回放 + 屏幕文字识别（OCR）+ 目标检测（ncnn）+ 无障碍手势执行”的一体化闭环。适用于 Android 平台的学习、演示与实验性游戏自动化场景。



 
目录
引言	I
1.1 编写目的	II
1.2 目标读者	II
1.3 软件适用范围与目标	II
第 1 章 系统简介	1
1.1 系统功能简介	1
1.2 核心技术与智能功能	1
1.3 主要使用场景与目标用户	3
第 2 章 系统运行环境	4
2.1 硬件要求	4
2.2 软件环境	4
2.3 网络要求	4
2.4 第三方依赖说明	4
第 3 章 安装与登录	5
3.1 安装步骤	5
3.2 数据与模型初始化	5
3.3 登录方式	5
3.4 支持平台	5
第 4 章 系统功能与操作说明	6
4.1 常规功能模块说明	6
4.1.1 脚本计划管理模块（ScriptPlan / ScriptGroup / ScriptPlanStore）	6
4.1.2 无障碍连接桥模块（MD.core.ASReceiver）	7
4.2 智能功能模块说明	7
4.2.1 屏幕捕获模块（MediaProjection → RGBA 帧）	8
4.2.2 RGBA → NV21 转换模块（JNI + libyuv）	8
4.2.3 NV21 裁剪模块（保证 UV 对齐的裁剪算法）	9
4.2.4 OCR 中文识别模块（Google ML Kit）	10
4.2.5 OCR 关键词定位算法模块（matchSequence / containsAny）	10
4.2.6 目标检测模块（ncnn + JNI）	11
4.2.7 脚本回放引擎模块（CSV → Gesture）	12
4.2.8 窗口/Activity 监听模块（用于“等待页面加载完成”）	13
第 5 章 用户角色与权限说明	14
第 6 章 数据导入与导出说明	15
6.1 数据导入	15
6.2 数据导出	15
第 7 章 常见问题与解答	16
第 8 章 系统限制与已知问题	17
第 9 章 技术支持与联系方式	18
第 10 章 附录	19
10.1 术语解释	19





 
第 1 章 系统简介
1.1 系统功能简介
MiroDaily 是一款基于 Android 平台的脚本与自动化管理系统，利用使用屏幕识别技术，通过无障碍服务实现对手游界面的自动化任务执行操作。

系统由 主界面应用（MD.ui） 与 无障碍服务应用（accessibility） 组成：
•	主界面：脚本计划管理、启动/停止、配置、调试信息展示等
•	无障碍服务：系统级手势执行、屏幕捕获、OCR 文本识别、目标检测、事件监听、脚本回放

通过本项目的设计与实现，完成了一个集系统服务调用、图像处理、智能识别与自动化执行于一体的 Android 自动化系统。项目在工程结构与算法实现层面具备一定复杂度和完整性。
 
1.2 核心技术与智能功能
系统主要采用以下关键技术：
•	AIDL 跨进程通信机制
•	Android 无障碍服务（Accessibility Service）
•	MediaProjection 屏幕截取
•	OCR 文本识别（基于 ML Kit）
•	目标检测
•	脚本回放引擎：自动化手势执行（点击、滑动）

具体技术细节如下（对应到代码模块/类名）：
1.	跨应用/跨进程通信：AIDL
•	common 模块定义 AIDL：IAccessibilityService.aidl、IASReceiver.aidl 等
•	主程序 MD.core.ASReceiver 作为 Service 接收无障碍服务回连并持有 IAccessibilityService 实例

2.	无障碍手势执行：AccessibilityService.dispatchGesture
•	AccessibilityCoreService.click() / swipe() 使用 GestureDescription + Path 下发手势轨迹，支持点击与滑动

3.	屏幕捕获：MediaProjection + VirtualDisplay + ImageReader(RGBA_8888)

•	ProjectionRequesterActivity 发起授权
•	AccessibilityServicePart2_Projection.startProjection() 创建前台服务通知并建立虚拟屏幕
•	ImageHandler 通过 ImageReader.acquireLatestImage() 获取 RGBA 帧缓冲

4.	OCR 文本识别：Google ML Kit 中文识别
•	GoogleOCRer 使用 TextRecognition.getClient(ChineseTextRecognizerOptions)
•	输入格式：NV21（由 RGBA 转换）
•	输出包装：ParcelableText（将 ML Kit 的 Text/Block/Line/Element/Symbol 全部 Parcelable 化）

5.	目标检测：ncnn（C++ JNI）
•	NCNNDetector.kt 调用 native：init() / detect() / release()
•	ncnn_detector.cpp：模型从 assets 加载 .param/.bin，预处理 resize+padding+normalize，推理后输出 bbox + class + confidence，并做简单重叠去重与坐标反算

6.	脚本回放引擎：CSV 文本脚本 → 手势序列（支持屏幕旋转）
•	AccessibilityServicePart5_ScriptReplay.ScriptReplayer
•	解析 Down/Move/Up/wait/NEXT 五类指令，打包为 Gesture(含多 Stroke)，再 dispatchGesture() 回放
•	通过 rotateCoordinate() 自动适配屏幕旋转（0/90/180/270）
1.3 项目创新性介绍
采用双应用+ AIDL 架构 提高了系统稳定性
JNI + libyuv 保证图像处理实时性
OCR+检测+脚本回放构建完整自动化闭环
考虑使用场景，我们算法设计针对移动端性能进行优化，使用了轻量化的算法模型，减小计算负担。

1.4 主要使用场景与目标用户
Android 应用普通使用者
课程设计评阅教师
对游戏自动化与无障碍技术感兴趣的学习者


第 2 章 系统运行环境
2.1 硬件要求
Android设备
CPU：ARM64 架构
内存：≥ 4GB（用于推理与OCR）
存储空间：≥ 500MB（用于存储模型、日志、脚本文件）
2.2 软件环境
操作系统：推荐Android 10.0及以上
开发工具：Android Studio
开发语言：项目语言使用Kotlin / Java，底层推理与图像转换模型采用C++
2.3 网络要求
基本功能不依赖网络，能够在离线环境下使用。
2.4 第三方依赖说明
Google ML Kit OCR
ncnn 推理框架（C++，assets 模型文件）
libyuv（用于 RGBA→NV21 的高性能颜色空间转换）
第 3 章 安装与登录
3.1 安装步骤
1.	在 Android 设备上安装 MiroDaily 主程序
2.	安装配套的无障碍服务应用（accessibility 模块）
3.	打开系统设置（可以通过软件“启动页——无障碍服务”打开），手动启用无障碍服务权限
在首次使用OCR功能前需要启动屏幕共享授权。
3.2 数据与模型初始化
系统首次运行时自动初始化本地配置
OCR 模型由系统自动加载，无需手动导入
3.3 登录方式
当前版本无需账号登录，所有的配置信息均保存在设备本地。
3.4 支持平台
Android平台
PC 端脚本录制工具，如Python + adb
第 4 章 系统功能与操作说明
 
4.1 常规功能模块说明
4.1.1 脚本计划管理模块（ScriptPlan / ScriptGroup / ScriptPlanStore）
功能描述
•	管理“脚本分组（group）”与“计划顺序（plan）”
•	支持保存/加载/兼容旧字段/清理无效脚本引用

数据结构
•	ScriptGroup(id, name, scripts[])：分组信息与脚本列表
•	ScriptPlan(groups[], plan[])：groups 为分组集合；plan 为分组 id 的执行顺序

保存算法（JSON 序列化）
•	groups：写入数组，每项包含 id/name/scripts
•	plan：写入分组 id 数组

兼容与清理策略
•	兼容旧字段键名：若 scripts 不存在，则尝试读取历史错误键名 com/hika/mirodaily/ui/scripts
•	清理缺失脚本：pruneMissingScripts(plan, existedScriptNames)
	将 group.scripts 中不在磁盘/集合中的脚本名过滤掉
	将 plan 中不存在的 groupId 过滤掉

操作步骤（用户视角）
1.	打开主界面 → 进入脚本管理
2.	新建分组 / 添加脚本 / 调整计划顺序
3.	点击保存 → 应用下次启动自动恢复

4.1.2 无障碍连接桥模块（MD.core.ASReceiver）
功能描述
无障碍服务应用无法像普通 Activity 那样由主应用直接控制，因此主程序提供 ASReceiver 作为“接收器”：
•	接收无障碍服务通过 AIDL 回连的 IAccessibilityService
•	对外提供统一调用：click / swipe / getTextInRegion / listenToActivityClassNameAsync 等

关键接口
•	IAccessibilityService.click(PointF, startTime, duration)
•	IAccessibilityService.swipe(PointF, PointF, startTime, duration)
•	IAccessibilityService.getTextInRegion(Rect?) : ParcelableText
•	IAccessibilityService.setListenerOnActivityClassName(...)

点击策略（随机点选）
•	clickLocationBox(Rect box)：在矩形范围内随机取点，避免点到边缘导致失效（提升鲁棒性）
4.2 智能功能模块说明
本项目“智能”主要体现在 屏幕内容理解（OCR/检测）+ 脚本引擎决策，不依赖云端大模型。以下是具体介绍。
4.2.1 屏幕捕获模块（MediaProjection → RGBA 帧）
功能说明
通过 MediaProjection 捕获屏幕，创建 VirtualDisplay 把画面输出到 ImageReader 的 Surface，再读取 RGBA 帧。获取的信息将作为后续的OCR识别和目标检测的输入。

实现流程（精准步骤）
1.	ProjectionRequesterActivity 发起系统屏幕捕获授权请求：MediaProjectionManager.createScreenCaptureIntent()
2.	用户同意后返回 resultCode + data
3.	AccessibilityCoreService.startProjection(resultCode, data)
4.	服务提升为前台服务（必须展示通知）
5.	MediaProjection.createVirtualDisplay(...) 输出屏幕内容到 ImageHandler.surface
6.	ImageHandler.getRecognizable() 使用 ImageReader.acquireLatestImage() 读取最近一帧

帧率/节流算法（interval 控制）
•	默认最小时间间隔ImageHandler.interval = 50ms（约 20 FPS 上限）
•	通过时间戳 expirationTime 判断是否允许更新帧：
	若当前时间currentTime ≥ expirationTime 且没有正在更新的 job，则启动协程取帧；反之不获取新帧
	取帧后更新 expirationTime = now + interval
	该策略避免OCR/检测过于频繁导致CPU占用过高产生卡顿

4.2.2 RGBA → NV21 转换模块（JNI + libyuv）
功能说明
OCR模块ML Kit 的 InputImage.fromByteArray 要求输入为 NV21，而Android屏幕捕获得到的是 RGBA_8888，需要高效、准确的转换。
此外，Kotlin/Java逐像素转换存在效率低的问题。利用native 层可充分利用 SIMD 优化的特点，保证了运行的实时性和系统稳定性。

关键点：字节序与颜色通道问题
•	代码采用libyuv : : ABGRToNV21()
•	原因：在部分设备上 RGBA 的内存排列与期望不同（小端序导致 ABGR 更匹配实际缓冲）
 
转换算法（native 侧）
输入：RGBA 指针 rgbaData、stride、width、height
输出：NV21 Y 平面 + VU 平面
•	dst_y = nv21Buffer
•	dst_vu = nv21Buffer + width*height

4.2.3 NV21 裁剪模块（保证 UV 对齐的裁剪算法）
功能说明
为提高 OCR 速度与准确性，支持只识别屏幕某区域。由于 NV21 的 UV 是 2×2 采样，裁剪必须满足偶数对齐，否则可能产生图像识别错误、识别异常的问题。

裁剪算法步骤
•	裁剪起点(x,y)边界约束：cx = max(x,0), cy = max(y,0)
•	裁剪宽高(w,h)边界约束：cw <= originalWidth - cx，ch <= originalHeight - cy
•	强制偶数对齐，(x,y,w,h)调整为偶数：cx,cy,cw,ch 按偶数对齐（& ~1）
•	拷贝 Y 平面像素数据：逐行 System.arraycopy 拷贝 cw 字节，共 ch 行
•	拷贝 UV 平面数据（高度h/2）：从 ySize + (cy/2)*W + cx 开始，拷贝 cw 字节，共 ch/2 行

意义
•	保证 NV21 结构合法
•	提高OCR稳定性，避免 OCR 输入畸变或崩溃
•	减少无关区域干扰，提高识别准确率

4.2.4 OCR 中文识别模块（Google ML Kit）
功能说明
识别屏幕文字，用于“找到按钮/关键词 → 自动点击”。

输入/输出
输入：NV21 byte[] + width + height
输出（分层文本结构（Block / Line / Element / Symbol））：Text（ML Kit），再包装为 ParcelableText 以便跨进程传输

识别步骤
•	构造 InputImage.fromByteArray()
•	调用 ML Kit OCR 客户端进行识别
•	将识别结果封装为可序列化数据结构
•	通过 AIDL 回传至主程序

4.2.5 OCR 关键词定位算法模块（matchSequence / containsAny）
功能说明：由于ML Kit text 中可能包含空格/换行等不可见字符，导致 index 偏移。这可能会产生误点、重复点击等问题。我们通过调用这一模块，把识别出来的一段文本，精确映射到字符 Symbol 的 boundingBox，用于点击。

算法要点
•	在 block.text 中 indexOf(sequence) 找到首次匹配位置
•	通过 countSpaces(text, start, length) 统计关键词之前的空白字符数量
•	对索引进行修正，计算“去空白后的真实索引”与“真实长度”：
	actualIndex = index - countSpaces(text, 0, index)
	actualLength = seqLen - countSpaces(text, index, seqLen)
•	最终映射到 Symbol 级 bounding box，即返回 symbols.subList(actualIndex, actualIndex + actualLength)

点击策略（防抖/消失判定）
destructivelyClickOnceAppearsText(keyword)：
•	loopUntil(durationMillis) 循环 OCR，直到出现关键字或超时
•	检测到关键词，关键词出现后每 200ms 点击一次（在 boundingBox 内随机点）
•	点击后继续 OCR：如果关键字不再出现，认为点击成功并返回次数；若一直不消失，返回 -1

4.2.6 目标检测模块（ncnn + JNI）
功能说明
识别屏幕上的特定图标/目标（例如签到按钮类目标），输出类别名、位置框与置信度。

模型加载（assets）
•	.param：使用 load_param_mem() 从内存加载（规避部分解析问题）
•	.bin：使用 load_model(mgr, "ncnn/IconLabeling.bin")
•	类别表 CLASS_NAMES[]：如 "GS_CheckIn", "HK2_CheckIn"...

预处理算法（letterbox + normalize）
1.	目标输入尺寸：INPUT_SIZE=640
2.	等比例缩放：
a)	若 w>h：h = h*640/w, w=640
b)	否则：w = w*640/h, h=640
3.	RGBA → RGB 并 resize：from_pixels_resize(... PIXEL_RGBA2RGB ...)
4.	padding 到 640x640：边界填充常数 114（常见 YOLO 风格）
5.	normalize：乘 1/255

推理与解码算法（简化版）
•	extractor.input("in0", in_mat)
•	extractor.extract("out0", out_mat)
•	对每个候选 i：在所有类别 score 中取最大且 > confidence 的类别
•	bbox 参数取 out_mat[i + mat_w * k] (k=0..3)，分别为中心点与宽高（按该模型输出定义）

重叠去重算法（轻量 NMS）
•	判断同类目标是否“近似重叠”：4 个 bbox 参数的相对差异均 ≤ 30%，判定为同一目标
•	若重叠：保留置信度更高者（替换低置信度的 bbox 与 confidence）
优势：该算法为轻量级NMS变体，计算复杂度低，更适配移动端的使用场景。

坐标反算（从 640 输入映射回原屏幕）
•	先去 padding：realCenterX = cx - wpad/2，realCenterY = cy - hpad/2
•	再乘比例 ratio：ratio = originalSize / 640（按宽高哪个未 padding 来决定）
•	得到原图上的 Rect(x1,y1,x2,y2)

4.2.7 脚本回放引擎模块（CSV → Gesture）
脚本格式定义（以CSV格式描述）：
•	Down, t, x, y：按下（t 可空/忽略）
•	Move, dt, x, y：移动（dt 表示相对等待/持续）
•	Up：抬起
•	wait, dt：等待 dt 毫秒
•	NEXT, dt：分割手势序列，表示下一段手势序列在 dt 后开始

解析算法（extractScript）：解析CSV，构建时间路径
•	将脚本拆为多个时间路径 TimePath（每段手势一条 TimePath）
•	维护 unDownTimePoint：处理 Up 后到下一次 Down 前的等待时间
•	wait 的处理：
	若当前在“未 Down”状态，则累计到 unDownTimePoint.preTime
	否则累加到当前路径末尾点（或新建点）

打包算法（packTimePaths → Gesture/Stroke）
•	每条 TimePath 转为 1 个 Gesture，Gesture 包含若干 Stroke
•	Stroke 由 points 构成，并设置 duration
•	若相邻点的 preTime（时间间隔）近似相等（±10%），则合并进同一 Stroke（减少 stroke 数量）
•	限制条件：
	Stroke 数量不得超过系统上限 GestureDescription.getMaxStrokeCount()
	duration 必须在合理范围（例如 <3000ms 的保护）

旋转适配算法（rotateCoordinate）
功能：根据当前屏幕方向，对坐标进行旋转映射，增强不同方向下脚本的复用性。
回放前读取 display.rotation，对每个点做坐标旋转：
•	ROTATION_0： (x,y)
•	ROTATION_90： (y, H-x)
•	ROTATION_180： (W-x, H-y)
•	ROTATION_270： (W-y, x)

最终执行
•	每个 Gesture 由 GestureDescription.Builder().addStroke(...) 构建
•	dispatchGesture(builder.build(), null, null) 执行
•	并按 gesture.startTime 延迟启动（脚本时序得以保持）

4.2.8 窗口/Activity 监听模块（用于“等待页面加载完成”）
功能说明
当脚本需要“等待跳转到某个页面再继续”，通过监听 TYPE_WINDOW_STATE_CHANGED / TYPE_WINDOWS_CHANGED 实现。

算法要点
•	classNameMap: Map<String, List<Listener>> 存储待监听 className
•	每个监听器带 expirationTime = now + maximalMillis
•	收到事件时：
	若 className 命中 → 回调 reply(true/false) 并移除监听器
	若超时或主程序断连 → 清空所有监听器
•	仅在存在监听需求时才动态开启事件监听（降低系统开销）

第 5 章 用户角色与权限说明
用户角色
•	普通用户：普通用户可以使用全部功能，包括脚本管理、回放、屏幕捕捉、OCR、检测等。
•	管理员：当前版本未区分管理员角色

权限要求：系统级权限（必须开启）
•	无障碍服务权限（手势执行、窗口事件）
•	屏幕捕获权限（MediaProjection，需要用户授权）
•	通知权限（前台服务必须展示通知，否则投影可能失败）
第 6 章 数据导入与导出说明
6.1 数据导入
本项目支持从 PC 端录制触摸事件，生成脚本 CSV 后导入手机进行回放。

一、	PC 录制工具（MotionRecord）工作流
1.	Record.py：
adb devices 选择设备
adb shell getevent -lp 找到触摸设备
adb shell getevent -lt <touch_device> 开始实时输出

2.	extractADBEvent.py：
解析 ABS_MT_POSITION_X/Y、ABS_MT_SLOT、ABS_MT_TRACKING_ID
推断 Down/Move/Up
插入 wait（根据时间差）与 NEXT（分段）
坐标做缩放（示例代码中 /16）并输出 CSV

二、	导入方式
将生成的 CSV/脚本文本放入应用可读取目录（或通过adb push到指定目录/资源位）
在主界面脚本管理中选择该脚本用于回放（具体UI界面的入口为准）
6.2 数据导出
ScriptPlanStore 将计划保存到 SharedPreferences 的 plan_json 字段
可通过备份 SharedPreferences 文件实现导出（工程演示中一般不要求）
第 7 章 常见问题与解答

Q1：开启无障碍后仍无法点击/滑动？
A：检查是否开启了对应的无障碍服务；部分系统需允许“完全控制”。另外，手势执行依赖系统接口，少数 ROM 会限制。

Q2：屏幕捕获失败或立刻停止？
A：需要前台服务通知权限。若通知被系统禁止，MediaProjection 可能无法稳定运行。

Q3：OCR 识别不到字怎么办？
A：优先缩小识别区域（cropNV21），并避免过小字体；必要时可多次采样合并结果（可作为扩展优化点）。

Q4：目标检测识别不准？
A：调整 confidence 阈值；确认模型与输入预处理一致（resize+padding+normalize）。也可加入更严格 NMS。
第 8 章 系统限制与已知问题
1.	在开发过程中的测试中，我们发现Harmony OS系统由于系统安全设置，不能顺利打开无障碍模式。因此相关设备无法顺利运行我们的程序。
2.	OCR 受字体/对比度影响较大，极端配色会漏识别
3.	ncnn 检测模型类别固定，若 UI 改版可能失效，需要重新训练/替换模型
4.	不同 ROM 对无障碍与投影权限策略不同，稳定性存在差异
5.	脚本回放对分辨率/旋转适配已处理，但对“控件位置变动”仍敏感（建议结合 OCR/检测做动态定位）

第 9 章 技术支持与联系方式

项目负责人：xxx（公开仓库需隐藏）
联系邮箱：1658106711@qq.com
反馈方式：https://github.com/hikali8/MiroDailyWorkshop.git
第 10 章 附录
10.1 术语解释
缩写	英文全称	中文释义
ADB	Android Debug Bridge	Android 调试桥，用于电脑与设备之间的调试与命令交互。
AIDL	Android Interface Definition Language	Android 接口定义语言，用于进程间通信接口描述与代码生成。
API	Application Programming Interface	应用程序编程接口；在 Android 语境下也可指系统接口级别（如 API 26）。
ARM64	ARM 64-bit Architecture	64 位 ARM 处理器架构（常见于 Android 真机）。
CPU	Central Processing Unit	中央处理器。
CSV	Comma-Separated Values	逗号分隔值文件格式，本项目用于脚本指令序列存储。
FPS	Frames Per Second	每秒帧数，用于描述屏幕捕获/处理刷新率。
GPU	Graphics Processing Unit	图形处理器（用于图形渲染/并行计算；本项目不强依赖）。
IPC	Inter-Process Communication	进程间通信（Android 常通过 Binder/AIDL 实现）。
JNI	Java Native Interface	Java 与 C/C++ 原生代码交互接口，用于调用 native 库。
JSON	JavaScript Object Notation	轻量数据交换格式，本项目用于计划配置持久化存储。
ML	Machine Learning	机器学习；用于统称 OCR/目标检测等智能模块。
NMS	Non-Maximum Suppression	非极大值抑制，目标检测中用于去除重复候选框。
NV21	YUV 4:2:0 (NV21)	Android 常用图像格式：Y 平面 + 交错 VU 平面（V 在前、U 在后）。
OCR	Optical Character Recognition	光学字符识别，从图像中识别文字。
PC	Personal Computer	个人计算机（用于脚本录制/导入等上位机操作）。
PNG	Portable Network Graphics	无损位图格式，常用于导出结构图插入论文/报告。
RGBA	Red Green Blue Alpha	像素格式（含 Alpha 通道），屏幕捕获常见输出格式之一。
ROM	(Android) Custom ROM / System Firmware	厂商定制系统/系统固件，不同 ROM 的权限策略可能不同。
SDK	Software Development Kit	软件开发工具包（如 Android SDK）。
UI	User Interface	用户界面。
UV	U/V Chroma Channels	YUV 色度分量（色彩信息）；NV21 的色度数据位于 VU 平面。
VU	VU Interleaved Plane	NV21 色度平面的交错排列方式：V 在前、U 在后。


完整内容请见于doc文件夹。


