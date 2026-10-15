#!/bin/sh

# 单入口：无参数进入交互向导；带参数直接执行对应命令（用法：bin/qingzhou.sh --help）
export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

if [ $# -eq 0 ]; then
    set -- wizard
fi

exec java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" "$@"
