# SAGE Motion Research Notes

## 采用的研究线索

1. Heer & Robertson（2007）指出，分阶段的动画转换有助于图形感知。设计落实：路线、创作素材和动态调整不同时变更所有属性，而采用 staged transition。
   - https://idl.uw.edu/papers/animated-transitions
   - DOI: 10.1109/TVCG.2007.70539
2. Chang & Ungar（UIST 1993）强调用动画维持旧状态与新状态之间的因果联系。设计落实：旧路线使用 afterglow 残影，照片和发现沿连续轨迹进入游记，不瞬间替换。
   - https://doi.org/10.1145/168642.168647
3. Chalbi 等（2020）研究共同命运，发现运动是强感知分组因素。设计落实：同一条路线、同一段回忆的元素共享方向和节奏，避免随机粒子。
   - https://doi.org/10.1109/TVCG.2019.2934288
4. W3C WCAG 2.3.3 要求可关闭非必要交互动效。设计落实：不使用视差、闪烁和剧烈缩放；Baseline 与系统动画缩放提供低运动路径。
   - https://www.w3.org/WAI/WCAG21/Understanding/animation-from-interactions

## v0.8 动效矩阵

| 场景 | 状态 | 主语义 | 主要运动 |
|---|---|---|---|
| A 环境感知 | intent / locating / reasoning | 人的约束、空间连续、候选竞争、依据汇入 | 点选/语言约束汇聚为意图摘要；双路线独立分段显影、计算头、证据节点归入路径、竞争进度收敛 |
| 探索工作台 | persistent | 行程持续、事件留痕 | 慢速路线行进点；拍照/语音/重规划记录成为路径锚点并低频扩散 |
| B1 拍照圈搜 | activating / recognizing / selecting / responding | 镜头接入、感知范围、注意选择、问题连续性 | 光圈接入、扫描幕、语义气泡低频漂浮、连续圈选光晕、推荐问题显影、回答面板承接 |
| B2 语音陪伴 | listening / reasoning / responding | 真实输入强度、理解收敛、输出方向 | RMS 驱动语音球与波形、向心理解、向外回答线层 |
| C 创作分享 | summarizing / generating / editing | 空间叙事、素材回收与编排 | 主路线逐段显影、照片依序落位、问题徽标出现、节点选择呼吸与详情连续切换 |
| D 动态调整 | locating / replanning / deciding | 变化因果与方案接管 | 障碍波纹、旧路线残影、新路线分段 Morph、旧/新方案进度对照 |

## GitHub 开源实现审计

- [Android Compose Samples](https://github.com/android/compose-samples)（Apache-2.0）：借鉴状态驱动的 `AnimatedContent`、自定义 Canvas 与路径图形组织方式。
- [SmartToolFactory Compose Tutorials](https://github.com/SmartToolFactory/Jetpack-Compose-Tutorials)：核对 `PathMeasure`、路径片段、PathEffect 与 Canvas 动画的实现模式。
- [Rive Android](https://github.com/rive-app/rive-android)（MIT）和 [Lottie Android](https://github.com/airbnb/lottie-android)（Apache-2.0）：评估过状态机资产方案，但本轮没有引入运行时依赖或复制动画资产。原因是研究 Demo 需要让每个运动参数可审计、可记录，并避免二进制运行时和外部资产改变 APK 性能。
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Apache-2.0）：参考层级与半透明材质处理；未引入该库，以维持 Android 8+ 的一致渲染和实验设备可控性。

本轮代码为项目内原生 Compose/Canvas 重写，没有直接复制第三方源文件；开源项目用于技术路线与许可审计。

## 实验注意

- 动效丰富度不等于同时运动元素数量；主要评价语义识别率、状态判断时间和接管决策。
- 每个场景保留 Baseline / Semantic / SAGE Full 三种等时版本，避免视觉质量与信息量混淆。
- 正式实验需增加“你认为 AI 当前在做什么”的状态识别题，以验证语义动效是否被正确解码。
- 圈搜新增可记录的 `circle_search_question` 与 `circle_search_reset` 行为事件；若把圈搜纳入条件比较，应固定圈选目标、问题文本和动作次数，避免输入差异成为混淆变量。
- 路线式游记把“在哪里拍摄”与“当时问了什么”放在同一空间索引中；后续研究可比较路线节点界面与普通时间线在情境回忆、问题定位速度和知识复述上的差异。
