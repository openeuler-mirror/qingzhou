@echo off
setlocal enabledelayedexpansion

rem 本脚本位于 bin\win 下，实例目录与 bin 同级，故回退两级得到 qingzhou 根目录
if "%qingzhou_home%"=="" (
    for %%a in ("%~dp0..\..") do set qingzhou_home=%%~fa
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