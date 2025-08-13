#!/bin/sh

LEO_CDP_FOLDER="/build/leo-cdp"
BUILD_VERSION="v_0.8.9"

JAR_MAIN="leo-observer-starter-$BUILD_VERSION.jar"
OBSERVER_HTTP_ROUTER_KEY="localLeoObserverWorker"

if [ -z "$LEO_CDP_FOLDER" ]
then
      echo "Skip cd to LEO_CDP_FOLDER, just starting ..."
else
      echo "The path: $LEO_CDP_FOLDER is current folder"
      cd $LEO_CDP_FOLDER
fi

JVM_PARAMS="-Xms1G -Xmx2G -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"

kill -15 $(pgrep -f "$OBSERVER_HTTP_ROUTER_KEY")
sleep 4

java -jar $JVM_PARAMS $JAR_MAIN $OBSERVER_HTTP_ROUTER_KEY >> observer.log 2>&1 &
