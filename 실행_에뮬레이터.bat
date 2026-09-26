@echo off
title DutchPay Android Emulator Launcher
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
cd /d "C:\Program Files (x86)\Android\android-sdk\emulator"
echo Starting Pixel 7 Emulator... Please wait.
start emulator.exe -avd pixel_7_-_api_35
cd /d "C:\Program Files (x86)\Android\android-sdk\platform-tools"
echo Waiting for emulator device to be ready...
adb.exe wait-for-device
echo Installing latest DutchPay App build...
adb.exe install -r "%APK_PATH%"
echo Launching DutchPay App...
adb.exe shell am start -S -n kr.dutchpay/.MainActivity
echo.
echo [Done] DutchPay App is now running with the latest build on the emulator!
pause
