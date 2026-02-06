$env:JAVA_HOME = "F:\Java\Jdk8"
$env:PATH = "F:\Java\Jdk8\bin;" + $env:PATH

Write-Host "Building Village Defense..."
.\gradlew.bat build --no-daemon
