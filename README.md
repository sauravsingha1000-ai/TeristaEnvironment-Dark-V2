# Terista Environment

A personal-use virtual app environment for Android 15, forked and adapted from [NewBlackbox](https://github.com/ALEX5402/NewBlackbox).

## Features

- 🔲 **Virtual App Sandbox** — Run multiple independent copies of any app
- 👥 **Multi-User** — Separate user spaces with custom labels
- 📍 **Fake Location** — Set custom GPS coordinates per app
- 🕹️ **Location Joystick** — Floating joystick overlay to move fake GPS in real-time
- 🔧 **GMS Support** — Enable/disable Google Play Services per user
- 🔒 **Root Hide** — Hide root detection for sandboxed apps
- 📌 **Desktop Shortcuts** — Create shortcuts to launch virtual apps directly
- 🔄 **Daemon Mode** — Keep virtual apps alive in background
- 🌐 **VPN Network** — Route sandboxed app traffic through VPN
- 🛡️ **Disable FLAG_SECURE** — Allow screenshots inside sandboxed apps

## Package Info

- **App Name**: Terista Environment
- **Package**: `com.terista.environment`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 35 (Android 15)
- **Architecture**: arm64-v8a, armeabi-v7a, Universal

## Building via GitHub Actions

This project uses GitHub Actions for building. No PC required.

### Steps:
1. Push this project to your GitHub repository
2. Go to **Actions** tab → **Build Terista Environment APK**
3. Click **Run workflow** → **Run workflow**
4. Wait ~5–10 minutes for the build to complete
5. Download the APK from **Artifacts** section

### Automatic builds:
Every push to `main` branch automatically triggers a build.

## Credits

See [CREDITS.md](CREDITS.md) for full attribution to original open-source projects.

## License

MIT License — See [LICENSE](LICENSE)

---
*Built from the open-source NewBlackbox project. All original licenses respected.*
