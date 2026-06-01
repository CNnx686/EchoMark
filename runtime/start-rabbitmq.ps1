$erlangHome = "$PSScriptRoot\erlang"
$rabbitHome = "$PSScriptRoot\rabbitmq\rabbitmq_server-4.1.3"
$rabbitBase = "$PSScriptRoot\rabbitmq\data"
$ertsBin = (Get-ChildItem "$erlangHome\erts-*\bin" | Select-Object -First 1).FullName

$env:ERLANG_HOME = $erlangHome
$env:RABBITMQ_BASE = $rabbitBase
$env:RABBITMQ_NODENAME = "rabbit@localhost"
$env:RABBITMQ_NODE_IP_ADDRESS = "127.0.0.1"
$env:PATH = "$erlangHome\bin;$ertsBin;$env:PATH"

New-Item -ItemType Directory -Force -Path $rabbitBase | Out-Null

Write-Host "Starting RabbitMQ from $rabbitHome"
Set-Location "$rabbitHome\sbin"
& ".\rabbitmq-server.bat"
