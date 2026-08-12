param(
    [string[]]$GradleTasks = @("testDebugUnitTest", "assembleDebug")
)

$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "D:\software\Android Studio\jbr" }
$sdkHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "D:\software\Android_SDK" }
$driveLetter = @("S", "R", "Q", "P") | Where-Object { -not (Test-Path "${_}:\") } | Select-Object -First 1

if (-not $driveLetter) {
    throw "No temporary drive letter is available. Move the project to an ASCII-only path and run gradlew.bat."
}

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $sdkHome

try {
    & subst "${driveLetter}:" $projectRoot
    Push-Location "${driveLetter}:\"
    # A daemon started on a temporary subst drive keeps that drive as its working directory.
    # The drive is removed after each build, so a later invocation can fail before Gradle starts.
    & .\gradlew.bat --no-daemon @GradleTasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
    Write-Host "Gradle tasks succeeded: $($GradleTasks -join ', ')"
    if ($GradleTasks -contains "assembleDebug") {
        Write-Host "APK: $projectRoot\app\build\outputs\apk\debug\app-debug.apk"
    }
}
finally {
    if ((Get-Location).Path -like "${driveLetter}:*") { Pop-Location }
    & subst "${driveLetter}:" /D 2>$null
}
