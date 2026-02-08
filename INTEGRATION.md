# URL Sentinel Server Integration

This document describes the integration between the URL Sentinel Android app and the url-sentinel-server.

## Overview

The Android app now connects to the Spring Boot server for URL validation instead of using local mock logic. The server provides sophisticated phishing detection using multiple rule-based checks.

## Architecture

```
User clicks URL
    ↓
Android intercepts via ACTION_VIEW
    ↓
MainActivity receives URL
    ↓
URLValidationService calls server API
    ↓
Server analyzes URL with rule engine
    ↓
Response mapped to URLValidationResult
    ↓
UI displays threat info + allows browser selection
```

## Server API

**Endpoint:** `POST /api/v1/url/check`

**Request:**
```json
{
  "url": "http://192.168.1.1/login"
}
```

**Response:**
```json
{
  "verdict": "REJECT",
  "reasons": [
    {
      "code": "IP_ADDRESS_DOMAIN",
      "severity": "CRITICAL",
      "message": "URL uses an IP address instead of a domain name"
    }
  ],
  "riskScore": 60
}
```

## Implementation Details

### New Files Created

1. **`api/UrlCheckRequest.kt`** - Request model
2. **`api/UrlCheckResponse.kt`** - Response models (includes ReasonDetail)
3. **`api/UrlPoliceApiService.kt`** - Retrofit service interface
4. **`api/ApiClient.kt`** - Retrofit client configuration

### Modified Files

1. **`URLValidationService.kt`** - Now calls real API instead of mock logic
2. **`AndroidManifest.xml`** - Added `android:usesCleartextTraffic="true"` for dev
3. **`CLAUDE.md`** - Updated with server integration details

### Response Mapping

The server response is mapped to the existing `URLValidationResult` model:

- **verdict** → **isSafe**: `ALLOW` = true, `REJECT` = false
- **riskScore** → **confidence**: `(100 - riskScore) / 100`
- **reasons** → **threatType**: Mapped from reason codes
  - `IP_ADDRESS_DOMAIN` → `PHISHING`
  - `SUSPICIOUS_KEYWORD` → `PHISHING`
  - `URL_SHORTENER` → `SUSPICIOUS`
  - Others → `SUSPICIOUS`
- **reasons** → **message**: Top 2 most severe reasons + risk score

## Configuration

### Server URL

The server URL is configured in `ApiClient.kt`:

```kotlin
private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
```

- **Android Emulator:** Use `http://10.0.2.2:8080` (points to host's localhost)
- **Physical Device:** Use your computer's IP address (e.g., `http://192.168.1.100:8080`)
- **Production:** Update to your deployed server URL

To change the URL, edit `DEFAULT_BASE_URL` in `ApiClient.kt`.

### BuildConfig Alternative

For production, consider moving the URL to `build.gradle.kts`:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.urlsentinel.com\"")
        }
    }
}
```

## Testing

### 1. Start the Server

```bash
cd /Users/seheon/GitHub/url-sentinel-server
./gradlew bootRun
```

The server should start on `http://localhost:8080`.

### 2. Build and Install the App

```bash
cd /Users/seheon/GitHub/url-sentinel-android
./gradlew installDebug
```

### 3. Test URL Interception

Try opening these test URLs in any app (e.g., Notes, Messages):

**Safe URLs:**
- `https://google.com`
- `https://github.com`

**Phishing/Suspicious URLs:**
- `http://192.168.1.1/login/verify` (IP address + suspicious keywords)
- `http://example.xyz/bank-verify` (suspicious TLD + keywords)
- `http://bit.ly/abc123` (URL shortener)
- `http://sub1.sub2.sub3.sub4.example.com` (excessive subdomains)

The app should intercept the URL, show validation results from the server, and let you choose a browser.

### 4. Test Error Handling

Stop the server and try opening a URL. The app should show:
> "Unable to validate URL: Failed to connect to /10.0.2.2:8080. Proceed with caution."

## Network Configuration

### Android 9+ (API 28+) Cleartext Traffic

For development, the app allows cleartext (HTTP) traffic via:

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

**For production**, remove this and use HTTPS with a proper network security config.

### Internet Permission

Already configured in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Dependencies

The integration uses existing dependencies:

```toml
retrofit = "2.9.0"
moshi = "1.15.0"
```

- **Retrofit** - HTTP client for API calls
- **Moshi** - JSON serialization/deserialization
- **Moshi Kotlin Reflect** - Automatic Kotlin data class support

## Future Improvements

1. **Configuration UI** - Allow users to set custom server URLs
2. **Caching** - Cache validation results for recently checked URLs
3. **Offline Mode** - Fallback to basic local checks when offline
4. **Certificate Pinning** - For production HTTPS connections
5. **Network Security Config** - Proper cleartext traffic policies for production
6. **Loading States** - Better error messages and retry mechanisms
7. **Analytics** - Track validation success rates and response times

## Troubleshooting

### "Unable to validate URL: Failed to connect"

- Ensure the server is running on `http://localhost:8080`
- For emulator, use `http://10.0.2.2:8080`
- For physical device, use your computer's network IP
- Check firewall settings allow incoming connections on port 8080

### Build Errors

```bash
./gradlew clean build
```

### Server Not Responding

```bash
cd /Users/seheon/GitHub/url-sentinel-server
./gradlew bootRun --info
```

Check server logs for errors.

## Security Notes

⚠️ **Development Only**

The current configuration is for **development only**:
- Uses HTTP (cleartext traffic)
- No authentication/authorization
- Server URL is hardcoded

**Before production:**
- Use HTTPS with valid certificates
- Implement API authentication (API keys, OAuth, etc.)
- Use proper network security configuration
- Remove `android:usesCleartextTraffic="true"`
- Move server URL to secure configuration or BuildConfig
