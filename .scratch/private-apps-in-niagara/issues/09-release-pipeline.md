# 09: Release pipeline

**What to build:** Pushing a `<versionCode>-<versionName>` tag builds, verifies and publishes a signed release to both `crankshift/niagara-private-space` and the store repo. It also syncs the store page files. Releases are signed with the module's own new key.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **Signing key:** a new key generated with `keytool`, stored outside the repo with a random password. The user backs up the key and password in Bitwarden; the agent waits for confirmation.
- [ ] **Secrets:** `NIAGARAPS_KEYSTORE_BASE64`, `NIAGARAPS_STORE_PASSWORD` and `MODULES_REPO_TOKEN` (classic PAT, `public_repo`), set on the GitHub repo **only after asking the user**.
- [ ] **Release workflow,** following Origram's. It refuses:
  - a tag that doesn't match the APK version;
  - an APK whose signing certificate SHA-256 doesn't match the pinned value;
  - a version with no CHANGELOG section.

  It checks access to the store repo before publishing, publishes to both repos, and syncs the store page directory to the store repo.
- [ ] **Store page directory:** exists in the repo. It holds a README, a SUMMARY (512 characters at most), SOURCE_URL and SCOPE, and never a HIDE file. The README names the last tested Niagara version.
- [ ] **CHANGELOG:** exists, with a section for the first version.
- [ ] **Dry run:** a manually triggered run performs every check without publishing, and passes.
- [ ] **AGENTS.md:** documents the release commands and the secrets, as Origram's does.
