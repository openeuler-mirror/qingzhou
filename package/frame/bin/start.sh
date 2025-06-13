#!/bin/sh

if [ -z "${qingzhou_home+x}" ] || [ ! -d "${qingzhou_home}" ]; then
  export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")
fi

# 设定要启动的 instance
if [ -n "$1" ]; then
    runInstance=$1
else
    runInstance="instance1"
fi

instanceDir="${qingzhou_home}/instances/${runInstance}"
if ! [ -d "${instanceDir}" ]; then
    echo "Instance does not exist: ${runInstance}"
    exit 1
fi

# java 启动参数
#startCmd="$(java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777 -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" start-args "${runInstance}" 2>&1)"
startCmd="$(java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" start-args "${runInstance}" 2>&1)"

# 判断返回码，给出错误提示信息
exit_code=$?
if [ $exit_code -ne 0 ]; then
    echo "${startCmd}"
    exit $exit_code
fi

# 设置工作目录，保障 qingzhou.json 里的 logs/jvm/jvm.log 能识别准确
cd "${instanceDir}"
exec ${startCmd}