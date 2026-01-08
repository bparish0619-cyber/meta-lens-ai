# MetaLens (Android) — minimal starter

This is a **minimal Android app** created to mirror the modern Gradle setup from `turbometa-rayban-ai/android` (Kotlin + Compose + version catalog), but with a tiny "Hello World" UI and **English-only** resources.

## Open in Android Studio

1. Open Android Studio
2. **File → Open…**
3. Select this folder: `meta-lens-ai/android`
4. Wait for **Gradle Sync** to finish (Android Studio will download a JDK/Gradle/dependencies if needed)

## Run on your phone

1. Enable **Developer options** + **USB debugging** on your Android phone
2. Connect the phone via USB (accept the RSA prompt on the phone)
3. In Android Studio, select the device in the toolbar
4. Press **Run ▶** (it will install and launch the app)

## Troubleshooting

## What to look at (for learning step-by-step)

- `settings.gradle.kts`: project name + repositories + includes `:app`
- `gradle/libs.versions.toml`: versions catalog (plugins + libraries)
- `app/build.gradle.kts`: Android + Compose configuration and dependencies
- `app/src/main/AndroidManifest.xml`: app + launcher activity
- `app/src/main/java/.../MainActivity.kt`: Compose "Hello World"

## Next small steps we can add

- A second screen + Navigation
- Basic settings screen
- Permissions (Bluetooth/Microphone) scaffolding
- A simple service layer + dependency injection

