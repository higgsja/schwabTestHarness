package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.exception.*;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Debug utility to diagnose token refresh issues
 */
public class TokenRefreshDebugger {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshDebugger.class);
    
    public static void main(String[] args)
    throws SchwabApiException {
        System.out.println("=".repeat(60));
        System.out.println("           TOKEN REFRESH DEBUGGER");
        System.out.println("=".repeat(60));
        
        debugTokenRefresh();
    }
    
    /**
     * Comprehensive token refresh debugging
     */
    public static void debugTokenRefresh() 
    throws SchwabApiException{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.systemDefault());
        Instant now = Instant.now();
        
        System.out.println("Current Time: " + formatter.format(now));
        System.out.println();
        
        // Step 1: Check current tokens without auto-refresh
        System.out.println("STEP 1: Loading tokens without auto-refresh");
        TokenResponse tokensNoRefresh = TokenManager.loadTokens(false);
        
        if (tokensNoRefresh == null) {
            System.out.println("No tokens found - authorization required");
            return;
        }
        
        analyzeTokenStatus(tokensNoRefresh, "BEFORE AUTO-REFRESH");
        
        // Step 2: Check tokens with auto-refresh enabled
        System.out.println("\nSTEP 2: Loading tokens with auto-refresh enabled");
        TokenResponse tokensWithRefresh = TokenManager.loadTokens(true);
        
        if (tokensWithRefresh == null) {
            System.out.println("Auto-refresh failed - no tokens returned");
            return;
        }
        
        analyzeTokenStatus(tokensWithRefresh, "AFTER AUTO-REFRESH");
        
        // Step 3: Compare results
        System.out.println("\nSTEP 3: Comparing results");
        compareTokens(tokensNoRefresh, tokensWithRefresh);
        
        // Step 4: Test manual refresh if needed
        if (!tokensWithRefresh.isAccessTokenValid()) {
            System.out.println("\nSTEP 4: Testing manual refresh");
            testManualRefresh(tokensWithRefresh);
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DIAGNOSTIC COMPLETE");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Analyzes token status with detailed information
     */
    private static void analyzeTokenStatus(TokenResponse tokens, String stage) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.systemDefault());
        Instant now = Instant.now();
        
        System.out.println("┌─ " + stage + " " + "─".repeat(Math.max(1, 45 - stage.length())) + "┐");
        
        // Access token analysis
        if (tokens.getAccessToken() != null) {
            System.out.println("│ Access Token: Present");
            if (tokens.getExpiresAt() != null) {
                System.out.println("│ Expires At: " + formatter.format(tokens.getExpiresAt()));
                long secondsRemaining = tokens.getExpiresAt().getEpochSecond() - now.getEpochSecond();
                
                if (secondsRemaining > 0) {
                    System.out.println("│ Time Remaining: " + formatDuration(secondsRemaining));
                    System.out.println("│ Status: VALID");
                    
                    if (secondsRemaining <= 300) { // 5 minutes
                        System.out.println("│ WARNING: Expires within 5 minutes - should auto-refresh");
                    }
                } else {
                    System.out.println("│ Time Remaining: EXPIRED (" + Math.abs(secondsRemaining) + " seconds ago)");
                    System.out.println("│ Status: EXPIRED");
                }
            } else {
                System.out.println("│ Expires At: Not set");
                System.out.println("│ Status: UNKNOWN");
            }
        } else {
            System.out.println("│ Access Token: MISSING");
        }
        
        // Refresh token analysis
        if (tokens.getRefreshToken() != null) {
            System.out.println("│ Refresh Token: Present");
            if (tokens.getRefreshTokenExpiresAt() != null) {
                System.out.println("│ Refresh Expires: " + formatter.format(tokens.getRefreshTokenExpiresAt()));
                long refreshSecondsRemaining = tokens.getRefreshTokenExpiresAt().getEpochSecond() - now.getEpochSecond();
                
                if (refreshSecondsRemaining > 0) {
                    System.out.println("│ Refresh Valid: YES (" + formatDuration(refreshSecondsRemaining) + " remaining)");
                } else {
                    System.out.println("│ Refresh Valid: EXPIRED");
                }
            } else {
                System.out.println("│ Refresh Valid: UNKNOWN");
            }
        } else {
            System.out.println("│ Refresh Token: MISSING");
        }
        
        System.out.println("└" + "─".repeat(47) + "┘");
    }
    
    /**
     * Compares tokens before and after auto-refresh
     */
    private static void compareTokens(TokenResponse before, TokenResponse after) {
        boolean accessTokenChanged = !java.util.Objects.equals(before.getAccessToken(), after.getAccessToken());
        boolean expirationChanged = !java.util.Objects.equals(before.getExpiresAt(), after.getExpiresAt());
        
        System.out.println("┌─ COMPARISON RESULTS ─────────────────────────┐");
        System.out.println("│ Access Token Changed: " + (accessTokenChanged ? "YES" : "NO"));
        System.out.println("│ Expiration Changed: " + (expirationChanged ? "YES" : "NO"));
        
        if (accessTokenChanged || expirationChanged) {
            System.out.println("│ Result: AUTO-REFRESH WORKED");
        } else {
            System.out.println("│ Result: NO REFRESH OCCURRED");
            
            // Analyze why refresh didn't occur
            if (before.isAccessTokenValid()) {
                System.out.println("│ Reason: Token was still valid");
            } else if (!before.isRefreshTokenValid()) {
                System.out.println("│ Reason: Refresh token expired/missing");
            } else {
                System.out.println("│ Reason: UNKNOWN - Check logs");
            }
        }
        System.out.println("└──────────────────────────────────────────────┘");
    }
    
    /**
     * Tests manual refresh
     */
    private static void testManualRefresh(TokenResponse currentTokens) {
        System.out.println("Attempting manual token refresh...");
        
        if (currentTokens.getRefreshToken() == null) {
            System.out.println("Cannot refresh: No refresh token available");
            return;
        }
        
        if (!currentTokens.isRefreshTokenValid()) {
            System.out.println("Cannot refresh: Refresh token is expired");
            return;
        }
        
        try {
            TokenResponse refreshed = TokenManager.forceTokenRefresh();
            
            if (refreshed != null && refreshed.isAccessTokenValid()) {
                System.out.println("Manual refresh successful!");
                analyzeTokenStatus(refreshed, "AFTER MANUAL REFRESH");
            } else {
                System.out.println("Manual refresh failed");
            }
        } catch (Exception e) {
            System.out.println("Manual refresh error: " + e.getMessage());
        }
    }
    
    /**
     * Formats duration in seconds to human-readable format
     */
    private static String formatDuration(long seconds) {
        if (seconds < 0) {
            return "Expired";
        } else if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }
}