# 02: Fingerprint check against a local Niagara APK

**What to build:** A test on the Mac that runs the finder against a Niagara APK the maintainer has pulled locally. It shows that every fingerprint resolves to exactly one target, so a Niagara update that breaks recognition is caught before anything goes on the phone. Niagara's APK must never be committed to this public repo, so the test is skipped when no APK is present.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **DexKit on the Mac:** establish whether DexKit runs on the desktop JVM, and record the answer in the research notes.
- [ ] **If it does:** a test reads the APK location from an environment variable or Gradle property, runs every fingerprint and asserts exactly one match each. It's skipped, not failed, when no APK is configured.
- [ ] **If it doesn't:** record that in the research notes and the spec's testing decisions. The check falls back to the activation log line on the phone.
- [ ] **`.gitignore`:** blocks APK files from ever being committed.
- [ ] **Coverage:** every fingerprint added by later tickets is covered by this test without further setup.
- [ ] **AGENTS.md:** says how to pull Niagara's APK from the phone and run the test.
