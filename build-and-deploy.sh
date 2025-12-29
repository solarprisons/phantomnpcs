#!/bin/bash

# Build and Deploy Script for Phantom NPC Test Plugin
# Builds the project, deploys to test server, and restarts the server

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PLUGIN_DIR="/home/connor/Desktop/PaperMC-1.21/plugins"
SERVER_DIR="/home/connor/Desktop/PaperMC-1.21"
START_SCRIPT="$SERVER_DIR/start.sh"

echo -e "${BLUE}=== Phantom NPC Build and Deploy ===${NC}"
echo ""

# Step 1: Clean build with dependency refresh
echo -e "${YELLOW}[1/5] Building project (clean + refresh dependencies)...${NC}"
./gradlew clean build --refresh-dependencies

if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed! Aborting deployment.${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful!${NC}"
echo ""

# Step 2: Find the built JAR
echo -e "${YELLOW}[2/5] Finding test-plugin JAR...${NC}"
TEST_PLUGIN_JAR=$(find test-plugin/build/libs -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -n 1)

if [ -z "$TEST_PLUGIN_JAR" ]; then
    echo -e "${RED}Could not find test-plugin JAR in test-plugin/build/libs/${NC}"
    exit 1
fi

echo -e "${GREEN}Found: $TEST_PLUGIN_JAR${NC}"
echo ""

# Step 3: Check if server directory exists
echo -e "${YELLOW}[3/5] Checking server directory...${NC}"
if [ ! -d "$SERVER_DIR" ]; then
    echo -e "${RED}Server directory not found: $SERVER_DIR${NC}"
    exit 1
fi

if [ ! -d "$PLUGIN_DIR" ]; then
    echo -e "${YELLOW}Plugins directory not found, creating: $PLUGIN_DIR${NC}"
    mkdir -p "$PLUGIN_DIR"
fi

if [ ! -f "$START_SCRIPT" ]; then
    echo -e "${RED}Start script not found: $START_SCRIPT${NC}"
    exit 1
fi

echo -e "${GREEN}Server directory OK${NC}"
echo ""

# Step 4: Stop existing server if running
echo -e "${YELLOW}[4/5] Checking for running server...${NC}"

# Find Minecraft server process (looking for java process running from server directory)
# Try multiple patterns to catch different server jar names
SERVER_PID=$(ps aux | grep -E "java.*-jar.*server\.jar|java.*-jar.*paper.*\.jar|java.*-jar.*spigot.*\.jar|java.*-jar.*asp-server\.jar" | grep -v grep | awk '{print $2}' | head -n 1 || true)

# Alternative: check for process running in server directory
if [ -z "$SERVER_PID" ]; then
    SERVER_PID=$(lsof -t "$SERVER_DIR/server.properties" 2>/dev/null || true)
fi

# Alternative: check for any java with nogui argument in server directory
if [ -z "$SERVER_PID" ]; then
    SERVER_PID=$(ps aux | grep "java.*nogui" | grep -v grep | awk '{print $2}' | head -n 1 || true)
fi

if [ -n "$SERVER_PID" ]; then
    echo -e "${YELLOW}Found running server (PID: $SERVER_PID), stopping...${NC}"

    # Send stop command to server console if possible (graceful shutdown)
    if command -v screen &> /dev/null; then
        # Try to find screen session
        SCREEN_SESSION=$(screen -ls | grep -oP '\d+\.\w+' | head -n 1 || true)
        if [ -n "$SCREEN_SESSION" ]; then
            echo -e "${YELLOW}Sending 'stop' command to server...${NC}"
            screen -S "$SCREEN_SESSION" -X stuff "stop^M"
            sleep 3
        fi
    fi

    # If still running, send SIGTERM
    if kill -0 "$SERVER_PID" 2>/dev/null; then
        echo -e "${YELLOW}Sending SIGTERM to server process...${NC}"
        kill "$SERVER_PID" 2>/dev/null || true
    fi

    # Wait for process to stop (max 20 seconds)
    for i in {1..20}; do
        if ! kill -0 "$SERVER_PID" 2>/dev/null; then
            echo -e "${GREEN}Server stopped successfully${NC}"
            break
        fi
        echo -e "${YELLOW}Waiting for server to stop... ($i/20)${NC}"
        sleep 1
    done

    # Force kill if still running
    if kill -0 "$SERVER_PID" 2>/dev/null; then
        echo -e "${RED}Server did not stop gracefully, force killing...${NC}"
        kill -9 "$SERVER_PID" 2>/dev/null || true
        sleep 2
    fi

    # Verify server is actually stopped
    if kill -0 "$SERVER_PID" 2>/dev/null; then
        echo -e "${RED}ERROR: Failed to stop server! Aborting deployment.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}No running server found${NC}"
fi

# Remove old plugin JARs (clean up previous versions)
echo -e "${YELLOW}Removing old plugin JARs...${NC}"
rm -f "$PLUGIN_DIR"/phantom-test-*.jar "$PLUGIN_DIR"/test-plugin-*.jar "$PLUGIN_DIR"/phantomtest-*.jar

echo ""

# Step 5: Deploy new plugin
echo -e "${YELLOW}[5/5] Deploying plugin...${NC}"
cp "$TEST_PLUGIN_JAR" "$PLUGIN_DIR/"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Plugin deployed successfully!${NC}"
    JAR_NAME=$(basename "$TEST_PLUGIN_JAR")
    echo -e "${GREEN}Location: $PLUGIN_DIR/$JAR_NAME${NC}"
else
    echo -e "${RED}Failed to copy plugin JAR${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}=== Build and Deploy Complete ===${NC}"
echo ""

# Step 6: Start the server
echo -e "${YELLOW}Starting server...${NC}"
cd "$SERVER_DIR"

# Check if start.sh is executable
if [ ! -x "$START_SCRIPT" ]; then
    echo -e "${YELLOW}Making start.sh executable...${NC}"
    chmod +x "$START_SCRIPT"
fi

# Run start script
bash "$START_SCRIPT"
