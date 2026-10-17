# 02: Fingerprint check against a local Niagara APK

**What to build:** A test on the Mac that runs the finder against a Niagara APK the maintainer has pulled locally. It shows that every fingerprint resolves to exactly one target, so a Niagara update that breaks recognition is caught before anything goes on the phone. Niagara's APK must never be committed to this public repo, so the test is skipped when no APK is present.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** done-pending-device-check

- [x] **DexKit on the Mac:** establish whether DexKit runs on the desktop JVM, and record the answer in the research notes. *(Answer: not as published; a host library would have to be built from source, which was not done. Recorded.)*
- [ ] **If it does:** *(not applicable as shipped, see Notes)* a test reads the APK location from an environment variable or Gradle property, runs every fingerprint and asserts exactly one match each. It's skipped, not failed, when no APK is configured.
- [x] **If it doesn't:** record that in the research notes and the spec's testing decisions. The check falls back to the activation log line on the phone. *(Taken as the branch for now. The phone check is why the status is `done-pending-device-check`.)*
- [x] **`.gitignore`:** blocks APK files from ever being committed.
- [x] **Coverage:** every fingerprint added by later tickets is covered by this test without further setup. *(The test walks `Hook.entries`. A hook that resolves to several targets, which ticket 03 introduces, declares that on the `Hook` itself.)*
- [x] **AGENTS.md:** says how to pull Niagara's APK from the phone and run the test.

## Notes

- **DexKit has no desktop library as published.** The Maven Central artifact is an Android AAR with Android ELF libraries only; a macOS `libdexkit.dylib` has to be built from DexKit's source (its CMake project has a desktop target). The agent was not permitted to build code cloned from GitHub, so no host library exists and DexKit has never been run on the Mac. That is an inspection of the artifact, not a failed run. Recorded in `docs/research/niagara-internals.md` ("DexKit on the desktop JVM") and in the spec's Testing Decisions (Seam 2). The check falls back to the activation log line on the phone, as the spec allows.
- **The test is written anyway, so the Mac check works the day a host library exists.** `FingerprintTest` runs the finder over `NIAGARA_APK` with `System.load($DEXKIT_LIBRARY)` and asserts that every `Hook` was found; the finder only reports a single-target hook when its fingerprint matched exactly once, so "found" means "exactly one". It iterates `Hook.entries`, so a hook added by a later ticket is covered without setup. Skipped, not failed, while the variables are unset (run with none set, and with only `NIAGARA_APK` set); a variable that is set to a wrong path fails loudly. **What is not verified:** the run path itself, because nothing here could load DexKit.
- **The "If it does" criterion is left unchecked on purpose.** Its premise is that DexKit runs; the test file satisfies its other words (environment variable, one match each, skipped when absent), but ticking it would claim a run that never happened.
- **The recipe for the host library is untested.** It lives in `docs/fingerprint-check.md` (AGENTS.md keeps a short pointer and the `adb pull` line), follows DexKit's `CMakeLists.txt`, and says it has not been run. The APK and the library are declared as test task inputs in `app/build.gradle.kts`, by content, so Gradle reruns the test when either changes.
- **Glossary.** Added the module's own terms (decision module, finder, fingerprint, hook, resolution report) to `GLOSSARY.md`, because the test and every later ticket use them.
- `.gitignore` now blocks `*.apk`, `*.apks`, `*.xapk`, `*.aab` and `*.dex`.
