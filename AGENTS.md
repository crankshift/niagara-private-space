# AGENTS.md

## Agent skills

### Issue tracker

Issues and specs are local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Device check

`docs/device-check.md` is the on-device check. It is optional, and an agent must always ask the user before running it: it locks and unlocks Private Space, restarts Niagara, reboots the phone and reads Niagara's database as root.

## Fingerprint check

`FingerprintTest` checks that the finder resolves every hook in a Niagara APK. It is optional and skipped unless `NIAGARA_APK` (Niagara's APK, kept outside the repo, never committed) and `DEXKIT_LIBRARY` (a DexKit library built for the Mac) are set. Pull the APK with `adb pull "$(adb shell pm path bitpit.launcher | head -1 | cut -d: -f2 | tr -d '\r')" ~/niagara-apk/base.apk`, (untested). `docs/fingerprint-check.md` has the rest: building the library and running the test, none of which has been run end to end yet.
