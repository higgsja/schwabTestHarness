# Schwab API Test Harness

An interactive Spring Boot application for testing and validating the `schwab-api` client library against the Charles Schwab Developer API. Covers OAuth 2.0 authorization, token lifecycle management, and market data retrieval.

---

## Prerequisites

- Java 17+
- A registered Schwab Developer application with `appKey` and `appSecret`
- The `schwab-api` library JAR on your classpath (or installed in your local Maven repo)

---

## Quick Start

### Running the Pre-built JAR

```bash
java -jar schwab-test-harness.jar
```

### Building from Source

```bash
mvn clean package
java -jar target/schwab-test-harness.jar
```

---

## Configuration

Edit `application.yml` before running. The only required fields are your API credentials:

```yaml
schwab:
  api:
    appKey: "YOUR_APP_KEY"
    appSecret: "YOUR_APP_SECRET"
    tokenPropertiesFile: "schwab-api.json"       # where access/refresh tokens are persisted
    refreshTokenFile: "schwab-refresh-token.txt"  # optional secondary refresh token store
    urls:
      auth: "https://api.schwabapi.com/v1/oauth/authorize"
      token: "https://api.schwabapi.com/v1/oauth/token"
      traderBase: "https://api.schwabapi.com/trader/v1"
      marketData: "https://api.schwabapi.com/marketdata/v1"
    defaults:
      redirectUri: "https://127.0.0.1:8182"
      httpTimeoutMs: 30000
      scope: "readonly"
```

> **Warning:** `application.yml` contains live credentials. Never commit this file to version control. Add it to `.gitignore`.

---

## Interactive Menu

When the application starts, it presents a numbered menu:

| Option | Description |
|--------|-------------|
| 1 | Configuration Status — display masked credentials and all resolved URLs |
| 2 | Automatic OAuth Authorization — launches browser and spins up a local HTTPS callback server on `127.0.0.1:8182` |
| 3 | Manual OAuth Authorization — fallback; prints the authorization URL for you to visit and paste the redirect back |
| 4 | Check Token Status — shows access token expiry, refresh token expiry, and validity |
| 5 | Test Automated Refresh (Forced) — forces a token refresh regardless of current expiry |
| 6 | Test Market Data API — fetches live quotes for a set of symbols |
| 7 | Test Historical Data (Individual) — fetches price history for a single symbol |
| 8 | Test Bulk Historical Data — fetches 30-day price history for multiple comma-separated symbols |
| 9 | Exit |

---

## Standalone Utilities

These can be run directly without the full Spring context (useful for cron jobs or health checks).

### Token Health Check

```bash
java -cp schwab-test-harness.jar com.higgstx.schwabtest.debug.SimpleTokenCheckerMain --check-health
```

Prints access token validity (hours remaining) and refresh token validity (days remaining) without starting a full application context.

### Automated Token Refresher

```bash
java -jar schwab-test-harness.jar --spring.main.web-application-type=none
# or call the class directly if your launcher supports it:
java -cp schwab-test-harness.jar com.higgstx.schwabtest.debug.AutoTokenRefresherMain
```

Loads the token file, calls the Schwab token endpoint to get a new access token using the stored refresh token, and writes the result back to `schwab-api.json`. Suitable for a cron job that runs every 20–25 minutes during market hours.

**Example crontab entry:**
```
*/25 * * * * cd /opt/schwab && java -cp schwab-test-harness.jar com.higgstx.schwabtest.debug.AutoTokenRefresherMain >> logs/cron-refresh.log 2>&1
```

---

## OAuth Flow

### First-Time Authorization (Option 2 — Automatic)

1. The harness starts a local HTTPS server on `https://127.0.0.1:8182`.
2. It opens your default browser to the Schwab authorization URL.
3. After you approve, Schwab redirects to `https://127.0.0.1:8182?code=...`.
4. The harness captures the authorization code, exchanges it for tokens, and saves them to `schwab-api.json`.

> The redirect URI must be registered exactly as `https://127.0.0.1:8182` in your Schwab developer app settings.

### Manual Fallback (Option 3)

If the automatic browser/server flow fails, Option 3 prints the authorization URL. Visit it in your browser, approve, then copy the full redirect URL (including the `?code=` parameter) and paste it back at the prompt.

### Token Persistence

Tokens are stored as JSON in the file specified by `tokenPropertiesFile` (default: `schwab-api.json`). The `TokenManager` automatically refreshes the access token using the stored refresh token when it detects expiry.

- Access tokens typically expire after **30 minutes**.
- Refresh tokens expire after **7 days** — if the refresh token expires, you must re-run the OAuth flow.

---

## Logging

Logging is configured in `logback.xml`. By default, all output goes to files in the `logs/` directory; console output is suppressed to keep the interactive menu readable.

| Log File | Contents |
|----------|----------|
| `logs/schwab-oauth-client.log` | Main application log (rolling, 10 MB max, 30-day history) |
| `logs/schwab-debug.log` | DEBUG-level details from `com.higgstx.schwab*` and OkHttp |

### Log Viewer Utility

```bash
# Show log file sizes and last-modified times
java -cp schwab-test-harness.jar com.higgstx.schwabtest.util.LogViewer

# Tail the last 100 lines of the main log
java -cp schwab-test-harness.jar com.higgstx.schwabtest.util.LogViewer tail main 100

# Show full OAuth log
java -cp schwab-test-harness.jar com.higgstx.schwabtest.util.LogViewer show oauth

# Clear all log files
java -cp schwab-test-harness.jar com.higgstx.schwabtest.util.LogViewer clear
```

Available log type aliases: `main`, `app`, `oauth`, `http`, `requests`.

To enable console logging during a debugging session, uncomment the `<appender-ref ref="CONSOLE"/>` lines in `logback.xml`, or add this to `application.yml`:

```yaml
logging:
  level:
    com.higgstx.schwab: DEBUG
    okhttp3: INFO
    org.springframework: WARN
```

---

## Project Structure

```
src/main/java/com/higgstx/schwabtest/
├── SchwabTestHarnessApplication.java   # Spring Boot entry point
├── config/
│   ├── SchwabTestConfig.java           # @ConfigurationProperties binding for application.yml
│   └── SchwabServiceConfiguration.java # Spring @Bean wiring for TokenManager, MarketDataService
├── debug/
│   ├── TestHarnessRunner.java          # CommandLineRunner — interactive menu
│   ├── SimpleTokenCheckerMain.java     # Standalone token health check
│   └── AutoTokenRefresherMain.java     # Standalone token refresh for cron use
└── util/
    ├── LogViewer.java                  # CLI log file viewer
    └── LoggingUtil.java                # Log directory initialization helper

src/main/resources/
├── application.yml                     # Credentials and API configuration
└── logback.xml                         # Logging configuration

src/test/java/
└── MarketDataServiceTest.java          # Unit/integration tests for MarketDataService
```

---

## Running Tests

```bash
mvn test
```

`MarketDataServiceTest` covers initialization validation, null-safety guards, token error handling, graceful shutdown, resource cleanup via `AutoCloseable`, and integration of `SchwabApiProperties` with `TokenManager`.

---

## Security Notes

- Keep `application.yml` and `schwab-api.json` out of version control — both contain live secrets.
- The local HTTPS callback server uses a self-signed certificate; your browser will show a security warning — this is expected and safe for localhost use.
- Credentials in `SimpleTokenCheckerMain` are hardcoded as a fallback. Remove or externalize these before committing to any shared repository.
