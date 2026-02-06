$env:JAVA_HOME = "F:\Java\Jdk8"
$env:PATH = "F:\Java\Jdk8\bin;" + $env:PATH

Write-Host "Building Village Defense with retry for dependency download..."
for ($i = 1; $i -le 30; $i++) {
    Write-Host "=== Build Attempt $i ===" -ForegroundColor Yellow
    & .\gradlew.bat build --no-daemon --refresh-dependencies 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "BUILD SUCCESS!" -ForegroundColor Green
        exit 0
    }
    Write-Host "Build failed, retrying in 3 seconds..." -ForegroundColor Red
    Start-Sleep -Seconds 3
}
Write-Host "All attempts failed" -ForegroundColor Red
exit 1
