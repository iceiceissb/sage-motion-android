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
- C 创作与分享：路线、照片、知识发现汇聚成旅程回顾并可分享。
- D 动态调整：变化点警示、旧路线残影、新路线连续重构与方案权衡。
- 6 种 Latin square 条件顺序。
- 隐藏研究员控制台：长按主界面顶部状态卡。
- 任务重置、条件切换、结果采纳、证据查看和 CSV 行为日志导出。
- 全部核心流程使用确定性本地数据，不依赖网络，保证实验节奏可复现。
- 参与者先完成 A 路线规划，再进入并行探索工作台；B1 拍照、B2 语音和 D 重规划可任意顺序、反复调用，最后主动进入 C 生成游记。
- Trail / Seed / Veil 三类语义动效：路径显影与移动光点、特征聚合、扫描幕、声波与呼吸场、不确定性虚线呼吸。
- 活动会话自动恢复；应用意外退出后可从上次条件和任务继续。
- 本地历史列表与事件时间线；支持导出单次 CSV/全部会话 ZIP，也支持单条、详情或全部删除并清理关联照片。
- 研究员控制台支持逐状态预览、任务/条件切换、保存结束和数据检查。
- 可调用真实相机并使用端侧 ML Kit 提取 400+ 类通用图像标签；模型打包在 APK 内，不需要 API Key。
- 可在应用内直接申请麦克风权限并调用 Android 系统语音识别，支持实时转写状态、停止聆听，并通过 TextToSpeech 自动播报和重播回答。
- Gather / Weave / Afterglow / Fork / Morph 动效：素材汇聚编排、新旧路线因果连续和动态重规划。
- v0.7 动效重构：双路线独立逐段计算与竞争、探索路径锚点、镜头光圈/候选框、真实音量驱动波形、素材数量驱动游记汇聚、新旧路线分段形变。
- v0.8 圈搜与约束式规划：路线计算前支持偏好点选、文字/实际语音约束；多点识别结果以悬浮气泡叠加到照片，支持手势圈选、推荐/自定义问题、圈搜回答与游记留存。
- 拍照、语音、重规划和游记页均提供明确的左上返回入口；探索工作台三张功能卡重新设计为具有任务语义和按压反馈的并行入口。
- v0.9 路线式知识游记：生成阶段逐段绘制用户走过的主线并将照片依次落到对应节点；完成后可点击照片节点，查看该照片下保存的全部圈搜问题与回答。

## API 预留

应用通过 `data/AiDemoApi.kt` 中的 `AiDemoApi` 获取阶段事件和任务结果。当前注入的是
`MockAiDemoApi`，因此无网络也能完整演示。接入真实服务时新增一个实现（例如 Retrofit + SSE），
把服务端进度映射为 `AiTaskEvent.StageChanged`、最终 JSON 映射为 `AiTaskEvent.Completed`，再在
`ExperimentViewModel` 中替换实例即可；Compose 页面和流程状态机无需改动。密钥不要写入仓库或 APK。

## 本地数据

- 会话事件实时写入应用私有目录 `files/experiment_logs/`，不申请外部存储权限。
- 活动会话摘要使用私有 `SharedPreferences` 保存，用于进程被杀或重启后的恢复。
- 开始页“查看历史数据”可浏览会话摘要和事件时间线；研究员控制台也有相同入口。
- 单次记录以 CSV 分享；“导出全部”会生成包含所有 CSV 的 ZIP。只有主动分享后数据才离开应用。
- 历史页可删除单次或全部非活动会话；删除会同步清理该会话记录到的过程照片，操作前二次确认。
- 结束会话不会删除数据；卸载应用会按 Android 规则清除应用私有数据，因此正式实验应及时导出备份。

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
2. 先点选或说出路线约束，完成双路线规划；再在探索工作台任意调用拍照圈搜、语音和重规划，最后点击“结束探索并生成知识游记”。
3. 长按顶部状态卡打开研究员控制台，可切换 A/B1/B2/C/D 和实验条件。
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

- JVM 单元测试：12 项通过。
- Debug APK 构建：通过。
- Android Lint：通过，无错误。
- 390 × 844dp Compose 截图回归：路线约束、双路线计算、路线结果、并行探索工作台、视觉提取/圈搜回答、语音、路线式知识游记、动态调整与历史页共 12 个基线。
- 设计对比与修正记录见 [design-qa.md](design-qa.md) 和 `qa-reference-vs-render.png`。
