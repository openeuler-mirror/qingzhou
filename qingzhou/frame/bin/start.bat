@echo off
setlocal enabledelayedexpansion

if "%qingzhou_home%"=="" (
    set qingzhou_home=%~dp0
    set qingzhou_home=!qingzhou_home:~0,-1!
    for %%a in ("!qingzhou_home!") do set qingzhou_home=%%~dpa
    set qingzhou_home=!qingzhou_home:~0,-1!
)

rem 设定要启动的 instance
if not "%~1"=="" (
    set runInstance=%~1
) else (
    set runInstance=default
)

set instanceDir=%qingzhou_home%\instances\%runInstance%
if not exist "%instanceDir%" (
    echo Instance does not exist: %runInstance%
    exit /b 1
)

rem java 启动参数
for /f "delims=" %%i in ('java -jar "%qingzhou_home%\bin\qingzhou-launcher.jar" start-arg "%runInstance%" 2^>^&1') do set startCmd=%%i

rem 判断返回码，给出错误提示信息
if %errorlevel% neq 0 (
    echo !startCmd!
    exit /b %errorlevel%
)

rem 设置工作目录，保障 qingzhou.json 里的 logs/jvm/jvm.log 能识别准确
cd /d "%instanceDir%"
%startCmd%