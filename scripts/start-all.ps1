# EchoMark - Start All Services
# Starts Redis, Resource Server, Gateway, all 8 microservices, and Frontend

param(
    [switch]$SkipBuild = $false
)

$projectRoot = Join-Path $PSScriptRoot ".."
$runtimeDir = Join-Path $projectRoot "runtime"
$backendsDir = Join-Path $projectRoot "programming\Backends"
$frontendDir = Join-Path $projectRoot "programming\Frontends"

Write-Host "=== EchoMark - Starting All Services ===" -ForegroundColor Cyan

# 1. Redis
Write-Host "[1/11] Starting Redis..." -ForegroundColor Yellow
$redisExe = Join-Path $runtimeDir "redis\redis-server.exe"
if (Test-Path $redisExe) {
    Start-Process -FilePath $redisExe -WindowStyle Hidden
    Write-Host "  Redis started (port 6379)" -ForegroundColor Green
} else {
    Write-Host "  WARNING: Redis not found at $redisExe, skipping" -ForegroundColor Red
}
Start-Sleep -Seconds 2

# 2. Resource Server
Write-Host "[2/11] Starting Resource Server (port 5000)..." -ForegroundColor Yellow
$rsDir = Join-Path $projectRoot "local_resource_server"
Start-Process -FilePath "python" -ArgumentList "server.py" -WorkingDirectory $rsDir -WindowStyle Minimized
Start-Sleep -Seconds 2

# 3-10. Build if needed
if (-not $SkipBuild) {
    Write-Host "Building backends..." -ForegroundColor Yellow
    $buildResult = mvn -f (Join-Path $projectRoot "pom.xml") clean install -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed. Check Maven output."
        exit 1
    }
}

# Services to start (name, dir-suffix, port)
$services = @(
    @{Name="Gateway";           Dir="gateway";                          Port=8080},
    @{Name="AuthService";       Dir="services\AuthService";             Port=5001},
    @{Name="UserService";       Dir="services\UserService";             Port=5002},
    @{Name="AudioService";      Dir="services\AudioService";            Port=5003},
    @{Name="SocialService";     Dir="services\SocialService";           Port=5004},
    @{Name="NotificationService"; Dir="services\NotificationService";   Port=5005},
    @{Name="SseService";        Dir="services\SseService";              Port=5006},
    @{Name="LLMService";        Dir="services\LLMService";              Port=5007},
    @{Name="UserPersonaService"; Dir="services\UserPersonaService";     Port=5008}
)

$idx = 3
foreach ($svc in $services) {
    Write-Host "[$idx/11] Starting $($svc.Name) (port $($svc.Port))..." -ForegroundColor Yellow
    $svcDir = Join-Path $backendsDir $svc.Dir
    Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-q" -WorkingDirectory $svcDir -WindowStyle Minimized
    Write-Host "  $($svc.Name) launching..." -ForegroundColor Green
    Start-Sleep -Seconds 4
    $idx++
}

# 11. Frontend
Write-Host "[11/11] Starting Frontend (port 5173)..." -ForegroundColor Yellow
Start-Process -FilePath "npm" -ArgumentList "run","dev" -WorkingDirectory $frontendDir -WindowStyle Minimized
Start-Sleep -Seconds 3

Write-Host "`n=== All services launched! ===" -ForegroundColor Green
Write-Host "Frontend:  http://localhost:5173" -ForegroundColor Cyan
Write-Host "Gateway:   http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "Redis:     localhost:6379" -ForegroundColor Cyan
Write-Host "`nUse scripts/stop-all.ps1 to stop all services." -ForegroundColor Gray
