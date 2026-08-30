# SAGE Agent Backend

这是 SAGE Motion 的服务端单主 Agent。Android 只连接本服务；`OPENAI_API_KEY` 始终保存在服务端环境变量中，不进入 APK。

## 能力与边界

- 使用 OpenAI Responses API，而不是客户端直连模型。
- 每个场景只暴露必要的 strict function tools。
- 支持 Open-Meteo/CAMS 环境工具、OpenStreetMap 附近地点工具和可复现路线画像评分器。
- 通过 SSE 输出 `stage_changed`、`tool_started`、`tool_completed`、`completed` 和 `error`。
- `store: false`，不使用 OpenAI 服务端会话存储；每次请求只上传完成任务所需的最小旅程摘要。
- 不上传参与者真实位置。天气与地点工具固定使用东升八家郊野公园实验点。
- 路线几何、定位和导航仍由 Android 高德 SDK 提供；服务端不能伪造高德路径。

## 本地运行

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
Copy-Item .env.example .env
# 编辑 .env，只在这里填写 OPENAI_API_KEY
uvicorn sage_backend.main:app --reload --host 0.0.0.0 --port 8000
```

检查：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/healthz
Invoke-RestMethod http://127.0.0.1:8000/readyz
pytest
ruff check .
```

## 请求示例

```powershell
$body = @{
  request_id = "testrequest001"
  scenario = "B2"
  prompt = "附近有什么好吃的？"
  vision_findings = @()
  journey_context = @{ active_route_name = "湖边林荫线" }
  client_capabilities = @("amap_route", "on_device_vision", "offline_speech")
} | ConvertTo-Json -Depth 6

Invoke-WebRequest `
  -Uri http://127.0.0.1:8000/v1/agent/tasks:stream `
  -Method POST `
  -Headers @{ Accept = "text/event-stream"; Authorization = "Bearer $env:SAGE_BACKEND_AUTH_TOKEN" } `
  -ContentType "application/json" `
  -Body $body
```

## Android 配置

在项目根目录 `local.properties` 中设置：

```properties
SAGE_AGENT_BACKEND_URL=https://your-agent.example.com
SAGE_AGENT_CLIENT_TOKEN=replace-with-a-short-lived-or-development-token
```

没有配置 URL 时，APK 自动使用原来的本地工具编排。配置 URL 后，生态演示模式优先调用本后端；连接失败会明确回退本地工具编排。正式实验模式始终使用确定性 `MockAiDemoApi`。

`SAGE_AGENT_CLIENT_TOKEN` 不是 OpenAI Key。它若静态写进发布 APK 仍可被提取，因此只适合受控预实验；公开发布时应由登录/设备证明流程签发短期令牌，并在 API Gateway 上做用户级限流。

## 生产部署要求

1. 设置 `SAGE_ENV=production`。生产模式未提供 `SAGE_BACKEND_AUTH_TOKEN` 时会拒绝启动。
2. 只通过 HTTPS 暴露服务，建议放在 Cloud Run、Fly.io、Azure Container Apps 或反向代理之后。
3. 在网关配置短期身份令牌、全局限流、请求体限制、日志脱敏和告警；进程内限流只是第二道保护。
4. 将 `OPENAI_API_KEY` 存入部署平台 Secret Manager，不写 `.env` 镜像层、不输出到日志。
5. 正式实验不要启用远程 Agent；生态演示日志与正式实验数据分开。
6. 对固定回归集评测工具选择、无数据拒答、路线事实一致性、延迟、成本和回退率后再升级模型。

## SSE 协议

每个事件使用标准 `event:` / `data:` 帧。例如：

```text
event: stage_changed
data: {"request_id":"...","stage":"reasoning"}

event: completed
data: {"request_id":"...","result":{...}}
```

连接期间每 10 秒发送一次注释心跳，兼容常见反向代理。错误帧不会返回密钥、系统提示或上游原始响应正文。
