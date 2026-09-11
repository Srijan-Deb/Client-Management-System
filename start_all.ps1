$services = @("api-gateway", "client-service", "account-service", "billing-service", "notification-service", "analytics-service")
$java21Home = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Starting All CMS Services with Java 21 & Safe Memory   " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$rootPath = $PSScriptRoot
if (-not $rootPath) {
    $rootPath = (Get-Location).Path
}

# Load .env variables into the current session so spawned processes inherit them
$envFile = Join-Path $rootPath ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env..." -ForegroundColor DarkCyan
    Get-Content $envFile | Where-Object { $_ -match '^\s*(?!#)([^=]+)=(.*)$' } | ForEach-Object {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

foreach ($service in $services) {
    $serviceDir = Join-Path $rootPath $service
    Write-Host "Starting $service in new window..." -ForegroundColor Green
    $cmd = "`$env:JAVA_HOME = '$java21Home'; `$env:Path = '$java21Home\bin;' + `$env:Path; Set-Location -LiteralPath '$serviceDir'; `$Host.UI.RawUI.WindowTitle = '$service'; mvn spring-boot:run '-Dspring-boot.run.jvmArguments=-Dspring.profiles.active=local -Xmx384m -Xms128m'"
    Start-Process powershell -WorkingDirectory $serviceDir -ArgumentList "-NoExit", "-Command", $cmd
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "All 6 services launched! Monitor the individual windows for startup logs." -ForegroundColor Yellow

