#!/bin/bash

# Simple token health checker for cron jobs
# File: check-tokens.sh

# Set the project directory
PROJECT_DIR="/home/omega/OneDrive/Documents/Dev/schwab/schwabProject/schwab-test-harness"
cd "$PROJECT_DIR"

# Function to check if file exists and get modification time
check_token_file() {
    local file="$1"
    local name="$2"
    
    if [ -f "$file" ]; then
        local size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "unknown")
        local modified=$(stat -f%Sm "$file" 2>/dev/null || stat -c%y "$file" 2>/dev/null || echo "unknown")
        echo "✅ $name: exists (${size} bytes, modified: ${modified})"
        return 0
    else
        echo "❌ $name: not found"
        return 1
    fi
}

# Function to analyze token JSON content
analyze_token_content() {
    local file="$1"
    
    if [ ! -f "$file" ]; then
        echo "❌ Token file not found: $file"
        return 1
    fi
    
    # Check if file is valid JSON
    if ! python3 -c "import json; json.load(open('$file'))" 2>/dev/null; then
        echo "❌ Token file is not valid JSON"
        return 1
    fi
    
    # Extract key information using Python
    python3 -c "
import json
import datetime
import sys

try:
    with open('$file', 'r') as f:
        tokens = json.load(f)
    
    now = datetime.datetime.now()
    
    # Check access token
    if 'access_token' in tokens and tokens['access_token']:
        print('✅ Access Token: Present')
        
        if 'expiresAt' in tokens and tokens['expiresAt']:
            try:
                # Parse ISO format timestamp
                expires_at = datetime.datetime.fromisoformat(tokens['expiresAt'].replace('Z', '+00:00'))
                expires_at = expires_at.replace(tzinfo=None)  # Remove timezone for comparison
                
                time_remaining = expires_at - now
                total_seconds = int(time_remaining.total_seconds())
                
                if total_seconds > 0:
                    hours = total_seconds // 3600
                    minutes = (total_seconds % 3600) // 60
                    
                    if hours > 0:
                        print(f'✅ Access Token: Valid for {hours}h {minutes}m')
                    else:
                        print(f'✅ Access Token: Valid for {minutes} minutes')
                        
                    if total_seconds <= 300:  # 5 minutes
                        print('⚠️  WARNING: Access token expires within 5 minutes!')
                else:
                    print(f'❌ Access Token: EXPIRED ({abs(total_seconds)} seconds ago)')
                    
            except Exception as e:
                print(f'❓ Access Token: Expiry format error - {e}')
        else:
            print('❓ Access Token: No expiration info')
    else:
        print('❌ Access Token: Missing')
    
    # Check refresh token
    if 'refresh_token' in tokens and tokens['refresh_token']:
        print('✅ Refresh Token: Present')
        
        if 'refreshTokenExpiresAt' in tokens and tokens['refreshTokenExpiresAt']:
            try:
                refresh_expires_at = datetime.datetime.fromisoformat(tokens['refreshTokenExpiresAt'].replace('Z', '+00:00'))
                refresh_expires_at = refresh_expires_at.replace(tzinfo=None)
                
                refresh_time_remaining = refresh_expires_at - now
                refresh_total_seconds = int(refresh_time_remaining.total_seconds())
                
                if refresh_total_seconds > 0:
                    days = refresh_total_seconds // 86400
                    hours = (refresh_total_seconds % 86400) // 3600
                    
                    if days > 0:
                        print(f'✅ Refresh Token: Valid for {days}d {hours}h')
                    else:
                        print(f'✅ Refresh Token: Valid for {hours} hours')
                        
                    if refresh_total_seconds <= 172800:  # 2 days
                        print('⚠️  WARNING: Refresh token expires within 2 days!')
                else:
                    print(f'❌ Refresh Token: EXPIRED ({abs(refresh_total_seconds)} seconds ago)')
                    
            except Exception as e:
                print(f'❓ Refresh Token: Expiry format error - {e}')
        else:
            print('❓ Refresh Token: No expiration info')
    else:
        print('❌ Refresh Token: Missing')
        
    # Overall status
    access_valid = 'access_token' in tokens and tokens['access_token']
    refresh_valid = 'refresh_token' in tokens and tokens['refresh_token']
    
    if access_valid and refresh_valid:
        print('🎯 Overall Status: Ready for API calls')
    elif refresh_valid:
        print('🎯 Overall Status: Refresh needed')
    else:
        print('🎯 Overall Status: Re-authorization required')
        
except Exception as e:
    print(f'❌ Error analyzing token file: {e}')
    sys.exit(1)
"
}

# Function to test cron compatibility
test_cron_compatibility() {
    echo "🔧 Testing cron job compatibility..."
    
    # Check if we can get tokens programmatically
    local exit_code=0
    
    # Check token files
    echo "📁 Checking token files:"
    check_token_file "schwab_tokens.json" "Main Token File" || exit_code=1
    check_token_file "schwab_tokens.json.backup" "Backup Token File"
    
    echo ""
    echo "📊 Analyzing token content:"
    if [ -f "schwab_tokens.json" ]; then
        analyze_token_content "schwab_tokens.json"
    else
        echo "❌ Cannot analyze - no token file found"
        exit_code=1
    fi
    
    echo ""
    echo "🚀 Cron Compatibility Assessment:"
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ Token files are present and readable"
        echo "✅ Ready for automated execution"
        echo ""
        echo "💡 Next steps for cron setup:"
        echo "   1. Create a wrapper script that calls your Java application"
        echo "   2. Handle token refresh logic programmatically"
        echo "   3. Set up proper logging and error handling"
        echo "   4. Test the script manually before adding to cron"
    else
        echo "❌ Issues detected - manual intervention required"
        echo ""
        echo "💡 Required actions:"
        echo "   1. Complete OAuth authorization to get valid tokens"
        echo "   2. Verify token files are created and readable"
        echo "   3. Test token refresh functionality"
    fi
    
    return $exit_code
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [--check-health|--test-cron|--help]"
    echo ""
    echo "Options:"
    echo "  --check-health    Check current token health status"
    echo "  --test-cron      Test cron job compatibility"
    echo "  --help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Run health check"
    echo "  $0 --check-health     # Same as above"
    echo "  $0 --test-cron        # Test cron compatibility"
}

# Main execution
main() {
    local command="${1:-check-health}"
    
    case "$command" in
        "--check-health"|"check-health"|"")
            echo "🔍 Schwab Token Health Check"
            echo "============================="
            echo "Current Time: $(date)"
            echo "Project Dir: $PROJECT_DIR"
            echo ""
            
            if [ -f "schwab_tokens.json" ]; then
                analyze_token_content "schwab_tokens.json"
            else
                echo "❌ No token file found - authorization required"
                exit 1
            fi
            ;;
            
        "--test-cron"|"test-cron")
            test_cron_compatibility
            ;;
            
        "--help"|"help"|"-h")
            show_usage
            ;;
            
        *)
            echo "❌ Unknown option: $command"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Check if Python3 is available
if ! command -v python3 &> /dev/null; then
    echo "❌ Error: python3 is required but not installed"
    echo "   Please install Python 3 to use this script"
    exit 1
fi

# Run main function with all arguments
main "$@"
