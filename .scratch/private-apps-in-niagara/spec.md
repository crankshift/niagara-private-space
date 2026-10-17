# Spec: Private apps in Niagara

Status: ready-for-agent

## Problem Statement

I keep apps in Android's **Private Space** (for example, a second copy of my work and messaging apps), and I use Niagara Launcher. Niagara shows **private apps** only in its own Private Space section at the bottom of the app list. It won't let me put a private app in **favorites**, a **folder** or a **pop-up**, use one as a **Niagara-button action**, or use most of the long-press options. The only way to get private apps into my folders was to edit Niagara's database by hand as root.

Niagara can already store, show and launch private apps in all those places, and my hand-made folders prove it. The block is only in Niagara's menus and app pickers. **Work apps** aren't blocked anywhere, so private apps are the odd ones out.

Simply lifting the block isn't enough. Private Space exists to keep those apps out of sight while it is **locked**, and Niagara currently shows locked private apps greyed out instead of hiding them.

## Solution

**Niagara Private Space** is an Xposed module for the Vector framework, scoped to Niagara. Once it's enabled, Niagara treats private apps like work apps everywhere I place or set up an app.

From my side:

- **Adding:** I long-press a private app and get the same options as for a work app: add to favorites, add to a folder, edit its pop-up, set as music app, add to a category, suggestion options, and so on. The pickers for favorites, folders, pop-ups and Niagara-button actions list private apps alongside the rest.
- **Locking:** when I lock Private Space, private apps disappear from favorites. Folders and pop-ups stay visible, their private apps are hidden, and a row reads "Unlock Private Space to see your apps". Tapping that row brings up Android's unlock screen, and the apps come back once I unlock.
- **Niagara button:** if a Niagara-button action is a private app and Private Space is locked, tapping or swiping brings up the unlock screen and then opens the app.
- **Unlocking:** every placement comes back exactly where it was, after any number of lock and unlock cycles and reboots. Locking never removes anything.
- **Niagara updates:** if an update means the module can no longer recognise the code it changes, it changes nothing at all. Niagara behaves as stock, and I get one dismissable home-screen tip for that Niagara version saying the module doesn't support it yet.
- **No settings:** there's no settings screen, no toggles and no launcher icon. I switch the module on by putting Niagara in its scope in Vector.
- **Installing:** I install and update the module from the Vector store.

## User Stories

### Placing private apps

1. As a Niagara user with Private Space, I want to add a private app to favorites from its long-press menu, so that I can open it from my home screen.
2. As a Niagara user, I want private apps listed in the favorites editor's app picker, so that I can add them the same way as personal apps.
3. As a Niagara user, I want to add a private app to a folder from the folder editor's app picker, so that I can group it with related apps.
4. As a Niagara user, I want a folder to hold personal, work and private apps together, so that I can group apps by purpose rather than by profile.
5. As a Niagara user, I want to add private apps to a pop-up on any favorite, so that they're one swipe away.
6. As a Niagara user, I want to attach a pop-up to a private app in favorites ("Edit pop-up"), so that a private favorite can carry its own pop-up like any other.
7. As a Niagara user, I want to set a private app as the Niagara button's tap action, so that my most-used private app is one tap away.
8. As a Niagara user, I want to set a private app as the Niagara button's swipe-up action, so that I can open it with a gesture.
9. As a Niagara user, I want to set a private app as a music app, so that it appears when headphones or speakers connect.
10. As a Niagara user, I want to add a private app to the Leisure Apps category, so that Niagara's distraction features cover it too.
11. As a Niagara user, I want Niagara to suggest private apps below favorites, and the "Stop suggesting" and "Suggest again" options on them, so that suggestions reflect all the apps I use.
12. As a Niagara user, I want to pin a private app's app shortcuts (e.g. "New chat") to favorites or the app list, so that I can jump straight into an action.
13. As a Niagara user, I want "Remove from app list" to work for private apps, so that I can tidy the Private Space section.
14. As a Niagara user, I want to choose private apps for the notification summary, so that their notifications appear there too.
15. As a Niagara user, I want a private app and the personal copy of the same app to be separate entries, so that I can place either or both and each opens its own copy.
16. As a Niagara user, I want private apps in favorites, folders and pop-ups to show Android's Private Space badge, so that I can tell them apart from the personal copy.
17. As a Niagara user, I want the private apps already in my hand-made folders to keep working, so that I don't have to rebuild them.
18. As a Niagara user, I want editing a folder or pop-up that already holds private apps to keep them, so that saving never drops my private apps.
19. As a Niagara user, I want to reorder and remove private apps in favorites, folders and pop-ups exactly as I do other apps, so that nothing behaves differently.

### While Private Space is locked

20. As a Private Space user, I want private apps to disappear from favorites while Private Space is locked, so that someone holding my phone can't see which apps I keep there.
21. As a Private Space user, I want hidden private favorites to come back in their old positions when I unlock, so that my layout never changes.
22. As a Private Space user, I want a folder to stay visible while locked even if it holds only private apps, so that my home screen layout stays stable.
23. As a Private Space user, I want a locked folder to show "Unlock Private Space to see your apps" in place of its private apps, so that I know why they're missing.
24. As a Private Space user, I want a folder with both kinds of apps to show its personal and work apps as normal while locked, with the unlock message below them, so that I can still use the rest of the folder.
25. As a Private Space user, I want tapping the unlock message to bring up Android's unlock screen, so that I can get to my apps in one step.
26. As a Private Space user, I want the folder to show its private apps as soon as I've unlocked from that message, so that I don't have to reopen it.
27. As a Private Space user, I want pop-ups to behave like folders while locked (private apps hidden, unlock message shown), so that the rule is the same everywhere.
28. As a Private Space user, I want a private favorite and its attached pop-up to hide together while locked, so that nothing about the private app shows.
29. As a Private Space user, I want a Niagara-button action that's a private app to bring up the unlock screen when locked and then open the app, so that the button still does what I set it to.
30. As a Private Space user, I want tapping a locked private app anywhere Niagara still shows one (for example in search) to bring up the unlock screen and then open the app, rather than doing nothing.
31. As a Private Space user, I want cancelling the unlock screen to open nothing and change nothing, so that backing out is safe.
32. As a Private Space user, I want private apps to stay hidden from search and suggestions while locked, as Niagara already does, so that nothing leaks through other surfaces.
33. As a Private Space user, I want locking, unlocking and rebooting never to remove a placement, so that I can keep Private Space locked for weeks without losing my layout.
34. As a Private Space user, I want the hiding to depend only on whether Private Space is locked, not on whether an app is paused or suspended, so that suspended private apps still show while unlocked.

### Niagara versions and failure

35. As a module user, I want the module to keep working after Niagara updates without a new module release whenever Niagara's code is still recognisable, so that routine updates don't break my setup.
36. As a module user, I want the module to change nothing at all when it can't recognise the code it needs, so that an update never crashes or half-breaks Niagara.
37. As a module user, I want the module never to lift the blocks unless it can also hide private apps while locked, so that a partial match can never leak private apps.
38. As a module user, I want one dismissable home-screen tip saying "Niagara Private Space doesn't support Niagara <version> yet", in Niagara's own tip style, so that I know why private apps stopped being placeable.
39. As a module user, I want that tip at most once per Niagara version, so that it doesn't nag me.
40. As a module user, I want a short message when the tip system itself can't be found, so that I'm still told.
41. As a module user, I want a line in Vector's module log on every activation saying what was found or missing, so that I can report useful bugs.
42. As a module user, I want folders and pop-ups to fall back to hiding private apps without the unlock message, if only the message row can't be found, so that a fragile cosmetic part never disables the module.
43. As a module user, I want my placements to survive the module being disabled or failing, so that turning it off loses nothing; the private apps just become uneditable again.

### Installing and trust

44. As a Vector user, I want to find "Niagara Private Space" in Vector's module store, so that I can install and update it there.
45. As a Vector user, I want the store page to name the Niagara version it was last tested with, so that I know whether my version is covered.
46. As a Vector user, I want the module to need only Niagara in its scope, with no System Framework hook, so that it can't affect the rest of my phone.
47. As a Vector user, I want the module to have no launcher icon and no settings screen, so that it adds nothing to my phone beyond the change itself.
48. As a user who reads code, I want the source public under GPL-3.0, so that I can check what it does inside my launcher and fork it.
49. As a module user, I want every release signed with the same key, so that updates install over the previous version.

### Maintainer

50. As the maintainer, I want the module's decisions in one module, unit-tested on my Mac, so that I can change behaviour confidently without a phone.
51. As the maintainer, I want an optional test that checks every fingerprint matches exactly once in a local Niagara APK, so that I learn whether a Niagara update breaks recognition before installing it.
52. As the maintainer, I want a written device check covering placing, locking, unlocking and rebooting, so that I can verify a release or a Niagara update on the phone.
53. As the maintainer, I want an agent always to ask me before running the device check, because it locks and unlocks Private Space, restarts Niagara and reads its database as root.
54. As the maintainer, I want pushing a version tag to build, verify and publish the release to both my repo and the store repo, so that releasing is one command.
55. As the maintainer, I want the release refused when the tag doesn't match the APK, the APK isn't signed with the module's key, or the changelog has no section for the version, so that broken releases never ship.

## Implementation Decisions

### Identity and platform

- Package `io.github.crankshift.niagaraprivatespace`, display name **Niagara Private Space**. The scope is Niagara only (`bitpit.launcher`), declared as a static scope.
- Uses the libxposed (modern) API: target API 102, minimum 101. Vector 2.2 provides 102. The legacy `de.robv` API is not used.
- Minimum Android 15 (SDK 35), since Private Space doesn't exist below it. Compile and target SDK follow Origram (37).
- No activity, no launcher icon, no settings, no toggles. The module is only its hook entry point.
- The build copies Origram's setup: Kotlin, AGP 9.4, Gradle 9.7, JDK 21, a version catalog, and R8 with a keep rule for the entry class plus the merged `META-INF/xposed` resources.
- DexKit finds Niagara's code by fingerprint at runtime.

### Module structure

The module has four parts. Only the first holds decisions.

1. **Decision module** (pure Kotlin; no Android, Niagara or Xposed types). It's the single seam the unit tests exercise. It works in the module's own vocabulary:
   - **Inputs** describe an item: its profile kind (personal, work, private), whether Private Space is locked, and the container it's being shown in (favorites, folder, pop-up, Niagara-button action, picker or menu).
   - **Answers it gives:**
     - whether an item gets the full long-press options and picker eligibility (private behaves like work);
     - which rendered favorites rows to keep;
     - for a folder or pop-up, which rows to keep and whether to add the unlock message, given whether the message row is available;
     - what a tap on an item should do: open it now, or unlock Private Space first, remember the app, and open it when Private Space becomes available;
     - from a report of which hooks were found, whether the module activates, with which optional parts, and whether to notify;
     - whether the unsupported-version notice is due, given the current Niagara version and the last version notified.
2. **Finder.** It uses DexKit to resolve every Niagara class, method and field the hooks need from stable anchors: surviving enum names and keys such as `"PRIVATE_SPACE"` and `"favorites_add"`, log strings, Room SQL, resource names and method signatures. It produces a **resolution report** listing each hook as found or missing. Results may be cached per Niagara versionCode.
3. **Hook adapters.** These are thin: each translates Niagara's objects into the decision module's inputs and applies the answer. Nothing in them should need a test of its own.
4. **Notifier.** It shows the unsupported-version notice:
   - Preferably as a Niagara home-screen tip, borrowing the Xiaomi-only tip type `XIAOMI_FULL_SCREEN_GESTURES_MIUI_12`, which never shows on non-MIUI phones. The tip provider and its dismiss and click callbacks are dynamic proxies, dispatched by return type because Niagara's method names change between versions.
   - Otherwise as a Toast.
   - Always with a Vector log line.
   - The last Niagara versionCode notified is stored in a module-owned preferences file inside Niagara's data, never in Niagara's own keys.
   - **Result (ticket 08, 2026-10-17):** the tip is registered only once the home page is up, because a provider that fails to build crashes it; the tip type is looked up by its name string, since its field is obfuscated; the notice also covers a finder that could not run, and hooks that could not be installed. See `docs/research/niagara-internals.md`.

### Activation rule

- **Essential hooks:** the long-press menu gate, the picker filters (and the picker row step that keeps locked private apps out of the lists of apps already chosen), the favorites rendering filter, the folder and pop-up row filter, and the launch interception. If any essential hook is missing, the module activates **nothing** and notifies. Lifting the blocks without being able to hide locked private apps would leak them.
- **Optional parts:**
  - The folder and pop-up **unlock message row**. If it's missing, private apps hide without a message.
  - The **tip system**. If it's missing, the notice falls back to a Toast.

### Niagara facts the design relies on

These come from Niagara 1.16.32 (versionCode 1638); see the research notes.

- **The block** is one per-item "is Private Space" flag, checked only in UI code: the long-press options builder, which returns early for private apps, and the app-picker filter plus its overrides (favorites editor, Niagara-button action pickers, music apps, folder and pop-up editor, hide apps, Leisure category, notification summary). There are also small checks for suggestions and for pinning private app shortcuts. The module neutralises the flag **only within those checks** and never globally, because the Private Space section in the app list and hiding in search depend on it. The flag is a final field; whether reflection can write it at hook time is unverified, and the alternative is to override the check's result.
- **Leisure Apps** also requires a personal app, even for work apps. Per the user's decision, the module lifts that requirement for private apps.
- **Storage** already keeps the user in each app record, as Android's **user serial number** (not the user id). Folder and pop-up contents are rows pointing at app records. The module never writes Niagara's database. Placements are created by Niagara's own code once the block is lifted.
- **Locking** only sets a "locked" flag on the private records. Niagara's startup scan still sees private apps while locked; this was verified on the phone on 2026-10-17, and no placement was marked missing. So the module needs **no** protection against Niagara's missing-app cleanup. Uninstalling a private app or deleting Private Space is left entirely to Niagara's cleanup.
- **Locked detection:** use "is Private Space" together with Android's quiet-mode state for the private user (or Niagara's locked flag). **Never** use Niagara's "hidden everywhere" state, which is also true for suspended apps while unlocked.
- **Favorites hiding** filters only the rows being rendered, **never the stored favorites list**. The next save would otherwise delete the hidden placements.
- **Folder and pop-up hiding** filters the rows built by the shared folder and pop-up list controller, then appends a borrowed instance of the pop-up's own empty-state row with the text and click replaced. This is the fragile part, so it's optional.
- **Unlock:**
  - The module calls Android's quiet-mode request for the private user directly, with Niagara's own unlock path as a fallback. It works only while Niagara is the foreground default launcher, which is always the case for these taps.
  - The variant that also starts an app needs a system permission. So the module remembers the pending app, opens it itself when Private Space becomes available, and forgets it if the unlock is cancelled.
  - **Result (ticket 06, 2026-10-17):** Android sends no signal when the user backs out of the unlock screen, so the pending app is forgotten when a second tap replaces it, when Private Space locks again, and after a minute. The module opens it on `PROFILE_ACCESSIBLE` (the user is running and unlocked), and on `PROFILE_AVAILABLE` only if the user is already unlocked. Niagara's own unlock path is not built as a fallback: it ends in the same framework call, so it cannot succeed where the direct call is refused. All of it is unverified on LineageOS 24.
- **Launch interception** hooks the single Niagara method that starts apps. That covers Niagara-button tap and swipe-up actions, favorites, pop-ups and search.

### Publishing and release

- **Repos:** the public repo `crankshift/niagara-private-space` under GPL-3.0, and the store repo `Xposed-Modules-Repo/io.github.crankshift.niagaraprivatespace`, created through the store's submission form. The store repo's GitHub description must be **"Niagara Private Space"**. With the Maintain role it can only be set on the website, not the API, and the store leaves out repos without one. The store repo carries README, SUMMARY, SOURCE_URL and SCOPE, and never HIDE.
- **Tags** use `<versionCode>-<versionName>`, with the APK attached to a non-draft release.
- **CI** follows Origram's two workflows:
  - Build on push and on PRs.
  - On tag:
    1. Verify the tag matches the APK version.
    2. Verify the signing certificate's SHA-256 matches the module's pinned value.
    3. Require a CHANGELOG section for the version.
    4. Publish to both repos, and sync the store page files to the store repo.
- **Signing:** a new key, not Origram's. It's generated with `keytool` outside the repo, backed up by the user in Bitwarden, and given to CI as the secrets `NIAGARAPS_KEYSTORE_BASE64` and `NIAGARAPS_STORE_PASSWORD`, plus `MODULES_REPO_TOKEN`, a classic PAT with `public_repo`.
- **Store page:** it names the last Niagara version tested.
- **Device check:** a device-check document is added, and `AGENTS.md` gets a line saying the check is optional and an agent must always ask before running it.

## Testing Decisions

- **What a good test is:** a test exercises behaviour through the decision module's public interface, in the glossary's terms. For example: "a locked private app is hidden from favorites", "a folder of only private apps shows the unlock message while locked", "a missing favorites filter keeps the whole module inactive". Expected values come from the decisions in this spec, never recomputed the way the code computes them. No test mocks Niagara or reaches into the hook adapters.
- **Seam 1, the decision module, is the main seam:** plain JVM unit tests on the Mac, covering:
  - which blocks are lifted;
  - favorites filtering while locked;
  - folder and pop-up contents with and without the message row;
  - launch decisions, including pending-unlock launches and cancelled unlocks;
  - activation from resolution reports (all found, an essential missing, only optional parts missing);
  - when the notice is due.
- **Seam 2, fingerprint resolution, is optional:** a JVM test runs the Finder against a Niagara APK pulled locally and asserts that every fingerprint resolves to exactly one target. It's skipped when no APK is present, because Niagara's APK must never be committed to this public repo. The first implementation ticket confirms that DexKit runs on the desktop JVM. If it doesn't, this check moves to the device via the Vector log line.
  - **Result (ticket 01/02, 2026-10-17):** DexKit has no desktop library as published. Its only artifact is an Android AAR with Android libraries, so a macOS library has to be built from DexKit's source (not yet done; DexKit has never been run on the Mac). The test exists (`FingerprintTest`) and is skipped unless both a Niagara APK (`NIAGARA_APK`) and that host library (`DEXKIT_LIBRARY`) are supplied. Until someone builds the library, the check is the activation log line on the phone. The finder's queries were checked meanwhile by emulating them over a `dexdump` of Niagara 1638; see `docs/research/niagara-internals.md`.
- **Seam 3, the device check, is manual and optional, and only run after asking the user.** It covers what the JVM can't:
  - the real menus and pickers;
  - adding a private app to every placement;
  - locking (favorites hidden, unlock message shown) and unlocking through the message and the Niagara button;
  - cancelling an unlock;
  - rebooting while locked;
  - reading Niagara's database as root to confirm placements are intact.
- **Prior art:** Origram's JUnit Jupiter tests (backtick behaviour names, pure functions with injected lookups) are the model for the decision module's tests. This repo has no tests yet.

## Out of Scope

- **Widgets from private apps.** Android only gives a launcher another profile's widgets if that profile's administrator allows it, and Private Space has no administrator. Lifting that would need a System Framework hook. Possible later work, as a separate effort.
- **OEM vaults** (Samsung Secure Folder, vendor hidden-app spaces). Only Android's built-in Private Space is covered.
- **Any change to work apps or the work profile.**
- **Niagara's Private Space section** in the app list, and its hiding of private apps from search and suggestions while locked. Both stay as they are.
- **Settings, toggles, a launcher icon or any app screen.**
- **Writing Niagara's database** from the module, or migrating existing hand-edited rows. They already work.
- **Protecting placements from Niagara's missing-app cleanup.** It's verified unnecessary.
- **Drag and drop:** Niagara has no drag-and-drop path for adding apps.
- **Android versions below 15, and the legacy Xposed API.**

## Further Notes

- **Research:** `docs/research/niagara-internals.md` covers Niagara's internals, hook points and fingerprint anchors for 1.16.32. `docs/research/vector-store-publishing.md` covers how the Vector store lists modules.
- **Test device:** Android 17, LineageOS 24 nightly, KernelSU root, Vector 2.2. Private Space is user 10 (serial 10) and the work profile is user 11.
- **Unverified, to settle during implementation or the device check:**
  - Android behaviour taken from AOSP source but not checked on LineageOS 24.
  - Whether a pop-up stays open behind the unlock screen.
  - Whether the final "is Private Space" field can be written by reflection. **Result (device check, 2026-10-17):** yes, on the test phone: with the module on, a private app's long-press menu offers Favourite, which only happens when the options builder sees the flag cleared, and no error was logged. Seen for the options builder only.
  - Whether Leisure Apps and app-shortcut pinning work for private apps once unblocked.
  - Whether locked private favorites are truly greyed today; the code says so.
- **Origram's tag-triggered release** has only ever had a manual dry run. This module's first tagged release will be the first real run of that workflow, so watch it.
- **Store timing:** once a release exists and the store repo has its description, expect about 8 days before the module appears in Vector.
