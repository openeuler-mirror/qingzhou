#!/bin/sh

export qingzhou_home=$(dirname -- "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd -P)")

java -jar "${qingzhou_home}/bin/qingzhou-launcher.jar" cipher-key
