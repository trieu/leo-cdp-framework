#!/bin/bash

APP_NAME="ai_agent_trigger_leocdp"
PID_FILE="/tmp/${APP_NAME}.pid"
APP_FILE="./airflow-dags/ai_agent_trigger_leocdp.py"
LOG_FILE="./airflow-output/ai_agent_trigger_leocdp.log"

start() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$APP_NAME already running with PID $(cat $PID_FILE)"
        exit 1
    fi

    echo "Starting $APP_NAME..."
    nohup python3 "$APP_FILE" >> "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "$APP_NAME started with PID $(cat $PID_FILE)"
}

stop() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "Stopping $APP_NAME..."
        kill $(cat "$PID_FILE")
        rm -f "$PID_FILE"
        echo "$APP_NAME stopped."
    else
        echo "$APP_NAME not running."
    fi
}

status() {
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
        echo "$APP_NAME is running with PID $(cat $PID_FILE)."
    else
        echo "$APP_NAME is not running."
    fi
}

restart() {
    stop
    sleep 2
    start
}

case "$1" in
    start) start ;;
    stop) stop ;;
    restart) restart ;;
    status) status ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
