<#
.SYNOPSIS
    Automated Client User Provisioning for Keycloak CMS Realm.
.EXAMPLE
    .\create_client_user.ps1 -Email "srijan@cms.local" -FirstName "Srijan" -LastName "Deb" -Password "Test@1234"
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$Email,

    [Parameter(Mandatory=$false)]
    [string]$Username,

    [Parameter(Mandatory=$false)]
    [string]$Password = "Test@1234",

    [Parameter(Mandatory=$false)]
    [string]$FirstName = "Client",

    [Parameter(Mandatory=$false)]
    [string]$LastName = "User"
)

if (-not $Username) { $Username = $Email }

Write-Host "Authenticating with Keycloak Admin API..." -ForegroundColor Cyan
$authBody = @{
    grant_type = "password"
    client_id  = "admin-cli"
    username   = "admin"
    password   = "admin123"
}

$authRes = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/realms/master/protocol/openid-connect/token" `
    -ContentType "application/x-www-form-urlencoded" -Body $authBody
$headers = @{
    Authorization  = "Bearer " + $authRes.access_token
    "Content-Type" = "application/json"
}

# 1. Fetch the 'client' Realm Role
$clientRole = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/admin/realms/cms/roles/client" -Headers $headers

# 2. Check if User Already Exists
$existingUsers = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/admin/realms/cms/users?email=$Email" -Headers $headers

if ($existingUsers -and $existingUsers.Count -gt 0) {
    $userId = $existingUsers[0].id
    Write-Host "User $Email already exists with ID: $userId" -ForegroundColor Yellow
} else {
    Write-Host "Creating user $Email..." -ForegroundColor Cyan
    $userPayload = @{
        username      = $Username
        email         = $Email
        firstName     = $FirstName
        lastName      = $LastName
        enabled       = $true
        emailVerified = $true
        credentials   = @(
            @{
                type      = "password"
                value     = $Password
                temporary = $false
            }
        )
    } | ConvertTo-Json -Depth 4

    Invoke-RestMethod -Method Post -Uri "http://localhost:8080/admin/realms/cms/users" -Headers $headers -Body $userPayload
    $createdUsers = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/admin/realms/cms/users?email=$Email" -Headers $headers
    $userId = $createdUsers[0].id
    Write-Host "Created user successfully (ID: $userId)" -ForegroundColor Green
}

# 3. Assign 'client' Role
$roleBody = "[{`"id`":`"$($clientRole.id)`",`"name`":`"client`"}]"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/admin/realms/cms/users/$userId/role-mappings/realm" -Headers $headers -Body $roleBody
Write-Host "Role 'client' successfully assigned to $Email." -ForegroundColor Green

Write-Host "`nClient user is ready to sign in at http://localhost:4200/login" -ForegroundColor Magenta
Write-Host "  Username : $Username"
Write-Host "  Password : $Password"
