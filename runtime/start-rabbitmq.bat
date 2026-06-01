@echo off
set "ERLANG_HOME=%~dp0erlang"
set "RABBITMQ_BASE=%~dp0rabbitmq\data"
set "RABBITMQ_HOME=%~dp0rabbitmq\rabbitmq_server-4.1.3"

REM Add erts bin to PATH for epmd
for /d %%i in ("%ERLANG_HOME%\erts-*") do set "ERTS_BIN=%%i\bin"
set "PATH=%ERLANG_HOME%\bin;%ERTS_BIN%;%PATH%"

echo Erlang Home: %ERLANG_HOME%
echo ERTS Bin: %ERTS_BIN%
echo RabbitMQ Base: %RABBITMQ_BASE%

REM Ensure data dir exists
if not exist "%RABBITMQ_BASE%" mkdir "%RABBITMQ_BASE%"

REM Kill any stale epmd
"%ERTS_BIN%\epmd.exe" -kill 2>nul
timeout /t 2 /nobreak >nul

REM Start epmd daemon
start "" "%ERTS_BIN%\epmd.exe" -daemon
timeout /t 2 /nobreak >nul

REM Enable management plugin
call "%RABBITMQ_HOME%\sbin\rabbitmq-plugins.bat" enable rabbitmq_management 2>nul

REM Start RabbitMQ server
call "%RABBITMQ_HOME%\sbin\rabbitmq-server.bat"
