# 01: Add a private app to favorites from its long-press menu

**What to build:** This is the first thin slice through the whole module. With the module enabled in Vector, long-pressing a **private app** in Niagara offers the same options as a **work app**, including "Favorite" and "Edit pop-up". Choosing "Favorite" adds it to **favorites**, where it opens normally and shows Android's Private Space badge.

This ticket also sets up the module:
- the project, from Origram's template;
- the **decision module**, the seam the unit tests exercise;
- the **finder**, which locates Niagara's code by fingerprint with DexKit;
- the **resolution report** and the all-or-nothing **activation rule**: if any essential hook is missing, the module changes nothing and writes a line to Vector's log.

See the spec: `.scratch/private-apps-in-niagara/spec.md`.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] **Project:** a Gradle project copied from Origram's setup (Kotlin, AGP 9.4, Gradle 9.7, JDK 21, version catalog), with package `io.github.crankshift.niagaraprivatespace` and display name "Niagara Private Space". It has minimum Android 15, compile and target SDK 37, and no activity or launcher icon.
- [ ] **Module metadata:** libxposed API 102 (minimum 101) with a static scope of only `bitpit.launcher`. R8 keeps the entry point.
- [ ] **Decision module:** pure Kotlin with no Android, Niagara or Xposed types. It answers whether an item gets the full long-press options, so that a private app is treated like a work app and personal and work apps are unchanged. JUnit Jupiter tests with backtick behaviour names, as in Origram, cover it.
- [ ] **Finder:** resolves the long-press options builder and the per-item "is Private Space" flag with DexKit from stable anchors. It's checked against Niagara 1.16.32 (1638).
- [ ] **Hook scope:** the hook neutralises the "is Private Space" check only inside the options builder, never globally. The Private Space section in the app list and hiding in search still behave as stock.
- [ ] **Activation:** the decision module turns a resolution report into activate or don't. When a required hook is missing, no hook is installed and Vector's log gets one line naming what's missing. Unit tests cover both outcomes.
- [ ] **Log on success:** on activation, Vector's log gets one line listing what was found.
- [ ] **CI:** a build workflow runs the build and unit tests on push and PR.
- [ ] **Repo files:** a GPL-3.0 `LICENSE` and a short README.
- [ ] **Device check:** `docs/device-check.md` exists, with steps for this slice (long-press a private app, add it to favorites, open it, see the badge). `AGENTS.md` gains a line saying the device check is optional and an agent must always ask the user before running it.
- [ ] **On the phone:** a private app is added to favorites through the long-press menu and opens normally, with the user's go-ahead.
