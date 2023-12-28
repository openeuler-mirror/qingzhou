@echo off

if exist %~dp0..\JAVA_HOME.txt (
	set /P CUSTOM_JAVA_HOME=<%~dp0..\JAVA_HOME.txt
)
if defined CUSTOM_JAVA_HOME (
	set "JAVA_HOME=%CUSTOM_JAVA_HOME%"
)
if defined CUSTOM_JAVA_HOME (
	set "PATH=%JAVA_HOME%\bin;%PATH%"
)

rem java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777 -jar "../qingzhou-launcher.jar" %*
java -jar "../qingzhou-launcher.jar" %*
