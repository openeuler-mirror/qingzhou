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

rem java 启动参数走 stdout 单独捕获：stderr 是 launcher 的告警通道（如 Windows 下临时目录删不掉），
rem 合并会把告警当成启动命令执行；for /f 还会丢失退出码，故用临时文件传递
set "startArgFile=%TEMP%\qingzhou-start-arg-%RANDOM%.txt"
java -jar "%qingzhou_home%\bin\qingzhou-launcher.jar" start-arg "%runInstance%" > "%startArgFile%"
if errorlevel 1 (
    type "%startArgFile%"
    del "%startArgFile%"
    exit /b 1
)
set /p startCmd=<"%startArgFile%"
del "%startArgFile%"

rem 设置工作目录，保障 qingzhou.json 里的 logs/jvm/jvm.log 能识别准确
cd /d "%instanceDir%"
%startCmd%