#!/bin/sh
JVM_PARAMS="-Xms1G -Xmx2G -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"
JAR_MAIN="leo-main-starter-v_0.20201111-1235.jar"

cd /leocdp/

kill -15 $(pgrep -f "leo-main-starter-")
sleep 3

java -jar $JVM_PARAMS $JAR_MAIN  >> /dev/null 2>&1 &
