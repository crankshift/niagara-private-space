# Getting a module into the Vector store

Checked 2026-10-17 by comparing Fermata Auto Enabler (listed) with Origram (not listed).

## Where Vector's store gets its list

- Vector is JingMatrix/Vector (JingMatrix/LSPosed redirects there). The store's list comes only from `https://backup.modules.lsposed.org/modules.json`. Module details come from `module/<pkg>.json`, first on the backup host, then on modules.lsposed.org. `usable()` drops entries with `hide==true` or no releases, and removes duplicate names. Source: `manager/src/main/kotlin/org/matrix/vector/manager/data/repository/RepoRepository.kt` at commit ddeed8c.
- `modules.json` is built from the Xposed-Modules-Repo GitHub org. `isPotentialModuleRepository()` and `parseRepository()` require **a `.` in the repo name, a non-empty GitHub repo description, and at least one release**. Otherwise the repo is marked `isModule:false` and left out. Source: `src/lib/modules.ts` on the `primer` branch of Xposed-Modules-Repo/modules.
- backup.modules.lsposed.org is a GitHub Pages mirror (`scripts/mirror-public-json.mjs`, daily at 18:23 UTC). It copies a Cloudflare deployment that is at least 7 days old. So a change can take about **8 days** to reach Vector, even though modules.lsposed.org updates within minutes.

## Why Origram isn't listed

`Xposed-Modules-Repo/io.github.crankshift.origram` has a blank GitHub description (`description: null`). Fermata's org repo has `"Fermata Auto Enabler"`. Both releases passed the Tag workflow. Origram's run is https://github.com/Xposed-Modules-Repo/modules/actions/runs/35243199872.

The approval bot on submission#1865 warned about a blank description. In submission#1866, an admin said the Maintain role can edit the description; GitHub's role docs agree. The claim in `crankshift/origram` `AGENTS.md` that only org admins can change it was wrong. The description was set to "Origram" on the website on 2026-10-17, and `AGENTS.md` was corrected in crankshift/origram@6cc1767. Expect about 8 days before Origram appears in Vector.

## Checklist for a new module

1. Package name `io.github.crankshift.<name>`. The bot rejects `com.<you>.*` unless you own the domain (submission#1781).
2. Submit at modules.lsposed.org/submission, then accept the org invite (Maintain role).
3. Make sure the org repo's GitHub description is the display name, ideally set at submission time. Don't start it with `N.`, which gets stripped. With the Maintain role, set it on the website (the ⚙ next to **About** → Description → Save changes); that worked for Origram on 2026-10-17. The API refuses the same edit: REST `PATCH /repos` returns 404, and GraphQL `updateRepository` returns "crankshift does not have the correct permissions".
4. Publish a non-draft release tagged `<versionCode>-<versionName>` (matching `^\d+-.+$`) with the `.apk` attached.
5. The APK must be either:
   - legacy: `assets/xposed_init` plus `xposedmodule` metadata, or
   - libxposed: `META-INF/xposed/module.prop` with `minApiVersion` and `targetApiVersion`, plus `java_init.list`.
6. Optional files in the org repo: `README.md`, `SUMMARY` (max 512 chars), `SOURCE_URL`, `SCOPE` (JSON). Never add a `HIDE` file.
7. Expect up to about 8 days before it appears in Vector.

## Build setup to reuse: crankshift/origram

- Versions (`gradle/libs.versions.toml`): AGP 9.4.0, Kotlin 2.4.20, Gradle 9.7.1, JDK 21.
- `io.github.libxposed:api` 102.0.0 (`compileOnly`) and `service` 102.0.0.
- Module files: `app/src/main/resources/META-INF/xposed/{java_init.list,module.prop,scope.list}`, with `minApiVersion` 101, `targetApiVersion` 102, `staticScope=true`.
- R8 with a keep rule; `packaging.merges` covers `META-INF/xposed/*`.
- CI: `.github/workflows/build.yml` runs on push and PR. `release.yml` triggers on a tag. It checks the tag against the APK, pins the signing certificate's SHA-256 and requires a CHANGELOG section. It then releases to both repos and syncs `modules-repo/` to the org repo. Secrets: keystore (base64), store password, `MODULES_REPO_TOKEN` (classic PAT, `public_repo`).
- The tag-triggered path of `release.yml` has never run; only a manual dry run has.

Fermata has no Gradle or CI: one Java class, `build.sh`, hand-written `de.robv` stubs, and the legacy manifest metadata. Only its publishing approach is worth copying.
