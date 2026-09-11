$ports = @(8090, 8081, 8082, 8083, 8084, 8085)

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Stopping All CMS Spring Boot Microservices             " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $pids) {
            try {
                $proc = Get-Process -Id $procId -ErrorAction Stop
                Write-Host "Stopping process on port $port (PID: $procId - $($proc.ProcessName))..." -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            } catch {
                # Process might have already terminated
            }
        }
    } else {
        Write-Host "Port $port is free." -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "All CMS services have been stopped." -ForegroundColor Green
