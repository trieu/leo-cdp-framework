#!/bin/sh

LEO_CDP_FOLDER="/build/leo-cdp"
BUILD_VERSION="v_0.8.9"

JAR_MAIN="leo-observer-starter-$BUILD_VERSION.jar"
OBSERVER_HTTP_ROUTER_KEY="datahub"

if [ -z "$LEO_CDP_FOLDER" ]
then
      echo "Skip cd to LEO_CDP_FOLDER, just stopping ..."
else
      echo "The path: $LEO_CDP_FOLDER is current folder"
      cd $LEO_CDP_FOLDER
fi

kill -15 $(pgrep -f "$OBSERVER_HTTP_ROUTER_KEY")
sleep 2
