# 在 JDK 1.8 下构建轻舟应用工程；可选把产物放入实例 apps 目录（同名不覆盖）。
param(
    [Parameter(Mandatory = $true)][string]$Project,
    [string]$Apps
)

$ErrorActionPreference = 'Stop'
$Project = (Resolve-Path $Project).Path
$jarName = (Split-Path $Project -Leaf) + '.jar'

if (-not (Test-Path (Join-Path $Project 'pom.xml'))) { throw "未找到 pom.xml: $Project" }
$sources = Get-ChildItem -Path (Join-Path $Project 'src/main/java') -Filter *.java -Recurse -ErrorAction SilentlyContinue
if (-not $sources) { throw "工程内没有 Java 源码: $Project/src/main/java" }

$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac.exe' }
if (-not (Get-Command $javac -ErrorAction SilentlyContinue)) {
    throw '未找到 javac，请使用完整 JDK 1.8 并配置 JAVA_HOME'
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java.exe' }
$spec = (& $java -XshowSettings:properties -version 2>&1 | Select-String 'java.specification.version =').ToString()
if ($spec -notmatch '1\.8') { throw "当前 Java 版本不是 1.8，请使用 JDK 1.8 构建: $spec" }

$log = Join-Path $Project 'build.log'
Remove-Item $log -ErrorAction SilentlyContinue
$maven = if (Get-Command mvn.cmd -ErrorAction SilentlyContinue) { 'mvn.cmd' } else { 'mvn.exe' }
$build = Start-Process -FilePath cmd.exe -WorkingDirectory $Project -WindowStyle Hidden -PassThru `
    -ArgumentList "/d /c `"$maven -B clean package > build.log 2>&1`""
$null = $build.Handle
try {
    $finished = $build.WaitForExit(120000)
    if (-not $finished) { $status = 124 } else { $status = $build.ExitCode }
} finally {
    if (-not $build.HasExited) { taskkill.exe /PID $build.Id /T /F | Out-Null }
}

if ($status -ne 0) {
    Write-Output "构建失败，未放置 JAR。工程: $Project"
    Write-Output "完整日志: $log"
    Select-String -Path $log -Pattern '\[ERROR\]' | Select-Object -First 20 | ForEach-Object { $_.Line }
    if ($status -eq 124) { Write-Output '构建超时（120 秒）。' }
    exit 1
}

$jar = Join-Path $Project (Join-Path 'target' $jarName)
if (-not (Test-Path $jar)) { throw "构建结束但未找到预期产物: $jar，请检查 POM 的 finalName" }
Write-Output "构建成功: $jar"

if ($Apps) {
    $Apps = (Resolve-Path $Apps -ErrorAction SilentlyContinue).Path
    if (-not $Apps) { throw "apps 目录不存在: $Apps" }
    $destination = Join-Path $Apps $jarName
    if (Test-Path $destination) {
        throw "未放置: $destination 已存在。请改用新的应用标识或人工处理旧包。"
    }
    Copy-Item $jar $destination
    Write-Output "JAR 已放入: $destination"
    Write-Output '请配置服务地址与凭据后手动重启轻舟；当前尚未加载或验证运行结果。'
}
