# 07 — GitHub releases

The repository ships two workflows:

| Workflow | Trigger | Output |
|---|---|---|
| `.github/workflows/ci.yml` | push to `main`, PRs, manual | core tests, debug APK artifact |
| `.github/workflows/release.yml` | tag `v*`, manual dispatch | APK/AAB, deb, rpm, dmg, msi, iOS simulator app, GitHub Release |

## Cut a release

```bash
git tag -a v1.0.0 -m "Starhold 1.0.0"
git push origin v1.0.0
```

The tag triggers all platform jobs and then `softprops/action-gh-release`
publishes the assets on the tag. Version tags must start at **v1.0.0 or
higher**: jpackage's Dmg/Msi version fields require `MAJOR > 0`.

Manual runs (`Actions → Release → Run workflow`) build the same artifacts and
upload them to the workflow run; they do not create a Release because there is
no tag.

## Android signing

`assembleRelease` is signed only when these repository secrets exist; without
them the job still produces an unsigned APK/AAB for manual testing:

| Secret | Meaning |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

Create them with:

```bash
base64 -w0 release.keystore | gh secret set ANDROID_KEYSTORE_BASE64
gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS
gh secret set ANDROID_KEY_PASSWORD
```

Local signed build:

```bash
export STARHOLD_KEYSTORE_PATH=$PWD/release.keystore
export STARHOLD_KEYSTORE_PASSWORD=…
export STARHOLD_KEY_ALIAS=…
export STARHOLD_KEY_PASSWORD=…
./gradlew :composeApp:assembleRelease
```

## Platform notes

- **Android** — one APK serves phones and Android TV (leanback launcher + banner).
- **Linux** — `.deb` needs `dpkg`, `.rpm` needs `rpmbuild`; the job installs
  `rpm` explicitly because some Ubuntu images omit it.
- **macOS** — unsigned `.dmg`. Notarisation needs an Apple Developer ID and is
  not automated.
- **Windows** — `.msi` through jpackage/WiX; unsigned, so SmartScreen will warn.
- **iOS** — CI builds an **unsigned simulator `.app`** with
  `CODE_SIGNING_ALLOWED=NO`. Installing on a device or TestFlight requires an
  Apple team: open `iosApp/Starhold.xcodeproj`, set `DEVELOPMENT_TEAM`, then
  Archive. The job is `continue-on-error` so a simulator-only failure does not
  block the other platform releases.

## Assets on a release

`Starhold-<version>.apk`, `Starhold-<version>.aab`,
`starhold_<version>_amd64.deb`, `starhold-<version>-1.x86_64.rpm`,
`Starhold-<version>.dmg`, `Starhold-<version>.msi`,
`Starhold-<version>-ios-simulator.zip`.

## Versioning

`-PstarholdVersion=<tag>` (leading `v` stripped) feeds Android `versionName`,
the desktop `packageVersion`, and release asset names.
`-PstarholdVersionCode=<run number>` feeds Android `versionCode`, so every
release is upgradeable.
