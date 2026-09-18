# SAGE Agent Backend

这是 SAGE Motion 的服务端单主 Agent。Android 只连接本服务；`OPENAI_API_KEY` 始终保存在服务端环境变量中，不进入 APK。

## 能力与边界

- 使用 OpenAI Responses API，而不是客户端直连模型。
- 由服务端为每个场景选择一个具名 skill；skill 同时约束任务规则、暴露的 strict function tools，
  以及工具执行时的二次白名单校验，用户提示不能切换 skill 或扩权。
- 支持 Open-Meteo/CAMS 环境工具、OpenStreetMap 附近地点工具和可复现路线画像评分器。
- 通过 SSE 输出 `stage_changed`、`tool_started`、`tool_completed`、`completed` 和 `error`。
- B1 视觉场景支持用户逐次授权后上传压缩照片，由多模态主控模型直接看图；端侧 ML Kit 标签作为辅助和无授权时的回退。
- `store: false`，不使用 OpenAI 服务端会话存储；每次请求只上传完成任务所需的最小旅程摘要。
- 照片在 Android 端去除元数据、缩至最长边 1280 px 且限制为 1.2 MB JPEG；后端再次校验类型、签名、场景和大小，不写入日志或数据库。
- 不上传参与者真实位置。天气与地点工具固定使用东升八家郊野公园实验点。
- 路线几何、定位和导航仍由 Android 高德 SDK 提供；服务端不能伪造高德路径。

## 当前 skills 与 tools

| 场景 | 服务端 skill | 可调用 tools |
|---|---|---|
| A 环境与路线 | `route_planning` | `get_park_environment`、`compare_park_route_profiles` |
| B 探索入口 | `exploration_hub` | 无 |
| B1 视觉发现 | `visual_discovery` | 无（读取获授权的压缩原图和 Android 视觉标签） |
| B2 语音陪伴 | `voice_companion` | 环境、附近地点、路线画像 |
| C 旅程编排 | `journey_composer` | 无（读取最小旅程摘要） |
| D 动态改道 | `dynamic_replanning` | 环境、路线画像 |

skill 是主 Agent 的受信任工作流与权限边界，不是另一个模型或另一个聊天机器人。工具仍有自己的
Pydantic 参数 schema、场景限制、超时、来源与失败语义；两层权限必须同时允许才会执行。

默认主控模型是 `gpt-5.6-terra`：它用于生态演示的正式后端，兼顾工具调用质量、延迟与成本。
开发期若只做连通性和低成本冒烟测试，可在 `.env` 中临时设置 `SAGE_OPENAI_MODEL=gpt-5.6-luna`；
不要在同一正式实验批次中途切换模型。

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

在 B1 拍照后，用户必须勾选“允许项目后端将压缩照片发送给 OpenAI”，该次任务才会携带图像。授权只保存在当前内存状态，重拍、重置或重启 App 后都会恢复为未授权；不勾选时后端只收到端侧标签。

`SAGE_AGENT_CLIENT_TOKEN` 不是 OpenAI Key。它若静态写进发布 APK 仍可被提取，因此只适合受控预实验；公开发布时应由登录/设备证明流程签发短期令牌，并在 API Gateway 上做用户级限流。

## 生产部署要求

1. 设置 `SAGE_ENV=production`。生产模式未提供 `SAGE_BACKEND_AUTH_TOKEN` 时会拒绝启动。
2. 只通过 HTTPS 暴露服务，建议放在 Cloud Run、Fly.io、Azure Container Apps 或反向代理之后。
3. 在网关配置短期身份令牌、全局限流、请求体限制、日志脱敏和告警；进程内限流只是第二道保护。
4. 将 `OPENAI_API_KEY` 存入部署平台 Secret Manager，不写 `.env` 镜像层、不输出到日志。
5. 正式实验不要启用远程 Agent；生态演示日志与正式实验数据分开。
6. 对固定回归集评测工具选择、无数据拒答、路线事实一致性、延迟、成本和回退率后再升级模型。

## 推荐部署：Google Cloud Run

仓库已经有可直接部署的 `Dockerfile`。下面的流程只部署后端容器；模型仍由 OpenAI Responses API 托管，不需要自行部署 GPU 或模型权重。

如果已经安装并登录 Google Cloud CLI，可以在仓库根目录直接运行一键脚本：

```powershell
.\backend\deploy-cloud-run.ps1 -ProjectId YOUR_GOOGLE_CLOUD_PROJECT_ID
```

脚本会检查结算、启用 Cloud Run/Cloud Build/Artifact Registry/Secret Manager API，在终端中隐藏读取
OpenAI Key，生成独立的后端访问令牌，把两者保存到 Secret Manager，部署一个最多 2 实例且可缩容到 0
的服务，验证 `/healthz` 与 `/readyz`，最后更新未纳入 Git 的 `local.properties`。再次执行会轮换两个
Secret 的版本，因此随后需要重新构建 APK。下面保留等价的手动流程，便于排错和审计。

### 1. 准备两个服务端 Secret

在 OpenAI Platform 创建一个仅供本项目使用的 API Key，并在 Google Secret Manager 创建：

- `sage-openai-api-key`：值为 OpenAI API Key。
- `sage-backend-auth-token`：值为 Android 访问本后端的随机 bearer token。

可在本机 PowerShell 生成第二个随机 token，生成后只保存到 Secret Manager 和受控构建机：

```powershell
$sageAuthBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($sageAuthBytes)
[Convert]::ToBase64String($sageAuthBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
```

### 2. 从 `backend/` 部署容器

先安装并登录 Google Cloud CLI，然后在本目录运行。把项目 ID 和区域替换为自己的值：

```powershell
gcloud config set project YOUR_GOOGLE_CLOUD_PROJECT_ID
gcloud services enable run.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com

gcloud run deploy sage-agent-backend `
  --source . `
  --region YOUR_REGION `
  --allow-unauthenticated `
  --port 8000 `
  --set-env-vars "SAGE_ENV=production,SAGE_OPENAI_MODEL=gpt-5.6-terra,SAGE_OPENAI_REASONING_EFFORT=low,SAGE_RATE_LIMIT_PER_MINUTE=30" `
  --set-secrets "OPENAI_API_KEY=sage-openai-api-key:latest,SAGE_BACKEND_AUTH_TOKEN=sage-backend-auth-token:latest"
```

这里的 `--allow-unauthenticated` 只表示 Cloud Run 不要求 Google IAM 登录；业务接口仍由 `SAGE_BACKEND_AUTH_TOKEN` 校验。若公开分发 App，应在 API Gateway 中换成短期用户/设备令牌，而不是把共享长期 token 固化进 APK。

### 3. 验证部署

```powershell
$sageBackendUrl = "https://YOUR_CLOUD_RUN_URL"
Invoke-RestMethod "$sageBackendUrl/healthz"
Invoke-RestMethod "$sageBackendUrl/readyz"
```

`/healthz` 应返回 `ok`，`/readyz` 应返回 `ready`。再用本文上方的 SSE 请求示例调用一次 B2；若返回 401，检查 Android 与 Secret Manager 中的 bearer token 是否一致；若返回 503，检查 OpenAI Key 是否正确挂载。

### 4. 把 Android 接到后端并重打包

在项目根目录、不会提交到 Git 的 `local.properties` 中填写：

```properties
SAGE_AGENT_BACKEND_URL=https://YOUR_CLOUD_RUN_URL
SAGE_AGENT_CLIENT_TOKEN=与 sage-backend-auth-token 相同的受控预实验 token
```

然后回到项目根目录执行 `./build-app.ps1`，生成推荐的精简 APK。只有“生态演示”模式会优先调用远程 Agent；正式实验模式仍固定走 `MockAiDemoApi`，不会因后端上线而改变实验刺激。

## SSE 协议

每个事件使用标准 `event:` / `data:` 帧。例如：

```text
event: stage_changed
data: {"request_id":"...","stage":"reasoning"}

event: completed
data: {"request_id":"...","result":{...}}
```

连接期间每 10 秒发送一次注释心跳，兼容常见反向代理。错误帧不会返回密钥、系统提示或上游原始响应正文。
