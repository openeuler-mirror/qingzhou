@echo off
setlocal enabledelayedexpansion

if "%qingzhou_home%"=="" (
    for %%a in ("%~dp0..\..") do set qingzhou_home=%%~fa
)

java -jar "%qingzhou_home%\bin\qingzhou-launcher.jar" init
