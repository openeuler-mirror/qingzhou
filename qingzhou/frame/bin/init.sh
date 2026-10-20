#!/bin/sh

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

#java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777 -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" init
java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" init
