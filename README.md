# Monterto Image Viewer (Android)

A small, standalone Android image viewer. It is separate from the Monterto desktop app and does not include memo, prompt, GitHub, or file-management features.

## Features
- Opens image files shared from file managers and other apps (`ACTION_VIEW` / `ACTION_SEND`).
- Requests images through Android's image picker (`ACTION_OPEN_DOCUMENT`).
- Full-screen black canvas with filename overlay.
- Fit-to-screen display, pinch-to-zoom (up to 8×), and drag-to-pan while zoomed.
- Reads content through Android `content://` URIs; no broad storage permission is requested.
- Minimum Android 7.0 (API 24).

## Build in the cloud from a phone
See [README-PHONE-KO.md](README-PHONE-KO.md). The included GitHub Actions workflow builds a debug APK in GitHub's cloud runner and uploads it as an artifact.

## Local build
With Android SDK and Gradle installed, run:

```bash
gradle assembleDebug
```

The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.
