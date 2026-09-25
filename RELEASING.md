# Releasing OpenAlarm

Releases are built and signed on the maintainer's computer. The signing key never goes into this repo or into CI.

## The signing key

Every update has to be signed with the same key as the version people already have installed. If the key is lost, users can only update by uninstalling OpenAlarm, which deletes their alarms. So keep an offline backup of both the key file and its password.

The key is a PKCS12 keystore that Gradle finds through a properties file:

```properties
storeFile=/path/to/openalarm-release.jks
storePassword=...
keyAlias=openalarm
keyPassword=...
```

Point Gradle at that file in `~/.gradle/gradle.properties`, outside the repo:

```properties
openalarm.signing=/path/to/signing.properties
```

Without this setting, release builds come out unsigned.

To create a new key, which only happens for a brand-new app ID:

```bash
keytool -genkeypair -storetype PKCS12 -keystore openalarm-release.jks -alias openalarm \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Your Name, OU=OpenAlarm"
```

## Making a release

1. Raise `versionCode` by one and set `versionName` in [androidApp/build.gradle.kts](androidApp/build.gradle.kts).
2. Add the version to [CHANGELOG.md](CHANGELOG.md), and write a short summary (500 characters at most) in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`. F-Droid shows that file as the release notes.
3. Build and test:

   ```bash
   ./gradlew check :androidApp:assembleRelease
   ```

4. Check that the APK is signed with the release key. The certificate's SHA-256 must match the one in [README.md](README.md):

   ```bash
   $ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs androidApp/build/outputs/apk/release/androidApp-release.apk
   ```

5. Install the release APK on a phone or emulator. Set an alarm with a mission, let it ring with the screen off, snooze it, then turn it off. Release builds are shrunk by R8, so they can break in ways debug builds don't.
6. Commit, tag and publish, with `X.Y.Z` as the new version:

   ```bash
   git tag -a vX.Y.Z -m "OpenAlarm X.Y.Z"
   git push origin main vX.Y.Z
   cp androidApp/build/outputs/apk/release/androidApp-release.apk OpenAlarm-X.Y.Z.apk
   sha256sum OpenAlarm-X.Y.Z.apk > OpenAlarm-X.Y.Z.apk.sha256
   gh release create vX.Y.Z OpenAlarm-X.Y.Z.apk OpenAlarm-X.Y.Z.apk.sha256 --title "OpenAlarm X.Y.Z" --notes-file <release notes>
   ```

Obtainium users get the new version from the GitHub release automatically.
