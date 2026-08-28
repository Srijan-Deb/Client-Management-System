param(
    [string]$Registry = "ghcr.io/srijan-deb"
)

Write-Host "Building project..."
mvn clean package '-Dmaven.test.skip=true'

$services = @("api-gateway", "client-service", "account-service", "billing-service", "notification-service")

$images = @()
foreach ($service in $services) {
    Write-Host "Building Docker image for $service..."
    docker build -t "$Registry/cms-$($service):latest" "./$service"
    $images += "$Registry/cms-$($service):latest"
}

Write-Host "Importing images sequentially to k3d cluster to prevent Docker daemon freezing..."
foreach ($img in $images) {
    Write-Host "Importing $img..."
    k3d image import $img -c cms-cluster
}

Write-Host "Done!"
