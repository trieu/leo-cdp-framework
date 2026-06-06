#!/usr/bin/env bash
# Shared JDK-25 JVM flags for all LEO CDP starters.
# Source this file, then compose: JVM_PARAMS="<sizing flags> $JAVA25_COMPAT_FLAGS"
# Rationale per flag: docs/02-java-25-migration.md §3.
#
# - --sun-misc-unsafe-memory-access=allow : Netty 4.1.x (via Vert.x 3) uses
#   sun.misc.Unsafe; JDK 24+ (JEP 498) warns and will eventually refuse.
# - --enable-native-access=ALL-UNNAMED    : JNI users (netty-native, sqlite-jdbc,
#   Kafka snappy/zstd) under JEP 472.
# - --add-opens java.nio / sun.nio.ch     : old Netty direct-buffer reflection.
# - --add-opens java.lang(+reflect)/util  : Gson/Jackson/MVEL reflective access.
# - -Dio.netty.tryReflectionSetAccessible : let Netty use the opened nio path.
JAVA25_COMPAT_FLAGS="\
 --sun-misc-unsafe-memory-access=allow \
 --enable-native-access=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.base/java.nio=ALL-UNNAMED \
 --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
 -Dio.netty.tryReflectionSetAccessible=true"
export JAVA25_COMPAT_FLAGS
