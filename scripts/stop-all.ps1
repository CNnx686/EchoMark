# EchoMark - Stop All Services

Write-Host "=== EchoMark - Stopping All Services ===" -ForegroundColor Cyan

# Kill all Java (Spring Boot) processes
Write-Host "Stopping Java services..." -ForegroundColor Yellow
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  Java processes stopped" -ForegroundColor Green

# Kill Node (Vite frontend)
Write-Host "Stopping Frontend..." -ForegroundColor Yellow
Get-Process -Name "node" -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  Node processes stopped" -ForegroundColor Green

# Kill Python (Resource Server)
Write-Host "Stopping Resource Server..." -ForegroundColor Yellow
Get-Process -Name "python" -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  Python processes stopped" -ForegroundColor Green

# Kill Redis
Write-Host "Stopping Redis..." -ForegroundColor Yellow
Get-Process -Name "redis-server" -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "  Redis stopped" -ForegroundColor Green

Write-Host "`nAll services stopped." -ForegroundColor Green
