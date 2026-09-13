# Android Release Signing via macOS Keychain

A guide for signing Android apps using a shared keystore stored in macOS Keychain, without ever committing passwords to git.

---

## Keystore Details

| Field | Value |
|-------|-------|
| **Keystore path** | `~/.android/release.jks` |
| **Key alias** | `release-key` |
| **Owner** | Akash, Ved Technologies, IN |
| **Valid until** | Aug 21, 2053 |
| **Keychain service** | `android-release-keystore` |
| **Keychain account** | `release-key` |

---

## One-Time Setup (New Mac / After Reinstall)

### 1. Copy the keystore to the right place

```bash
cp /path/to/release.jks ~/.android/release.jks
```

> Keep a backup of `release.jks` in a secure location (e.g. iCloud, encrypted drive). Losing it means you can never update your Play Store apps.

### 2. Store the password in macOS Keychain

```bash
security add-generic-password \
  -a "release-key" \
  -s "android-release-keystore" \
  -w "YOUR_PASSWORD_HERE"
```

### 3. Verify it works

```bash
# Check password is stored
security find-generic-password -a "release-key" -s "android-release-keystore" -w

# Check keystore is valid
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool \
  -list -v \
  -keystore ~/.android/release.jks \
  -storepass $(security find-generic-password -a "release-key" -s "android-release-keystore" -w)
```

---

## Wiring Up a New Android Project

### Step 1 — Add the helper function to `app/build.gradle.kts`

Place this above the `android { }` block:

```kotlin
fun getKeychainPassword(account: String, service: String): String {
    return Runtime.getRuntime()
        .exec(arrayOf("security", "find-generic-password", "-a", account, "-s", service, "-w"))
        .inputStream.bufferedReader().readLine() ?: ""
}
```

### Step 2 — Add the `signingConfigs` block inside `android { }`

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("${System.getProperty("user.home")}/.android/release.jks")
        storePassword = getKeychainPassword("release-key", "android-release-keystore")
        keyAlias = "release-key"
        keyPassword = getKeychainPassword("release-key", "android-release-keystore")
    }
}
```

### Step 3 — Reference it in the `release` build type

```kotlin
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## Building a Signed APK / AAB

```bash
# Signed APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Signed AAB (required for Play Store)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## Uploading to Google Play Store

1. Build the signed AAB: `./gradlew bundleRelease`
2. Go to [Google Play Console](https://play.google.com/console)
3. Select your app → **Production** (or Internal/Alpha/Beta)
4. Click **Create new release**
5. Upload `app-release.aab`
6. Fill in release notes and submit for review

> Play Store requires AAB format. APKs are for direct installs only.

---

## Notes

- The password never touches git — it's always fetched from Keychain at build time
- Builds will fail on machines without the Keychain entry (CI, other developers) — handle separately via environment variables or CI secrets
- All apps share the same keystore and key alias — this is fine for a solo developer
