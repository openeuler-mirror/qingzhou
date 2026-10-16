@echo off
setlocal enabledelayedexpansion

rem 本脚本位于 bin\win 下，实例目录与 bin 同级，故回退两级得到 qingzhou 根目录
if "%qingzhou_home%"=="" (
    for %%a in ("%~dp0..\..") do set qingzhou_home=%%~fa
)

java -jar "%qingzhou_home%\bin\qingzhou-launcher.jar" init %*
