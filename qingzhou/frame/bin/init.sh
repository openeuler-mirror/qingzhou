#!/bin/sh

# 实例安全配置初始化：交互式初始化全局密钥、密钥对，可选生成 SSL 证书与重置 admin 密码。
# 用法：bin/init.sh [instance]

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" init "$@"
