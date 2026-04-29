# Moves

A privacy-first Wear OS reminder app. Buzzes on an alternating schedule
(Move at :00, Water at :30) within user-chosen active hours. Auto-skips a
move buzz if you've been walking recently.

- No accounts, no cloud, no analytics
- Vibration only, no sound
- Settings stored locally with Jetpack DataStore; no movement history saved
- Standalone Wear OS app (no companion phone app required)

---

## Building from the terminal

You have **two options**: a native build (uses your local JDK + Android SDK)
or a Docker build (fully reproducible, no host SDK needed). Both produce the
same APK at `app/build/outputs/apk/debug/app-debug.apk`.

### Option A — Native build (fastest)

Requirements:
- JDK 17 or 21 (`java -version` to check)
- Android SDK with `platforms;android-35` and `build-tools;35.0.0` installed

The Gradle wrapper is included, so you don't need a system Gradle.

```sh
# 1. Tell Gradle where the Android SDK lives. Either:
#    a) edit local.properties to set sdk.dir=/path/to/Android/sdk, OR
#    b) export ANDROID_HOME=/path/to/Android/sdk
export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS default

# 2. Build the debug APK
./gradlew :app:assembleDebug

# 3. (optional) Install onto a watch reachable via adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If you don't already have the Android SDK and don't want to install Android
Studio, the easiest way to get just the command-line tools is:

```sh
# macOS (Homebrew)
brew install --cask android-commandlinetools
sdkmanager --install "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### Option B — Docker build (no host SDK required)

Requirements:
- Docker (or Podman aliased to `docker`)
- `adb` on the host **only** if you want to install onto a watch
  (the container builds the APK; you flash it from your machine)

```sh
make docker-image   # one-time: build the Android SDK container (~3 GB)
make build          # produces app/build/outputs/apk/debug/app-debug.apk
make install        # adb install onto the connected watch
```

---

## Running on a watch

You need a Wear OS device or emulator reachable via `adb`.

- **Physical watch**: enable developer options on the watch, then ADB debugging,
  then connect over Wi-Fi (`adb connect <watch-ip>:5555`) or USB.
- **Emulator**: in Android Studio's Device Manager, create a Wear OS API 30+ AVD
  and start it. `adb devices` should list it.

After `make install` (or the manual `adb install`), the "Moves" app appears in
the watch's app drawer.

### Manually triggering a reminder (for testing)

```sh
make trigger-move    # or:
adb shell am broadcast -a com.example.moves.REMINDER \
    -n com.example.moves/.receiver.ReminderReceiver
```

The next reminder type is determined by the alternation cycle (Move ↔ Water).
Run the broadcast twice in quick succession to cycle through both.

### Useful `make` targets

```
make help            list everything
make build           build the APK (uses Docker)
make clean           gradle clean (uses Docker)
make install         install the APK on the connected watch
make uninstall       remove the app from the watch
make logs            adb logcat filtered to this app
make trigger-move    force-fire a reminder for testing
```

---

## Why Docker?

Pinned JDK 17 + Android SDK 35 + build-tools 35.0.0 means the APK builds
identically on any machine — no "works on my Android Studio" surprises.
Docker is build-only; you still run the app on a real watch or AVD on the host.

## Project layout

```
app/src/main/java/com/example/moves/
├── data/         DataStore-backed settings + schedule state
├── domain/       Pure scheduler, sensor wrapper, vibration
├── receiver/     Alarm + boot BroadcastReceivers
└── presentation/ Compose screens (Home, Settings, Alert) + Activities
```
