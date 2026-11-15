# Touchpad Pro

Android Accessibility Service based floating touchpad prototype.

## Build APK without Termux
1. Create a GitHub repository and upload all files in this folder.
2. Open the repository's **Actions** tab.
3. Run **Build Touchpad Pro APK** (or push to `main`).
4. Open the completed workflow run and download the `Touchpad-Pro-APK` artifact.
5. Extract the artifact to get `app-debug.apk`.

## Enable
Install the APK, open Touchpad Pro, tap **OPEN ACCESSIBILITY SETTINGS**, then enable Touchpad Pro.
Android displays a system warning for accessibility services; review it and enable only if you trust this app.
The full-screen overlay has a red STOP button in the top-right corner.

## Gestures
- Finger movement: move the floating cursor
- Single tap: click (slight delay to detect double-tap)
- Double tap: double click
- Move finger while held: swipe/drag gesture on release
- Two fingers: scroll gesture

This is a prototype. Android gesture injection and overlay behavior can vary by device/OS.
