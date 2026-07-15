$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\software\Android Studio\jbr" }
$sdkHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\software\Android_SDK" }
$driveLetter = @("S", "R", "Q", "P") | Where-Object { -not (Test-Path "${_}:\") } | Select-Object -First 1

if (-not $driveLetter) {
    throw "没有可用的临时盘符。请将项目复制到不含中文的路径后运行 gradlew.bat。"
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $sdkHome

try {
    & subst "${driveLetter}:" $projectRoot
    Push-Location "${driveLetter}:\"
    & .\gradlew.bat testDebugUnitTest assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Gradle 构建失败，退出码 $LASTEXITCODE" }
    Write-Host "构建成功：$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
}
finally {
    if ((Get-Location).Path -like "${driveLetter}:*") { Pop-Location }
    & subst "${driveLetter}:" /D 2>$null
}
