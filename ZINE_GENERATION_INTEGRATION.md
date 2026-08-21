# Gathered Scenes 生成式纸刊接入说明

当前 APK 内的“纸刊预览”只做本机排版：保留用户真实照片、路线关系与实验统计，不上传照片，也不冒充模型生成结果。真正的 Gathered Scenes 成品需要由服务端运行图像模型和 `scenes-gathered-zine-v1-3` 视觉规范，再把最终图片返回 App。

## 为什么不能直接把 Codex Skill 塞进 APK

Codex Skill 是面向生成代理的工作流说明，不是 Android 可直接引用的 SDK。把模型密钥或完整生成逻辑写进 APK 会被反编译，也难以统一模型版本、提示词版本、失败重试和实验日志。因此推荐结构是：

```text
Android App
  └─ 用户确认上传真实照片与旅程数据
       └─ SAGE 后端 /v1/zines
            ├─ 校验与去标识化
            ├─ 应用 scenes-gathered-zine-v1-3 规范
            ├─ 调用图像生成模型
            └─ 返回任务进度与最终 PNG
```

## 建议的最小接口

创建任务：

```http
POST /v1/zines
Content-Type: multipart/form-data

photo=<一张当前选中的真实照片>
journey=<JSON：公园名、路线名、距离、时长、照片标题、用户确认过的发现>
style=scenes-gathered-zine-v1-3
language=zh-CN
```

返回：

```json
{
  "jobId": "zine_20260821_xxx",
  "status": "queued"
}
```

查询任务：

```http
GET /v1/zines/{jobId}
```

完成时返回 `status=completed`、最终 PNG 的短期下载地址、图片哈希、模型版本和视觉规范版本。App 下载后写入私有目录，并在历史记录中保存本地 URI；网络失败时继续显示本机纸刊预览。

## 生成约束

- 一次只用一张真实照片作为主要锚点，不拼贴重复照片。
- 保留照片内容真实性，不替换主体，不杜撰地点或植物结论。
- 使用清晰、可见的手撕纤维边缘，大面积安静留白，以及一条承担结构作用的高饱和色。
- 中文微文字只来自用户确认过的路线和发现；模型不得编造打卡点、天气或知识结论。
- 生成前显示上传确认，默认去除参与者编号和精确坐标；服务端设置明确的图片保留期限。

## 实验日志建议

记录 `generation_started_at`、`generation_completed_at`、`generation_duration_ms`、`model_version`、`style_version`、`input_photo_count`、`result_hash` 和失败码。不要把 API Key、完整系统提示词或未经用户确认的照片内容写入 CSV。

这条边界能保证现阶段 APK 仍可离线、可复现地跑实验；部署后端后，再把“纸刊预览”按钮升级为“生成纸刊”，而不需要改动路线、拍照和历史数据结构。
