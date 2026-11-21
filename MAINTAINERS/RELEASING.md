# Releasing UU Feed

Signed releases are sideload APKs. They are not Play Store uploads. The upload keystore is the app's identity: Android will only install an update over an existing UU Feed when the new APK is signed with the same certificate.

v1.0.0 was signed with the GitHub-hosted debug key. The first build signed with the upload keystore cannot replace that install. Anyone who installed v1.0.0 has to uninstall it once, then install the signed APK.

## Create the upload keystore

Run this once, on a machine you control. Pick passwords you can store offline. The example alias is `uufeed`.

```bash
keytool -genkeypair -v \
  -keystore uufeed-upload.jks \
  -alias uufeed \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=UU Labs, OU=UU Feed, O=UU Labs, C=AU"
```

`keytool` asks for the store password and then the key password. Current `keytool` writes a PKCS12 file, and PKCS12 keeps a single password: the key password you type is ignored and the store password is used for the key as well. Set `UUFEED_KEYSTORE_PASSWORD` and `UUFEED_KEY_PASSWORD` to that same value. A legacy JKS file (`-storetype JKS`) can use two different passwords; only do that if you already have one.

Back up `uufeed-upload.jks` and both passwords somewhere that is not this git repository: an encrypted disk, a password manager, and a second copy offline. If the keystore or the passwords are lost, phones that already installed a signed UU Feed cannot take an update. The only fix is a new application id, or asking every user to uninstall. There is no Play App Signing account that can reset this key.

Do not commit the `.jks`, a `.keystore`, or `keystore.properties`. Those paths are gitignored.

## Local release builds

Either export the four environment variables:

```bash
export UUFEED_KEYSTORE_PATH="$PWD/uufeed-upload.jks"
export UUFEED_KEYSTORE_PASSWORD='store-password'
export UUFEED_KEY_ALIAS=uufeed
export UUFEED_KEY_PASSWORD='key-password'
./gradlew assembleRelease
```

Or copy `keystore.properties.example` to `keystore.properties` and fill it in. Environment variables win when both are set.

| `keystore.properties` | Environment variable |
| --- | --- |
| `storeFile` | `UUFEED_KEYSTORE_PATH` |
| `storePassword` | `UUFEED_KEYSTORE_PASSWORD` |
| `keyAlias` | `UUFEED_KEY_ALIAS` |
| `keyPassword` | `UUFEED_KEY_PASSWORD` |

`storeFile` may be absolute, or relative to the repository root. `./gradlew assembleRelease` fails with a short message when the keystore settings are missing. Debug builds do not need them.

The APK is `app/build/outputs/apk/release/app-release.apk`. `./gradlew signingReport` prints its SHA-1. That SHA-1 is what an Android-restricted Places API key should allow, together with package `au.com.uulabs.uufeed`.

## GitHub Actions secrets

The tag workflow will not publish a release until all four secrets exist. It does not fall back to a debug APK. Add them under Settings → Secrets and variables → Actions → Repository secrets. The names have to match exactly:

| Secret | Value |
| --- | --- |
| `UUFEED_KEYSTORE_BASE64` | The keystore file, base64 on one line: `base64 -w0 uufeed-upload.jks` |
| `UUFEED_KEYSTORE_PASSWORD` | The store password |
| `UUFEED_KEY_ALIAS` | The alias, `uufeed` if you used the command above |
| `UUFEED_KEY_PASSWORD` | The key password |

The base64 value is the whole keystore. Treat it like the `.jks` file.

## Version and the APK name

Push and pull request builds stay at `versionName` `1.0.0` and `versionCode` `1`. They assemble a debug APK and upload it only as an Actions artifact named `UU-Feed-debug`.

A tag named `vMAJOR.MINOR.PATCH` (for example `v1.1.0`) sets:

- `versionName` = `MAJOR.MINOR.PATCH`
- `versionCode` = `MAJOR * 10000 + MINOR * 100 + PATCH`

Keep minor and patch below 100 so the codes stay in order (`v1.9.0` is 10900, `v1.10.0` is 11000). `v1.1.0` is versionCode `10100`, which is newer than the v1.0.0 debug APK (versionCode `1`).

The release asset is `UU-Feed-` plus the tag, such as `UU-Feed-v1.1.0.apk`. The filename does not contain `debug`.

## Cut a release

1. Confirm the upload keystore and the four secrets are in place, and that a backup of the keystore exists outside GitHub.
2. `main` should be green. Do not create the tag on a failing commit.
3. From the commit you want to ship, create an annotated tag and push only that tag:

```bash
git tag -a v1.1.0 -m "UU Feed 1.1.0"
git push origin v1.1.0
```

The tag needs the form `v1.2.3`. The workflow rejects anything else, and it fails before `assembleRelease` if a signing secret is missing.

4. Android CI on that tag runs the unit tests, builds the debug artifact for the Actions run, assembles the signed release, and attaches `UU-Feed-v1.1.0.apk` to the GitHub Release. It does not upload the debug APK to the Release.
5. Check the Release page: the asset name, the version on the phone's app info after install, and a first launch that opens Places API key setup.
