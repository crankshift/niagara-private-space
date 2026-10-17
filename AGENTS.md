# AGENTS.md

## Agent skills

### Issue tracker

Issues and specs are local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Build

- Build with the JDK bundled in Android Studio: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- `./gradlew assembleDebug` is the default check for any code change. `./gradlew test` runs the JVM unit tests. `./gradlew assembleRelease` exercises R8 and signing.
- Release signing reads `NIAGARAPS_STORE_FILE`, `NIAGARAPS_STORE_PASSWORD`, `NIAGARAPS_KEY_ALIAS` and `NIAGARAPS_KEY_PASSWORD` from `~/.gradle/gradle.properties` or the environment. Refer to them by name only. Without the store file every Gradle run warns (the check runs when the build is configured) and release builds sign with the debug key. The keystore is permanent: installed copies accept updates only from the same key.

## Releasing

`crankshift/niagara-private-space` is the source of truth. The store repo (`Xposed-Modules-Repo/io.github.crankshift.niagaraprivatespace`) is only a store page, filled by `.github/workflows/release.yml` from `modules-repo/`.

1. Offer the user the device check (below). It is optional and needs their go-ahead, its last section runs the signed, minified release build, and the store page names the Niagara version it last passed with.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, and add a `## <versionName>` section to `CHANGELOG.md`; its last line names the Niagara version the release was built against or tested with.
3. Update the store page in `modules-repo/` (README.md, SUMMARY) when features or the tested Niagara version change; its README names that version, and so do the root `README.md`'s status line and Requirements paragraph. Never add a `HIDE` file there.
4. Commit and push, then push the tag `<versionCode>-<versionName>`: `git tag 2-0.2.0 && git push origin 2-0.2.0`. The workflow refuses a tag that doesn't match the APK, an APK not signed with the module's key (the certificate's SHA-256 is pinned as `CERT_SHA256` in `release.yml`), or a version with no changelog section. If it refuses, delete the tag (`git push origin :2-0.2.0 && git tag -d 2-0.2.0`), fix the problem, and tag again.

`gh workflow run release.yml` runs the same checks without publishing (the dry run). Its store-access check needs the store repo to exist, and the store creates it from the submission form. The workflow uses three repo secrets: `NIAGARAPS_KEYSTORE_BASE64`, `NIAGARAPS_STORE_PASSWORD` and `MODULES_REPO_TOKEN`. `MODULES_REPO_TOKEN` is a classic PAT with only `public_repo`, because fine-grained tokens can't reach an org repo you only collaborate on (Origram's experience; not yet tried for this store repo). It expires, so when "Check Modules Repo access" fails, the owner generates a new token and replaces the secret.

Pushing a tag publishes a release and setting a secret changes what CI can do, so an agent asks the user first.

The store repo grants only the Maintain role. For Origram's store repo that was enough to edit the description on the website (the ⚙ next to **About**) while `gh repo edit` and the GraphQL API refused it (`docs/research/vector-store-publishing.md`, checked 2026-10-17); this store repo has not been tried. The description must read "Niagara Private Space" and must not be blank: the store leaves out any repo without one (`docs/research/vector-store-publishing.md`).

## Device check

`docs/device-check.md` is the on-device check. It is optional, and an agent must always ask the user before running it: it locks and unlocks Private Space, restarts Niagara, reboots the phone and reads Niagara's database as root.

## Fingerprint check

`FingerprintTest` checks that the finder resolves every hook in a Niagara APK. It is optional and skipped unless `NIAGARA_APK` (Niagara's APK, kept outside the repo, never committed) and `DEXKIT_LIBRARY` (a DexKit library built for the Mac) are set. Pull the APK with `adb pull "$(adb shell pm path bitpit.launcher | head -1 | cut -d: -f2 | tr -d '\r')" ~/niagara-apk/base.apk`, (untested). `docs/fingerprint-check.md` has the rest: building the library and running the test, none of which has been run end to end yet.
