# SAGE Motion Android 实验 Demo

这是依据项目实验流程制作的原生 Android Demo，用于比较同一任务骨架下三种 AI 反馈条件的增量效果：

1. `Baseline`：仅提供通用等待反馈和最终结果。
2. `Semantic Motion`：增加可辨识的 AI 阶段与对应动效。
3. `SAGE Full`：在语义动效上增加不确定性、判断依据和用户接管入口。

三个条件保持相同任务、信息结构、按钮位置和总计时，尽量把自变量限制在 AI 状态表达能力上。

## 已实现范围

- A 环境感知：定位、路线比较、遮阴/座椅推荐。
- B1 视觉探索：取景、识别、解释、不确定性提示。
- B2 语音陪伴：聆听、推理、组织回答。
- 6 种 Latin square 条件顺序。
- 隐藏研究员控制台：长按主界面顶部状态卡。
- 任务重置、条件切换、结果采纳、证据查看和 CSV 行为日志导出。
- 全部核心流程使用确定性本地数据，不依赖网络，保证实验节奏可复现。

## 运行

推荐用 Android Studio 打开此文件夹，等待 Gradle 同步后，连接 Android 8.0（API 26）或更高版本手机并点击 Run。

命令行构建：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\build-app.ps1
```

当前工作区含中文路径，Windows 下 Gradle/JUnit 可能无法正确解析类路径，因此脚本会临时映射一个纯英文盘符。构建完成后 APK 位于：

`app/build/outputs/apk/debug/app-debug.apk`

## 实验操作

1. 输入参与者编号并选择 Latin square 条件顺序。
2. 进入主任务，点击底部主按钮播放当前条件。
3. 长按顶部状态卡打开研究员控制台，可切换 A/B1/B2 和实验条件。
4. 每轮任务结束后记录参与者是否查看依据、换路线或采纳结果。
5. 实验结束后在研究员控制台导出 CSV。

更完整的实验执行说明见 [EXPERIMENT_PROTOCOL.md](EXPERIMENT_PROTOCOL.md)，视觉与动效规范见 [DESIGN_SPEC.md](DESIGN_SPEC.md)，上传 GitHub 见 [GITHUB_UPLOAD.md](GITHUB_UPLOAD.md)。

## 工程结构

```text
app/src/main/java/cn/tsinghua/sagemotion/
├─ ExperimentViewModel.kt       # 实验状态机与控制逻辑
├─ data/ExperimentLogger.kt     # CSV 行为日志
├─ model/ExperimentModels.kt    # 条件、任务、状态与计时
└─ ui/
   ├─ SageMotionApp.kt
   ├─ screens/ExperimentScreen.kt
   ├─ screens/ResearcherSetupScreen.kt
   └─ theme/Theme.kt
```

参考设计原图保存在 `design-reference-option-3.png`，应用中的地图和花朵为项目内真实位图资源。

## 验证状态

- JVM 单元测试：通过。
- Debug APK 构建：通过。
- 390 × 844dp Compose 截图回归：路线、视觉、语音三页通过。
- 设计对比与修正记录见 [design-qa.md](design-qa.md) 和 `qa-reference-vs-render.png`。
