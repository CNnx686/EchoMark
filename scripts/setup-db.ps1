# EchoMark Database Setup Script
# Prerequisite: MySQL 8.0+ installed and running, root user accessible
param(
    [string]$MySQLHost = "localhost",
    [string]$MySQLPort = "3306",
    [string]$MySQLUser = "root",
    [string]$MySQLPassword = ""
)

$mysqlBin = "mysql"
$schemaFile = Join-Path $PSScriptRoot ".." "schema.sql"

Write-Host "=== EchoMark Database Setup ===" -ForegroundColor Cyan

# Try to find mysql client
if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    $possiblePath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (Test-Path $possiblePath) {
        $mysqlBin = $possiblePath
    } else {
        Write-Error "MySQL client not found. Please install MySQL 8.0+ or add it to PATH."
        exit 1
    }
}

if (-not (Test-Path $schemaFile)) {
    Write-Error "schema.sql not found at: $schemaFile"
    exit 1
}

# Build connection args
$connArgs = @("-u", $MySQLUser, "-h", $MySQLHost, "-P", $MySQLPort)
if ($MySQLPassword) {
    $connArgs += "-p$MySQLPassword"
}

# Create database
Write-Host "Creating database..." -ForegroundColor Yellow
$result = & $mysqlBin @connArgs -e "CREATE DATABASE IF NOT EXISTS sound_map DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create database: $result"
    Write-Host "Try: .\setup-db.ps1 -MySQLPassword 'your_password'" -ForegroundColor Yellow
    exit 1
}

# Run schema
Write-Host "Running schema.sql..." -ForegroundColor Yellow
Get-Content $schemaFile | & $mysqlBin @connArgs sound_map 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Schema import failed."
    exit 1
}

Write-Host "Database 'sound_map' created successfully!" -ForegroundColor Green
Write-Host "JDBC URL: jdbc:mysql://${MySQLHost}:${MySQLPort}/sound_map" -ForegroundColor Cyan
