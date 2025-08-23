#!/bin/bash

# Schwab API Data Collection for Cron Jobs
# Exit codes: 0 = success, 1 = failure

PROJECT_DIR="/home/omega/OneDrive/Documents/Dev/schwab/schwabProject/schwab-test-harness"
LOG_FILE="/tmp/schwab-cron.log"

cd "$PROJECT_DIR"

# Function to log with timestamp
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log "🚀 Starting Schwab data collection"

# Step 1: Check token health
log "🔍 Checking token health..."
if ! ./check-tokens.sh > /dev/null 2>&1; then
    log "❌ Token health check failed - manual intervention required"
    exit 1
fi

log "✅ Tokens are healthy"

# Step 2: Simulate data collection (replace with your actual logic)
log "📊 Collecting market data..."

# Example: You could run Java code here that uses auto-refresh
# For now, we'll simulate it
sleep 2

# Example of what you might do:
# - Get quotes for specific symbols
# - Check market hours
# - Save data to database/files
# - Send notifications

log "✅ Data collection completed successfully"

# Step 3: Check if tokens were refreshed during execution
log "🔍 Post-execution token check..."
./check-tokens.sh >> "$LOG_FILE" 2>&1

log "🎯 Cron job completed successfully"
exit 0
