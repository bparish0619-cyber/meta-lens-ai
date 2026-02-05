# MetaLens AI (Android)

The source code in this repository contains the core components of Meta Lens AI. If you’re interested in contributing to the project, please email me.

## Run on your computer

### Prerequisites
- Android Studio (latest stable)
- Android SDK + Platform Tools (installed via Android Studio)
- JDK 17

### Run in Android Studio (recommended)
1. Open Android Studio.
2. Click **Open** and select the `android/` folder in this repo.
3. Let Gradle sync finish.
4. Start an emulator: **Tools → Device Manager → Create device**.
5. Click **Run** (green play button) to build and launch the app.

### Run from the command line
1. From the repo root:
   ```bash
   cd android
   ```
2. Build and install to a running emulator or connected device:
   ```bash
   ./gradlew :app:installDebug
   ```
3. If you need to start an emulator first, you can use Android Studio’s Device Manager.

### GitHub token (for GitHub Packages)
This project pulls `meta-wearables-dat-android` from GitHub Packages. Gradle reads the token from
`GITHUB_TOKEN` or `android/local.properties` (`github_token`).

Example `android/local.properties` entry:
```properties
github_token=YOUR_GITHUB_TOKEN
```
More details: https://github.com/facebook/meta-wearables-dat-android

### Troubleshooting
- If Gradle cannot find Java, set `JAVA_HOME` to your JDK 17 path.
- If the Android SDK is not found, set `ANDROID_SDK_ROOT` or configure it in Android Studio.

