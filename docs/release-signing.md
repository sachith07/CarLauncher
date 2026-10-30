# Release signing

## Default (no keystore)

Without `keystore.properties`, `assemble*Release` signs with the **Android debug keystore**. That is acceptable for USB pilot installs on a trusted head unit. Debug and release package IDs are both `com.vezel.launcher`.

```bat
gradlew.bat :app:assembleDrawerRelease
gradlew.bat :app:assembleHomePilotRelease
```

Outputs under `app/build/outputs/apk/`.

## Production keystore

1. Create a JKS/keystore offline; keep backups offline.
2. Copy `keystore.properties.example` → `keystore.properties` (gitignored).
3. Fill `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
4. Re-run `assembleDrawerRelease` / `assembleHomePilotRelease`.

Never commit `keystore.properties`, `*.jks`, or `*.keystore` (except a local debug keystore if needed).
