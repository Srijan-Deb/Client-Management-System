$services = @("api-gateway", "client-service", "account-service", "billing-service", "notification-service")

Write-Host "Starting all backend services in new windows using 'local' profile..."

foreach ($service in $services) {
    Write-Host "Starting $service..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd .\$service; mvn spring-boot:run '-Dspring-boot.run.jvmArguments=-Dspring.profiles.active=local'"
}

Write-Host "All services are launching!"
