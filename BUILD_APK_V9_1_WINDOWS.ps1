Write-Host "[1/3] LocalVision APP v9.1 Gradle build start" -ForegroundColor Cyan
Set-Location $PSScriptRoot

Write-Host "[2/3] Building debug APK..." -ForegroundColor Cyan
.\gradlew.bat :app:assembleDebug

Write-Host "[3/3] Build finished" -ForegroundColor Green
Write-Host "APK path: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Yellow
