Write-Host "Starting Pixel 7 Emulator..." -ForegroundColor Cyan
Set-Location "C:\Program Files (x86)\Android\android-sdk\emulator"
Start-Process .\emulator.exe -ArgumentList "-avd pixel_7_-_api_35"

Write-Host "Waiting for emulator device..." -ForegroundColor Yellow
Set-Location "C:\Program Files (x86)\Android\android-sdk\platform-tools"
.\adb.exe wait-for-device

Write-Host "Launching DutchPay App..." -ForegroundColor Green
.\adb.exe shell am start -n kr.dutchpay/.MainActivity
Write-Host "`n[Done] App is running on the emulator!" -ForegroundColor Green
Pause
