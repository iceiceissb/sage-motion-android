# SAGE Motion：信息架构与核心交互流程

依据 `app/src/main` 的 Compose 页面、`ExperimentViewModel` 状态机、数据层与项目说明整理。图中“参与者”指正常体验路径；“研究者”指长按顶部状态栏后出现的受控入口。

## 1. 信息架构

```mermaid
flowchart TD
    APP["SAGE Motion App\n单 Activity / Compose"]

    APP --> BOOT{"是否存在可恢复的\n进行中会话？"}
    BOOT -->|否| SETUP["研究者设置页"]
    BOOT -->|是| RESTORE["恢复上次会话\n回到中断前任务"]
    RESTORE --> EXP

    SETUP --> SID["参与者编号"]
    SETUP --> ORDER["实验条件顺序\n6 种 Latin square"]
    SETUP --> MODE["运行模式\n离线实验 / 联网 Agent"]
    SETUP --> START["开始实验会话"]
    SETUP --> HISTORY["本地历史数据"]
    START --> EXP["参与者实验界面"]

    subgraph EXP["参与者实验界面"]
        direction TB
        A["A 环境感知\n路线约束与双路线推荐"]
        HUB["B 探索工作台\n可重复、无固定顺序"]
        B1["B1 拍照圈搜\n拍照 / 识别 / 圈选 / 提问"]
        B2["B2 语音对话\n语音转写 / 回答播报"]
        D["D 动态重规划\n封路与天气变化下的路线取舍"]
        C["C 创作与分享\n知识游记"]
        DONE["完整体验完成"]

        A -->|"采纳路线"| HUB
        HUB -->|"拍照圈搜"| B1
        HUB -->|"语音对话"| B2
        HUB -->|"重新规划"| D
        B1 -->|"保存到游记并返回"| HUB
        B2 -->|"采纳回答并返回"| HUB
        D -->|"采纳路线并返回"| HUB
        HUB -->|"结束探索"| C
        C -->|"完成游记"| DONE
        DONE -->|"重新体验"| A
    end

    EXP --> RESULT["任务结果层\n推荐/备选、指标、来源、重试、采纳"]
    RESULT --> EVIDENCE["依据弹窗\n仅 SAGE Full 提供入口"]

    EXP --> RESEARCH["隐藏研究者控制台\n长按顶部状态栏"]
    RESEARCH -->|"切换条件/任务、预览状态、导出、结束"| EXP
    RESEARCH --> HISTORY

    HISTORY --> DETAIL["会话详情\n事件时间线"]
    HISTORY --> EXPORT["导出单次 CSV / 全部 ZIP"]
    HISTORY --> DELETE["删除单次 / 全部历史\n含关联过程照片"]

    EXP --> STORE[("本地持久化\nCSV 日志 + SharedPreferences 会话摘要\n相机过程照片")]
    HISTORY --> STORE
```

信息层级的关键点：A 是主路径的必经起点；B 是并行工具工作台，B1、B2、D 可以任意顺序并反复进入；C 由用户主动结束探索后进入。三种实验条件不改变任务结构，只改变 AI 状态与反馈呈现：Baseline、Semantic Motion、SAGE Full（额外呈现不确定性、依据和接管入口）。

## 2. 核心交互流程

```mermaid
flowchart TD
    S["研究者设置\n编号 + 条件顺序 + 运行模式"] --> SS["创建会话\n立即开始 CSV 事件记录"]
    SS --> AIN["A：选择偏好 / 输入或语音补充路线约束"]
    AIN --> ARUN["运行路线任务"]
    ARUN --> ASTATE["AI 阶段流\n唤起 → 定位 → 推理 → 不确定性 → 完成"]
    ASTATE --> ARES["路线结果\n推荐路线 vs 备选路线"]
    ARES --> ADEC{"参与者操作"}
    ADEC -->|"切换路线"| ARES
    ADEC -->|"查看依据"| AEVI["依据弹窗"]
    AEVI --> ARES
    ADEC -->|"重试"| AIN
    ADEC -->|"采纳"| HUB["探索工作台"]

    HUB --> TOOL{"选择任一工具\n可反复进入"}
    TOOL -->|"B1 拍照圈搜"| VCAP["拍照\n或使用固定刺激图"]
    VCAP --> VANALYZE["端侧 ML Kit 图像标签识别"]
    VANALYZE --> VSTAGE["AI 阶段流\n唤起 → 识别 → 推理 → 不确定性 → 完成"]
    VSTAGE --> VCIRCLE["在照片上拖拽圈选区域"]
    VCIRCLE --> VASK["选择建议问题\n或输入自定义问题"]
    VASK --> VANS["圈搜回答\n问题与回答写入该照片游记节点"]
    VANS -->|"保存并返回"| HUB

    TOOL -->|"B2 语音对话"| VLISTEN["申请麦克风权限\n系统语音识别转写"]
    VLISTEN --> VORUN["运行语音任务"]
    VORUN --> VOSTAGE["AI 阶段流\n唤起 → 聆听 → 推理 → 组织回答 → 完成"]
    VOSTAGE --> VOANS["展示结果并自动 TTS 播报\n可重播 / 重试 / 查看依据"]
    VOANS -->|"采纳"| HUB

    TOOL -->|"D 动态重规划"| DRUN["运行重规划任务"]
    DRUN --> DSTAGE["AI 阶段流\n唤起 → 定位变化 → 重规划 → 权衡 → 不确定性 → 完成"]
    DSTAGE --> DRES["新旧路线对比\n选择推荐或备选"]
    DRES -->|"采纳"| HUB

    HUB -->|"结束探索并生成知识游记"| CRUN["C：运行创作任务"]
    CRUN --> CSTAGE["AI 阶段流\n唤起 → 汇总 → 生成 → 编排 → 完成"]
    CSTAGE --> JOURNEY["路线式知识游记\n路线 + 过程照片 + 圈搜问答 + 行为计数"]
    JOURNEY --> SHARE["系统分享知识游记文本"]
    JOURNEY --> COMPLETE["采纳结果\n进入完整体验完成页"]
    COMPLETE --> END["保存并结束会话"]

    SS -. "每次关键操作" .-> LOG[("CSV 事件日志")]
    VCAP -. "照片 URI 与识别线索" .-> SESSION[("可恢复会话摘要")]
    ARES -. "取消 / 失败 / 中断" .-> RECOVER["保留现场\n可重试或下次启动恢复"]
    VANS -.-> SESSION
    VOANS -.-> LOG
    DRES -.-> LOG
    JOURNEY -.-> LOG
    END --> HISTORY["历史数据\n查看、导出、删除"]
```

## 3. 任务运行与数据来源

```mermaid
sequenceDiagram
    participant U as 参与者
    participant UI as Compose 界面
    participant VM as ExperimentViewModel
    participant API as 离线 Mock API / 联网 Park Agent
    participant CTX as Open-Meteo 固定校园演示点
    participant LOG as 本地 CSV 与会话摘要

    U->>UI: 提交当前任务
    UI->>VM: runCurrentScenario()
    VM->>LOG: task_started + 保存当前状态
    alt 离线实验模式
        VM->>API: 发送固定任务请求
    else 联网 Agent 模式（环境/语音/重规划）
        VM->>API: 发送任务请求
        API->>CTX: 并行读取天气与空气质量
        CTX-->>API: 实时数据 / 新鲜缓存 / 陈旧缓存 / 无结果
    end
    loop 每一个语义阶段
        API-->>VM: StageChanged
        VM->>UI: 更新动画与状态文案
        VM->>LOG: state_enter / state_exit + 保存
    end
    API-->>VM: Completed(result)
    VM->>UI: 显示结果、路线选择或下一步操作
    VM->>LOG: task_result_visible + 保存
    U->>UI: 采纳 / 重试 / 查看依据 / 取消
    UI->>VM: 对应操作
    VM->>LOG: 记录行为并持久化
```

联网模式不会读取或上传参与者的位置。视觉任务的图像标签识别在端侧 ML Kit 执行；当联网环境数据不可用时，Agent 会保留离线脚本结果并标注回退来源。
