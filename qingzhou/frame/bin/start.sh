#!/bin/sh

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

# 设定要启动的 instance
if [ -n "$1" ]; then
    runInstance=$1
else
    runInstance="default"
fi

instanceDir="${qingzhou_home}/instances/${runInstance}"
if ! [ -d "${instanceDir}" ]; then
    echo "Instance does not exist: ${runInstance}"
    exit 1
fi

# java 启动参数只取 stdout：stderr 是 launcher 的告警通道，合并后会被 eval 当成命令执行
startCmd="$(java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" start-arg "${runInstance}")"

# 判断返回码，给出错误提示信息
exit_code=$?
if [ $exit_code -ne 0 ]; then
    echo "${startCmd}"
    exit $exit_code
fi

# 设置工作目录，保障 qingzhou.json 里的 logs/jvm/jvm.log 能识别准确
cd "${instanceDir}"

# 使用eval执行（兼容启动参数中包含中文空格）
eval exec "${startCmd}"