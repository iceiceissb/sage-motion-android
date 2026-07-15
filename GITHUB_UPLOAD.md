# 上传到 GitHub

## 方式一：网页建仓库 + PowerShell 推送

1. 登录 GitHub，点击右上角 `+` → `New repository`。
2. 仓库名建议 `sage-motion-android`，先不要勾选 README、.gitignore 或 License。
3. 在本机 PowerShell 中进入工程：

```powershell
cd "D:\清华\2026AI动效\sage-motion-android"
git init
git branch -M main
git add .
git commit -m "feat: add SAGE motion experiment Android demo"
git remote add origin https://github.com/你的用户名/sage-motion-android.git
git push -u origin main
```

如果 GitHub 要求登录，浏览器会弹出授权；也可以使用 Personal Access Token 代替密码。

## 方式二：GitHub Desktop

1. 打开 GitHub Desktop，选择 `File → Add local repository`。
2. 选择 `sage-motion-android` 文件夹；若提示不是仓库，点击创建仓库。
3. 提交全部源文件，点击 `Publish repository`。

## 不要上传

`.gitignore` 已排除本机 SDK 路径、Gradle 缓存、IDE 设置、构建目录和签名文件。请不要强制上传：

- `local.properties`
- `.gradle/`、`.idea/`、`**/build/`
- `*.jks`、`*.keystore`
- 参与者 CSV 实验数据

## 发布可安装 APK

GitHub 源码仓库默认不会展示被忽略的 `build/`。需要给组员下载 APK 时：

1. 先运行 `.\build-app.ps1`。
2. 在 GitHub 仓库右侧选择 `Releases → Draft a new release`。
3. 创建标签，例如 `v0.1.0-demo`。
4. 上传 `app/build/outputs/apk/debug/app-debug.apk`。
5. 在说明中注明“研究 Demo，Android 8.0+，不包含真实 AI 服务”。

正式分发前应使用 Android Studio 的 `Build → Generate Signed App Bundle or APK` 创建 release 签名包；签名文件必须离线保存，不要提交到 GitHub。
