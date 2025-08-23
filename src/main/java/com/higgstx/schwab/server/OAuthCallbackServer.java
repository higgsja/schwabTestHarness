package com.higgstx.schwab.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A simple HTTP server to handle the OAuth 2.0 redirect callback.
 */
public class OAuthCallbackServer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OAuthCallbackServer.class);
    private final CompletableFuture<String> authCodeFuture = new CompletableFuture<>();
    private HttpServer server;
    private volatile boolean isStarted = false;

    /**
     * Starts the server and waits for the authorization code.
     *
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the timeout argument.
     * @return A {@link CompletableFuture} that will be completed with the authorization code.
     */
    public CompletableFuture<String> startAndWaitForCode(long timeout, TimeUnit unit) {
        try {
            // Create server on any available port
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", new AuthCodeHandler());
            server.setExecutor(null); // Use default executor
            server.start();
            isStarted = true;

            int port = server.getAddress().getPort();
            logger.info("✅ Started local OAuth callback server on port {}. Awaiting callback.", port);

            // Set up timeout handling
            return authCodeFuture
                .orTimeout(timeout, unit)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof java.util.concurrent.TimeoutException) {
                            logger.warn("⏰ Authorization timed out after {} {}.", timeout, unit.toString().toLowerCase());
                        } else {
                            logger.error("❌ Authorization failed: {}", throwable.getMessage());
                        }
                    } else if (result != null) {
                        logger.info("✅ Authorization code received successfully");
                    }
                    
                    // Stop server after completion or timeout
                    stopServer();
                });

        } catch (IOException e) {
            logger.error("❌ Failed to start local server for OAuth callback: {}", e.getMessage(), e);
            authCodeFuture.completeExceptionally(e);
            return authCodeFuture;
        }
    }

    /**
     * Gets the port number the server is running on
     */
    public int getPort() {
        if (server != null && isStarted) {
            return server.getAddress().getPort();
        }
        return -1;
    }

    /**
     * Checks if the server is currently running
     */
    public boolean isRunning() {
        return isStarted && server != null;
    }

    /**
     * Stops the server
     */
    private void stopServer() {
        if (server != null && isStarted) {
            try {
                server.stop(1); // Stop with 1 second delay
                isStarted = false;
                logger.debug("🛑 OAuth callback server stopped");
            } catch (Exception e) {
                logger.warn("Warning during server shutdown: {}", e.getMessage());
            }
        }
    }

    private class AuthCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            logger.debug("Received {} request to {} with query: {}", method, path, query);
            
            try {
                if (query != null && query.contains("code=")) {
                    String authCode = extractAuthCode(query);
                    if (authCode != null && !authCode.isEmpty()) {
                        logger.info("✅ Authorization code received: {}...", authCode.substring(0, Math.min(10, authCode.length())));
                        authCodeFuture.complete(authCode);
                        sendSuccessResponse(exchange);
                    } else {
                        logger.error("❌ Failed to extract authorization code from query: {}", query);
                        authCodeFuture.completeExceptionally(new RuntimeException("Failed to extract authorization code"));
                        sendErrorResponse(exchange, "Failed to extract authorization code from the callback.");
                    }
                } else if (query != null && query.contains("error=")) {
                    String error = extractErrorInfo(query);
                    logger.error("❌ OAuth error received: {}", error);
                    authCodeFuture.completeExceptionally(new RuntimeException("OAuth error: " + error));
                    sendErrorResponse(exchange, "Authorization was denied or failed: " + error);
                } else {
                    logger.error("❌ No authorization code or error found in callback. Query: {}", query);
                    authCodeFuture.completeExceptionally(new RuntimeException("No authorization code found in callback"));
                    sendErrorResponse(exchange, "No authorization code found in the callback.");
                }
            } catch (IOException e) {
                logger.error("❌ Error processing OAuth callback: {}", e.getMessage(), e);
                authCodeFuture.completeExceptionally(e);
                sendErrorResponse(exchange, "Error processing authorization callback: " + e.getMessage());
            }
        }

        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            String response = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Authorization Successful</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; background: #f0f8ff; }
                        .success { color: #28a745; font-size: 24px; margin-bottom: 20px; }
                        .message { color: #333; font-size: 16px; }
                    </style>
                </head>
                <body>
                    <div class="success">✅ Authorization Successful!</div>
                    <div class="message">
                        <p>Your Schwab API authorization was completed successfully.</p>
                        <p>You can now close this window and return to the application.</p>
                    </div>
                </body>
                </html>
                """;
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
            String response = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Authorization Failed</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; background: #fff5f5; }
                        .error { color: #dc3545; font-size: 24px; margin-bottom: 20px; }
                        .message { color: #333; font-size: 16px; }
                    </style>
                </head>
                <body>
                    <div class="error">❌ Authorization Failed</div>
                    <div class="message">
                        <p>%s</p>
                        <p>Please try again or check the application logs for details.</p>
                    </div>
                </body>
                </html>
                """, errorMessage);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(400, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        private String extractAuthCode(String query) {
            try {
                if (query == null || query.isEmpty()) {
                    return null;
                }
                
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "code".equals(pair[0])) {
                        return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                logger.error("Error parsing authorization code from query '{}': {}", query, e.getMessage());
            }
            return null;
        }

        private String extractErrorInfo(String query) {
            try {
                if (query == null || query.isEmpty()) {
                    return "Unknown error";
                }
                
                String[] params = query.split("&");
                StringBuilder errorInfo = new StringBuilder();
                
                for (String param : params) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && (pair[0].equals("error") || pair[0].equals("error_description"))) {
                        if (errorInfo.length() > 0) {
                            errorInfo.append(", ");
                        }
                        errorInfo.append(pair[0]).append("=").append(URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                    }
                }
                
                return errorInfo.length() > 0 ? errorInfo.toString() : "Unknown error";
                
            } catch (Exception e) {
                logger.error("Error parsing error info from query '{}': {}", query, e.getMessage());
                return "Error parsing error information";
            }
        }
    }

    @Override
    public void close() {
        stopServer();
    }
}