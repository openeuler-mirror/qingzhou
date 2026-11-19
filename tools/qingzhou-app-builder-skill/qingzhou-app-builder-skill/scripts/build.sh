#!/usr/bin/env bash
# 在 JDK 1.8 下构建轻舟应用工程；可选把产物放入实例 apps 目录（同名不覆盖）。
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "用法: $0 <工程目录> [apps 目录]" >&2
    exit 1
fi
project=$(cd "$1" 2>/dev/null && pwd) || {
    echo "工程目录不存在: $1" >&2
    exit 1
}
apps=${2:-}
jar_name=$(basename "$project").jar

[ -f "$project/pom.xml" ] || {
    echo "未找到 $project/pom.xml，请先创建工程" >&2
    exit 1
}
[ -n "$(find "$project/src/main/java" -name '*.java' -print -quit 2>/dev/null)" ] || {
    echo "工程内没有 Java 源码: $project/src/main/java" >&2
    exit 1
}

if [ -n "${JAVA_HOME:-}" ]; then
    java_bin="$JAVA_HOME/bin/java"
    javac_bin="$JAVA_HOME/bin/javac"
else
    java_bin=java
    javac_bin=javac
fi
[ -x "$javac_bin" ] || javac_bin=$(command -v javac 2>/dev/null || true)
[ -n "$javac_bin" ] || {
    echo "未找到 javac，请使用完整 JDK 1.8 并配置 JAVA_HOME" >&2
    exit 1
}
spec=$("$java_bin" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p')
[ "$spec" = "1.8" ] || {
    echo "当前 Java 版本为 ${spec:-未知}，请使用 JDK 1.8 构建" >&2
    exit 1
}

command=(mvn -B clean package)
if command -v perl >/dev/null 2>&1; then
    command=(perl -e 'alarm shift; exec @ARGV' 120 mvn -B clean package)
fi

log="$project/build.log"
rm -f "$log"
set +e
(cd "$project" && "${command[@]}") > "$log" 2>&1
status=$?
set -e

if [ $status -ne 0 ]; then
    echo "构建失败，未放置 JAR。工程: $project"
    echo "完整日志: $log"
    grep -F '[ERROR]' "$log" | head -20 || echo "请人工查看完整构建日志。"
    exit 1
fi

jar="$project/target/$jar_name"
[ -f "$jar" ] || {
    echo "构建结束但未找到预期产物: $jar，请检查 POM 的 finalName" >&2
    exit 1
}
echo "构建成功: $jar"

if [ -n "$apps" ]; then
    destination="$apps/$jar_name"
    if [ -e "$destination" ]; then
        echo "未放置: $destination 已存在。请改用新的应用标识或人工处理旧包。" >&2
        exit 1
    fi
    mkdir -p "$apps"
    staged=$(mktemp "$apps/app-builder-XXXXXX.part")
    cp "$jar" "$staged"
    mv "$staged" "$destination"
    echo "JAR 已放入: $destination"
    echo "请配置服务地址与凭据后手动重启轻舟；当前尚未加载或验证运行结果。"
fi
