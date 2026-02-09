# URL Sentinel Android

An Android app that intercepts URL clicks system-wide, validates them for security threats, and lets you choose which browser to open them with.

## Features

- **System-wide URL Interception**: Automatically intercepts links from any app
- **Security Validation**: Checks URLs against a backend validation server for threats
- **HTTPS/HTTP Indicator**: Visual feedback on connection security
- **Browser Picker**: Choose your preferred browser for each link
- **Auto-open Safe Links**: Option to automatically open validated safe links
- **Modern UI**: Built with Jetpack Compose and Material 3
- **Dark Mode**: Supports system-wide dark theme

## Requirements

- Android 8.0 (API 26) or higher
- [url-sentinel-server](https://github.com/seheon/url-sentinel-server) running for URL validation (optional but recommended)

## Installation

### From Source

1. Clone the repository:
```bash
git clone https://github.com/seheon/url-sentinel-android.git
cd url-sentinel-android
```

2. Build and install:
```bash
./gradlew installDebug
```

### Configuration

The app connects to `http://10.0.2.2:8080` by default (Android emulator localhost). To change the server URL, edit `app/src/main/java/kr/seheon/urlpolice/api/ApiClient.kt`.

## Usage

1. Install and launch the app
2. Click any link in any app (Messages, Email, etc.)
3. URL Sentinel will intercept it and show:
   - The URL
   - Security status (HTTPS/HTTP)
   - Validation result from server (if available)
4. Choose a browser to open the link
5. Optionally enable "Auto-open Safe Links" in settings to skip the picker for validated safe URLs

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Single Activity with Compose
- **State Management**: Kotlin `mutableStateOf()`
- **Networking**: Retrofit + Moshi
- **Theming**: Dynamic theming (Android 12+)

### Data Flow
```
External app link → ACTION_VIEW intent → MainActivity
→ URLPoliceApp composable (displays URL + security status)
→ Server validation (async)
→ User selects browser → BrowserManager launches chosen browser
```

### Key Components
- **MainActivity.kt** - Intent interception and browser launching
- **URLPoliceApp.kt** - Main Compose UI with URL card and browser picker
- **BrowserManager.kt** - Queries installed browsers via PackageManager
- **ApiClient.kt** - Retrofit client for server communication
- **Browser.kt** - Known browser definitions

## Server Integration

URL Sentinel connects to [url-sentinel-server](https://github.com/seheon/url-sentinel-server) for URL validation:

**API Endpoint**: `POST /api/v1/url/check`

**Request**:
```json
{
  "url": "http://example.com"
}
```

**Response**:
```json
{
  "verdict": "ALLOW",
  "reasons": ["HTTPS", "Known safe domain"],
  "riskScore": 10
}
```

## Build Commands

```bash
# Build debug APK
./gradlew build

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Build and install to connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Development

See [CLAUDE.md](./CLAUDE.md) for detailed architecture and development guidelines.

## License

[Add your license here]

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
