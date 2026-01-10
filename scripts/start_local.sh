#!/bin/bash

# Define services
SERVICES=("api-gateway" "identity-service" "pki-service" "tsa-service" "document-service")
PID_FILE="pids.txt"

# Clear previous PIDs
> $PID_FILE

echo "🚀 Starting Backend Services (Java 25)..."

for SERVICE in "${SERVICES[@]}"; do
    JAR_PATH="backend/$SERVICE/build/libs/$SERVICE-0.0.1-SNAPSHOT.jar"
    if [ ! -f "$JAR_PATH" ]; then
        echo "❌ Error: JAR not found for $SERVICE at $JAR_PATH"
        echo "   Please run './gradlew build -x test' first."
        exit 1
    fi

    echo "   Starting $SERVICE..."
    nohup java -jar "$JAR_PATH" > "logs/$SERVICE.log" 2>&1 &
    PID=$!
    echo $PID >> $PID_FILE
    echo "   ✅ $SERVICE started (PID: $PID)"
done

echo ""
echo "⏳ Services are starting in the background."
echo "   Logs are available in the 'logs/' directory."
echo "   To stop all services, run: ./scripts/stop_local.sh"
echo ""
