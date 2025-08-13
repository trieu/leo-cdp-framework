#!/bin/sh

KAFKA_HOME="/home/trieu/Programs/kafka_2.11-0.10.2.1"
ZK_URL="localhost:2181"


$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZK_URL --describe
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper $ZK_URL --replication-factor 1 --partitions 1 --topic content-request
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper $ZK_URL --replication-factor 1 --partitions 1 --topic content-view
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper $ZK_URL --replication-factor 1 --partitions 1 --topic content-notify
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper $ZK_URL --describe
