# 01: Add a private app to favorites from its long-press menu

**What to build:** This is the first thin slice through the whole module. With the module enabled in Vector, long-pressing a **private app** in Niagara offers the same options as a **work app**, including "Favorite" and "Edit pop-up". Choosing "Favorite" adds it to **favorites**, where it opens normally and shows Android's Private Space badge.

This ticket also sets up the module:
- the project, from Origram's template;
- the **decision module**, the seam the unit tests exercise;
- the **finder**, which locates Niagara's code by fingerprint with DexKit;
- the **resolution report** and the all-or-nothing **activation rule**: if any essential hook is missing, the module changes nothing and writes a line to Vector's log.

See the spec: `.scratch/private-apps-in-niagara/spec.md`.

**Blocked by:** None (can start immediately).

**Status:** done-pending-device-check

- [x] **Project:** a Gradle project copied from Origram's setup (Kotlin, AGP 9.4, Gradle 9.7, JDK 21, version catalog), with package `io.github.crankshift.niagaraprivatespace` and display name "Niagara Private Space". It has minimum Android 15, compile and target SDK 37, and no activity or launcher icon.
- [x] **Module metadata:** libxposed API 102 (minimum 101) with a static scope of only `bitpit.launcher`. R8 keeps the entry point.
- [x] **Decision module:** pure Kotlin with no Android, Niagara or Xposed types. It answers whether an item gets the full long-press options, so that a private app is treated like a work app and personal and work apps are unchanged. JUnit Jupiter tests with backtick behaviour names, as in Origram, cover it.
- [x] **Finder:** resolves the long-press options builder and the per-item "is Private Space" flag with DexKit from stable anchors. It's checked against Niagara 1.16.32 (1638). *(Checked by running the same structural queries over the APK's dex dump; the DexKit run itself moves to the phone's activation log line. See Notes.)*
- [x] **Hook scope:** the hook neutralises the "is Private Space" check only inside the options builder, never globally. The Private Space section in the app list and hiding in search still behave as stock.
- [x] **Activation:** the decision module turns a resolution report into activate or don't. When a required hook is missing, no hook is installed and Vector's log gets one line naming what's missing. Unit tests cover both outcomes.
- [x] **Log on success:** on activation, Vector's log gets one line listing what was found.
- [x] **CI:** a build workflow runs the build and unit tests on push and PR.
- [x] **Repo files:** a GPL-3.0 `LICENSE` and a short README.
- [x] **Device check:** `docs/device-check.md` exists, with steps for this slice (long-press a private app, add it to favorites, open it, see the badge). `AGENTS.md` gains a line saying the device check is optional and an agent must always ask the user before running it.
- [ ] **On the phone:** a private app is added to favorites through the long-press menu and opens normally, with the user's go-ahead.

## Notes

Decisions made without the user, each the option closest to the spec and research notes.

- **DexKit does not run on the Mac from Maven Central.** The `org.luckypray:dexkit` 2.3.0 AAR carries Android ELF libraries only (arm64-v8a, armeabi-v7a, x86, x86_64), and the code loads them with `System.loadLibrary("dexkit")`. A host library would have to be built from DexKit's C++ source; the auto-mode classifier refused building code cloned from GitHub, so I did not. Per the spec, the fingerprint check moves to the phone through the activation log line. Recorded in the research notes; ticket 02 settles the test side.
- **Fingerprints were checked without DexKit.** I parsed `dexdump -d` of Niagara 1638's `classes.dex` and ran the Finder's queries by hand: one class holds `"Ii(l=***, i="` (the item base); one method with signature `(ItemBase, ArrayList)V` holds `"bitpit.launcher.key.SHOW_MOST_USED_APPS"` (the options builder); that builder reads exactly one boolean field of the item base (the Private Space flag). The constructor of the item base writes the primary-user flag first and the Private Space flag second.
- **How the block is lifted: neutralise the final flag for one call.** `ItemFlags.whileTreatedAsWork` clears the flag on the one private item being asked about, runs Niagara's own method, and restores the flag in `finally`. Niagara's own code then runs unchanged, so no Niagara rule is re-implemented. The alternative, overriding the result, would skip the rules that follow the Private Space check. The flag is `public final boolean`, which reflection can write after `setAccessible(true)`; this is still unverified on ART. If a write is refused the hook throws, protective mode runs Niagara's own code and private apps stay blocked (fails closed).
- **Known small race.** While the flag is cleared, another thread reading the same item's flag would see "not private". The window is one options-builder call (microseconds, on the main thread) and only for the item under the user's finger.
- **Two profile flags, not one.** The finder also resolves the primary-user flag (`PRIMARY_USER_FLAG`), so `ItemFlags.kindOf(item)` can give the decision module the item's real profile (personal, work or private) and `whileSeenAs` can show Niagara another one for a single call. Ticket 03 needs it for Leisure Apps anyway. The finder confirms the pair against the item base class's constructor: exactly two booleans are assigned, primary-user first, and the options builder's boolean must be the second. DexKit's native source (`dex_item.cpp`, `GetUsingFields`) appends field uses in instruction order without de-duplicating, so that order is reliable.
- **Decision module shape.** `Decisions.profileForLongPressOptions(kind)` answers which profile Niagara should treat the app as (private becomes work, the others stay), `Decisions.activation(report)`, `Hook`, `ResolutionReport` and `Activation` (with `logLine()`). Every hook is required for now, so each ticket adds hooks to the essential set by adding enum entries. The activation tests name the hooks explicitly, so a new hook means a new test: that is deliberate. The essential/optional split, `notify` and "which optional parts" arrive with the first optional hook (the unlock message row, ticket 07) and the notice (ticket 08), tested then.
- **Must not ship alone.** The spec's essential set also contains the favorites filter, the folder and pop-up row filter and the launch interception. Until those tickets land, a build of this module lets a locked private app be favorited while Private Space is locked, and Niagara still shows it greyed out. That is only acceptable for a device check on the user's own phone; nothing is released before ticket 10.
- **Review round.** /code-review found tautological tests (they read the code's own `essential` flag, and a decision that was a constant `true`), an unused `kindOf`, an unused `Scope`, and a log line that said both "inactive" and "active", a finder that trusted "the one boolean the builder reads" without checking it, an adapter that fed the decision a hard-coded kind, and a native-library load with no fallback (now `System.loadLibrary`, then the module's own `nativeLibraryDir`). All fixed; `Scope` comes back in ticket 04, where the options sheet needs it. Added `docs/adr/0001-clear-the-private-space-flag-for-one-check.md`. The reviewer's point that `done-pending-device-check` is not in the triage vocabulary is waved off: it is the status the user asked for in the run instructions.
- **Resolution is all-or-nothing at install time too.** `InstalledHooks` removes every hook if installing any of them throws, and the installer first checks that the runtime lets reflection write a final field (a probe on a class of our own), so a refused write shows up as an `inactive` log line instead of an error on every long-press.
- **Module metadata.** `exceptionMode=protective` as in Origram. I left out Origram's `autoHotReload`: the module keeps no state that a hot reload would need to carry.
- **Native libraries.** Only `arm64-v8a` and `x86_64` are packaged (Android 15+ is 64-bit only in practice), and `useLegacyPackaging = true` extracts them so Vector's class loader can find `libdexkit.so` in the module's library directory.
- **Niagara version in the log line** is read with `getPackageArchiveInfo` on the APK, using the activity thread's system context, because the Application does not exist yet in `onPackageReady`. Failure prints `?`.
- **Not done here:** release signing (ticket 09). Release builds use the debug key until then.
