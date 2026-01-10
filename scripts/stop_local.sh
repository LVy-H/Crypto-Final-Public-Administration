#!/bin/bash

PID_FILE="pids.txt"

if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  No PID file found. Are services running?"
    exit 1
fi

echo "🛑 Stopping Backend Services..."

while read PID; do
    if ps -p $PID > /dev/null; then
        kill $PID
        echo "   ✅ Killed process $PID"
    else
        echo "   ⚠️  Process $PID not found"
    fi
done < $PID_FILE

rm $PID_FILE
echo "✅ All services stopped."
