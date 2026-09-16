#!/bin/sh

# 口令以不回显方式读取，避免进入 shell 历史与终端输出。

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

printf 'New password: '
stty -echo
read password
stty echo
printf '\n'

if [ -z "${password}" ]; then
    echo "password cannot be empty"
    exit 1
fi

java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" cipher-password "${password}"
