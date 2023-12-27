#!/bin/sh
ulimit -n 65535 >/dev/null 2>&1

if [ -e $(cd "$(dirname "$0")";pwd)/JAVA_HOME.txt ] ; then
  CUSTOM_JAVA_HOME=$(cat $(cd "$(dirname "$0")";pwd)/JAVA_HOME.txt)
  if [ -n "$CUSTOM_JAVA_HOME" ]; then
    JAVA_HOME=$CUSTOM_JAVA_HOME
    PATH=$JAVA_HOME/bin:$PATH
    export JAVA_HOME
    export PATH
  fi
fi

# java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=7777 -jar "./qingzhou-launcher.jar" $@
java -jar "./qingzhou-launcher.jar" $@
