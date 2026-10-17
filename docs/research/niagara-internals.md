# Niagara internals relevant to Private Space

Checked 2026-10-17 against **Niagara Launcher 1.16.32 (versionCode 1638)**, `bitpit.launcher`, minSdk 26, targetSdk 36. The APK was a single `base.apk` with no splits, SHA-256 `e3c8adcd6a2a627a2319edcf3f1b15436b2800efba3abcd62e00236c2b5c93ef`. It was pulled to `/tmp/niagara-apk/1638/` and decompiled with jadx 1.5.6 into `/tmp/niagara-src`. Two methods that jadx could not decompile were redone with `--decompilation-mode fallback` into `/tmp/niagara-src-extra/`. Class and member names below are the obfuscated names from that tree, all in package `b`. Unless a line says "inferred" or "unverified", it was read directly in the code.

## Device facts (OnePlus CPH2747)

- **OS**: Android 17, SDK 37. The ROM is LineageOS 24.0 nightly (`ro.lineage.version=24.0-20261014-NIGHTLY-infiniti`), build `CP2A.260605.016`, security patch 2026-10-01. It is not OxygenOS; only the vendor partition is OnePlus.
- **Root**: KernelSU 3.4.2, with BreZygisk (`rezygisk` module) providing Zygisk.
- **Users** (`pm list users`, `dumpsys user`, `cmd user list -v`):

  | id | serialNo | name | type | notes |
  |---|---|---|---|---|
  | 0 | 0 | Yurii Yesipov | `full.SYSTEM` (MAIN, PRIMARY) | current user |
  | **10** | 10 | Private space | **`android.os.usertype.profile.PRIVATE`** | `mProfileApiVisibility=1` (hidden), `mItemsRestrictedOnHomeScreen=true`, `mAuthAlwaysRequiredToDisableQuietMode=true` |
  | 11 | 11 | Work profile | `android.os.usertype.profile.MANAGED` | has a profile owner (Intune "Company Portal" is in it) |

  User 12 no longer exists. It was a removed profile, but App rows for it remain in the DB (see the DB section). There is no clone profile.
- **Private Space is unlocked now**: user 10's flags are `INITIALIZED|PROFILE` with no `QUIET_MODE`, and its state is `RUNNING_UNLOCKED`. The work profile is not paused either.
- **Niagara** 1.16.32 / 1638 is the default launcher: it holds `android.app.role.HOME`, and HOME resolves to `bitpit.launcher/.ui.HomeActivity`. It is installed only in user 0. It has `ACCESS_HIDDEN_PROFILES` (granted as an install permission) and `QUERY_ALL_PACKAGES`.
- **Many work and private apps are suspended by root.** For example, `com.Slack` and `com.google.android.calendar` in users 10 and 11 have `suspended=true` and `suspendingPackage=<10>root` or `<11>root`. Niagara reflects this as App flag 128 (see below), which explains the `flags=160` on the private apps that were hand-inserted into folders.
- **Xposed framework**: Vector `v2.2 (3080-88f8e1fa-JingMatrix-Vector)`, versionCode 3080, Zygisk module id `zygisk_vector`. The manager is parasitic: `org.matrix.vector.manager` is injected into `com.android.shell` (see `action.sh`), and no manager package is installed. In `framework/vector.dex`, `io.github.libxposed.api.XposedInterface` defines `API_101=101`, `API_102=102` and `LIB_API=102`, and the same values appear in `io.github.libxposed.service.IXposedService`. So it implements **libxposed API 102** and also accepts 101. No module is scoped to `bitpit.launcher` yet. The user's libxposed module `io.github.crankshift.origram` loads fine.

## Niagara's database (read-only SELECTs, Room schema version 69)

- **`App.user` holds the user serial number, not the user id.** Serial numbers come from `UserManager.getSerialNumberForUser`. On this phone they equal the ids (10 and 11), but any module code that writes or matches rows must use serial numbers.
- **Feed rows.** Columns are (`id`, `type`, `item_index`, `item_id`, `item_type`). The item id is packed as `subId + (itemType << 24)`, from `xRRaExrA8qXXMR8.S6IAQCLljUFq4a51beP6ri()`. Item types are 0 = App, 1 = Widget, 2 = Shortcut, and folders are Shortcut rows with `type=3`. That is why a folder's Feed id is `33554432 + Shortcut.id`.
  - Feeds with `type=0` are global lists: id 4 = favorites, id 10 = Niagara-button tap action, id 11 = Niagara-button swipe-up action, plus others (see `jITGD5xX7idP.<init>`).
  - Feeds with `type=1` hold the contents of a pop-up or folder. Their `id` is the packed id of the host item.
- **Current data on the phone:**
  - Favorites (Feed 4) has 7 personal apps plus the folders Maps (451), DataArt (453) and Ciklum (452).
  - The pop-ups are hosted on App ids 51 (Phone), 53 (Telegram), 141 (Termux) and 247 (MiX). **Feed id 138 ("Immich pop-up") no longer exists.**
  - The Ciklum folder (33554884) holds 14 private apps (user 10, mostly `flags=160`) and 1 personal app.
  - The DataArt folder (33554885) mixes work apps (user 11), private apps (user 10) and personal apps.
  - Row counts per user: user 0 has 389 App rows, user 10 has 49, user 11 has 43, and the removed user 12 has 13 rows, all with flag 256.
- The Feed row has no user column. The user is stored only in `App.user` and `Shortcut.user`.

## (a) How Niagara enumerates profiles and shows private apps

- **Profile registry: `GIMQ3HGidL8HUz6rU0DZZV5VKZ2gf`.**
  - `QCgZq9Gn9a7N1ia2hQxoHljG7Fg8()` (line 110) builds a serial↔`UserHandle` map from `UserManager.getUserProfiles()`. It rebuilds the map on `PROFILE_ADDED/REMOVED` (`MANAGED_PROFILE_*` on API < 35), in `koJbbTeaDcqStifPh8870RhirvL1(Intent,String)`.
  - `ursJAofciE(UserHandle)` (line 266) converts a `UserHandle` to a serial, returning -1 if unknown. `S6IAQCLljUFq4a51beP6ri(long)` (line 133) converts a serial to a `UserHandle`, returning **null if the serial is not a current profile**.
  - `qxwCegY8aElo6DGrOJdOOl3e(UserHandle)` (line 250) returns the profile kind: PRIMARY for `myUserHandle`. On API < 35 every other profile is WORK. On API 35+ the result comes from the static `RmTud9HmErhlU3DkQSc` (line 46), which calls `LauncherApps.getLauncherUserInfo(user).getUserType()`: `"android.os.usertype.profile.MANAGED"` gives WORK, `"android.os.usertype.profile.PRIVATE"` gives PRIVATE_SPACE, and anything else (for example a clone profile) gives null. Results are cached in a `ConcurrentHashMap`.
  - The profile-kind enum is `hE1r6UMnZ5lo8vIKm6`, and its names survive obfuscation: `PRIMARY`=`FcT8vexLQsFH8TD9PCCur`, `WORK`=`ekHpxv2EEaKM9GeUzzQBWAMCIwZ`, `PRIVATE_SPACE`=`Maiu1xWivs2Pl`.
  - Niagara does not use `LauncherApps.getProfiles()` or `UserManager.isPrivateProfile()`.
  - Other private-space helpers: `JtqOZVRSikW68pEfM()` returns the private profile's handle, and `MSIC9osuP4uEvtwoX8Ls()` returns true if Private Space exists and is locked (in quiet mode). `myg4xWl4jc7ydenlyX4YW(...)` locks or unlocks it through `UserManager.requestQuietModeEnabled`, in the lambda `bhy75HKiWNVJcn4MG`. Quiet state is snapshotted per profile with `isQuietModeEnabled` in `y12p3NmB0nospWRsnG8Ssp`.
- **App enumeration: `oYIYWo38q4RZRRlamGCdHzBu`.** This class extends `LauncherApps.Callback`. Its `oxtZuOAT85CRE5mHS8QhcvJYV()` calls `LauncherApps.getActivityList(null, user)` for every profile. It returns the combined activities plus a set of profiles whose call threw `NullPointerException` or `SecurityException` (the "failed" profiles). The same class also calls `getPrivateSpaceSettingsIntent()`, `getAppMarketActivityIntent(pkg, privateUser)` ("Install app") and `setArchiveCompatibility`.
- **The item base class is `U2m3eJXDAz`.** Its `toString` returns `"Ii(l=***, i=…)"`. Its constructor (lines 59–62) computes the profile kind once and stores two **final** boolean fields:
  - `Dm93ilxRylvqICs` = isPrimaryUser
  - **`JivfzRelv6lvjdmUmyDxa` = isPrivateSpace**

  `InBLWr22CpY()` (line 98) is the Kotlin getter `shouldBeHiddenEverywhere`; the name survives in `wcRvdaV9smApv7skeMrS2D7hWxJx`. It returns `isPrivateSpace && !cNZSwVhgw3bj87Y()`, where `cNZSwVhgw3bj87Y()` means the item is active. App items (`AAqdJflv6N9m3Mj`) count as active when `flags & 9600` is 0, meaning none of 128, 256, 1024 or 8192 is set.
- **All-apps list: `wc9ii3Nltew959oWw`.** At line 119, private apps are partitioned out of the main list into a separate section at the bottom. The section has the shield header `R.string.private_space`, key `"private_space"`, and header and footer rows.
  - When Private Space is locked, only the header row is shown. With the setting `shouldHidePrivateSpace` ("Hide private space while locked") on, nothing is shown.
  - Searching "private space" then offers "Unlock private space" (`uTRxllORq4JEtizXzxiQD4S1MkbQU`). The lock and unlock entries are `vU6PUKjz60Ch`.
  - Work apps stay in the main list.

## (b) The gate

There is **one boolean that gates everything: `U2m3eJXDAz.JivfzRelv6lvjdmUmyDxa` (isPrivateSpace)**. It is checked only in UI code: menus, pickers and suggestions. The gate is specific to Private Space; work-profile apps pass every check below.

1. **Long-press menu: `F5CBVETNWGKDUskOP3paHecjTS.S6IAQCLljUFq4a51beP6ri(U2m3eJXDAz, ArrayList)`** (lines 215–297). It is called from `VTeRaLtcXgU.<init>` (the options sheet) and reads `boolean z = item.JivfzRelv6lvjdmUmyDxa`.
   - BLACKLIST_* is added only `if (isApp && !z)`.
   - After APP_INFO, UNINSTALL, EDIT_ICON_OR_NAME and USAGE_STATS, there is **`if (z) return;`** (line 262).
   - So a private app never gets SUGGESTIONS_*, **FAVORITES_ADD**, FAVORITES_ADD_FROM_BLACKLIST, FAVORITES_EDIT, SHORTCUT_PIN_AND_ADD_TO_FAVORITES, **EDIT_POP_UP_EXPANDED** (attach a pop-up to this app), MEDIA or ADD_TO_CATEGORY.
   - The option enum is `NKrABSLhdNikuc0Vg6ehSB2Kf`. Its names and keys survive, for example `FAVORITES_ADD("favorites_add")` and `EDIT_POP_UP_EXPANDED("edit_pop_up_expanded")`.
   - A related check in `VTeRaLtcXgU` (line 100) hides SHORTCUT_PIN_AND_ADD_TO_APP_LIST for private deep shortcuts.
2. **Every app picker: `HdxOm26AKWF.gYACuz9hbjk08FgXc(U2m3eJXDAz)Z`** (line 985). The default body is `return !item.JivfzRelv6lvjdmUmyDxa;`. The list builder `ho3Z3OuW2p` (case 0, lines 543/568/654/744) applies this filter to the candidate apps. It does not apply it to the already-selected items: `HdxOm26AKWF.Dm93ilxRylvqICs()` copies the feed into the selection unfiltered, and saving writes the selection back. That is why private apps that were hand-inserted survive editing.
   - Subclasses that do not override the filter (private apps blocked by the default): the **favorites editor** `WttjFnHbnCI9TUvUZ7HEvaCDx5` ("Your favorites", analytics strings `"select_favorites"` and `"onboarding_add_settings_shortcut_to_favorites"`), the Niagara-button tap and swipe-up action picker `qGiFZhQ3nGmNGbSASyLsm2J` (feeds 10 and 11), and the music-apps picker `ji6MqJ2yTs62Cpul0hn`.
   - Subclasses that override the filter and also reject private apps:
     - **`tNjvAAlk5hkIdUHaZLEjCI`** (line 221) is the **pop-up and folder editor**. It uses `R.string.pop_up_edit`, `R.string.specific_app_shortcuts` and `R.string.app_options_edit_sesame_shortcuts`. Its filter is `if (PS) false; if (item == host) false; …shortcut rules…`.
     - `IOoBH2qZjVZzBwOutRLT0kq`: hide apps.
     - `Chs0oAk7wWTVaO9My18`: leisure category, which also requires a primary-user app.
     - `p673Nutn1RVJD4K6u9xnF1`: notification summary apps.
3. **Other private-space checks** (do not need changes for placement):
   - `gqD7qG2N3MYoNNJ:34`: most-used suggestions skip private apps.
   - `XWa5EoybyzcxRoHWOT1gSM7xX:258`: the "Add shortcut" screen skips private apps.
   - `XoYnnUj9fSu:108`: the widget picker skips private widget providers.
   - `wc9ii3Nltew959oWw:119`: the all-apps list moves private apps into their section. **This one must keep working**, so do not flip the field globally.

**Drag and drop**: I found no drag-and-drop path for adding an app to favorites, a folder or a pop-up. The only `OnDragListener` and `DragEvent` users are Compose's built-in drag-and-drop support and an `EditText` subclass. The edit lists only offer "Drag to reorder". This conclusion comes from searching, not from tracing every touch handler.

The storage layer has no Private Space check: neither the feed `add` (`kwpCNXnI9hAtHMzdAza1dSx.add/addAll`, line 96) nor the feed writer does one. The FAVORITES_ADD handler (`wOBkmtp9a6RBsaeG`, case 7/11) simply calls `favoritesFeed.add(item)`.

## (c) Every placement path

| Path | Code | Private apps blocked? |
|---|---|---|
| Long-press, "Favorite" | F5CB… options, FAVORITES_ADD | Yes (`if (z) return`) |
| Favorites editor | `WttjFnHbnCI9TUvUZ7HEvaCDx5` → base filter | Yes |
| Folder contents ("Create pop-up folder", then editor) | `tNjvAAlk5hkIdUHaZLEjCI.gYACuz9hbjk08FgXc` | Yes |
| Pop-up contents (pop-up on any host) | same editor | Yes |
| Private app as pop-up **host** | EDIT_POP_UP_EXPANDED in F5CB… | Yes (option hidden) |
| Niagara button tap and swipe-up action | `qGiFZhQ3nGmNGbSASyLsm2J` (feeds 10, 11) | Yes |
| Music apps, category, hide apps, suggestions, notification summary, leisure | `ji6Mq…`, ADD_TO_CATEGORY, `IOoBH…`, `gqD7q…`, `p673N…`, `Chs0o…` | Yes |
| Private deep shortcuts to favorites or app list | F5CB… / `VTeRaLtcXgU:100` / `XWa5…:258` | Yes |
| Contextual profiles | `ContextualProfile` table is empty; no separate picker class found | Not found (unverified) |
| Search bar | not a placement target; private apps are searchable unless `shouldBeHiddenEverywhere` | n/a |
| Drag and drop | none found | n/a |

There are exactly 8 picker subclasses of `HdxOm26AKWF`: `l53VGFijSISw4` (abstract, widget-capable) with subclasses `WttjFn…` and `tNjv…`, plus `IOoBH…`, `Chs0o…`, `ji6Mq…`, `p673N…` and `qGiFZh…`.

## (d) How Feed and App rows round-trip

- **Writing App rows**: `FLg9AR3yWreM58h8ZUCuKhMhnN2JF.S6IAQCLljUFq4a51beP6ri(LauncherActivityInfo, PackageInfo, int, boolean)` (line 54) builds the full entity `GDNqlxIXvtYCZ3sGR0BD0n6cf` with `user = registry.ursJAofciE(lai.getUser())` and throws `"serial number invalid"` if the serial is -1. Updates go through `mZcY0deygk6u3(...)`. The Room SQL is `INSERT OR REPLACE INTO \`App\`` in `CYSfFf4TRDlvX94ohrvPKK5` and `UPDATE OR ABORT \`App\`` in `qb6ItAD2pGyDkF9UUeGavQL`.
- **Reading App rows**: `lnhEodgGE7jNgfOxK58piATkW9JB6.x84QUk1KZDQn9Oaki3QtLcQFH19KN` (around line 320) loads **every** App row, with no profile filter, into an array indexed by id as `AAqdJflv6N9m3Mj`. Its constructor resolves the user with `registry.S6IAQCLljUFq4a51beP6ri(serial)`. Launching goes to `startActivity` for primary-user apps and to `LauncherApps.startMainActivity(component, user)` otherwise; this is `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E`.
- **Feeds**: `jITGD5xX7idP.<init>` (lines 83–128) builds the feeds from the DB. Each `FeedEntity` (`y6eFBMS0C2ooKOwYZuTnJt9K27`, `toString "FeedEntity(type=, group=, infoId=, index=…"`) is resolved through `lnhEod….MSIC9osuP4uEvtwoX8Ls(itemId, true)`. **Rows whose item does not resolve are dropped silently.** Writing happens in `bscGd6DHAcw6NpYRID6mLD0P` around line 215: each item gets `item.S6IAQCLljUFq4a51beP6ri()` (a persistability check that is not about Private Space), and the feed is rewritten with `INSERT OR REPLACE INTO \`Feed\``. After any edit to a feed, its unresolved rows are therefore gone for good.
- **Places that drop or change non-primary entries:**
  - A serial that is not a current profile makes the item's `UserHandle` null. The item then counts as neither primary nor private, and launching it fails with a toast.
  - Unresolved feed rows are dropped.
  - Feed rows are cleaned up when the purge task below deletes their App rows. I inferred this from `xMLtIEEVVOP6zh.S6IAQCLljUFq4a51beP6ri(xRRaExrA8qXXMR8)` putting deleted ids into its removed sets; I did not trace the commit.

## (e) Profile unavailable or removed: flags 256 and 512

Meanings of the App flag bits:

| Bit | Meaning | Where it is set |
|---|---|---|
| 2 | `FLAG_EXTERNAL_STORAGE` | computed in FLg9…:112 |
| 8 | `FLAG_SYSTEM` | computed in FLg9…:112 |
| 32 | user-renamed label | kept across syncs (`& 6197`) |
| 128 | `ApplicationInfo.FLAG_SUSPENDED` | computed in FLg9…:112 |
| 256 | "disabled": the app has gone missing | set by `oiyuEbE66iPyXxs` mode 0 |
| 512 | "uninstalled/hidden" | set by `oiyuEbE66iPyXxs` mode 1 |
| 1024 | the profile was in quiet mode (locked or paused) at sync time | computed in FLg9…:112 |
| 4096 | a pop-up host setting, inverted `kpdqrhvnShOoqekLp9CU`; exact UI meaning unverified | toggled in `tNjv…` |
| 8192 | `ApplicationInfo.isArchived` (API 35+) | computed in FLg9…:112 |

Bits 1, 4, 16, 2048 and 4096 are user flags and are kept across syncs (the `& 6197` mask). `q6GDDRWaiG40m()`, meaning installed, is `(flags & 768) == 0`. Items that are not installed get the INSTALL and REMOVE_UNINSTALLED options instead of the normal ones.

What happens on each event:

- **Locking or unlocking Private Space** (`PROFILE_AVAILABLE/UNAVAILABLE` on API 35+; `MANAGED_PROFILE_*` below): `c1f9HeekFzJZbklkcB2` handles the broadcast for WORK and PRIVATE_SPACE profiles and runs `hKms7hC9dI3Pvae`. That task sets or clears **App flag 1024** on all rows of that user serial (`SELECT … FROM App WHERE user = (?)`) and Shortcut flag 32. Nothing is deleted and feeds are untouched.
  - While locked, private apps are inactive, so `shouldBeHiddenEverywhere` is true and they are hidden from search and suggestions.
  - I found no filter on favorites, folder or pop-up rendering. Inferred: locked private apps there are shown greyed (inactive icon in `UFrPni1ntXYKJaAguWIdelrJ8vVFw:59`), not hidden. **Verify on the device.**
- **`LauncherApps.Callback.onPackagesUnavailable` is only logged** (a no-op). `onPackagesAvailable`, `Suspended`, `Unsuspended`, `Added` and `Changed` cause a resync of that package (`gmzP0HCEbgz8` → `r63gB0FiUmRa`). In that resync, **an App row whose activity is no longer returned by `getActivityList(pkg, user)` is deleted outright.**
- **`onPackageRemoved`** (event 3) runs `oiyuEbE66iPyXxs(mode 1)`: it sets **flag 512** and deletes that package and user's pinned Shortcut and Widget rows. The App row and its feed slots stay.
- **Startup reconciliation, `CheckAppsChangedTask`** (`ZKorSI85LI6`; body in `P9yYamKOJElGYT6jUsIu`, fallback decompile). The results are dispatched in `VZbTWt3LDLJsFtQocAKcmCr`. For each DB row with no matching activity:
  - It is skipped if its user is a "failed" profile, or if it is on external storage and that storage is unavailable.
  - Otherwise, the first time it goes into "disable" (event 2: **flag 256**).
  - If it already has 256 and `last_updated` is more than **3 days** old, it goes into "hide" (event 3: **flag 512**, plus deletion of its shortcuts and widgets).
  - If it already has 512 and `last_updated` is more than **14 days** old, it goes into "remove" (event 4, `QLqAXoLAd4HodeL6sb4TYNDSj`: the App, Shortcut and Widget rows are deleted, which removes them from feeds).
  - **This is what will happen to private entries if Private Space is ever deleted**, and it matches user 12's rows, which are at 256 now.
- `PROFILE_REMOVED` itself only refreshes the registry's map.
- **Unverified**: whether `getActivityList(null, privateUser)` returns the activities, returns an empty list, or throws while Private Space is locked. If it returns an empty list, this reconciliation would start ageing private rows (256 → 512 → purge) while locked. Niagara's own Private Space support (flag 1024 and the locked header) suggests it returns the activities. Test by locking for a while and checking `App.flags` for user 10.

## (f) Toasts

Toasts are fine: there are 51 files with `Toast.makeText`. Examples: `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E` (line 92, `R.string.error_app_not_available`) and `oYIYWo38q4RZRRlamGCdHzBu.qxwCegY8aElo6DGrOJdOOl3e` (`error_unknown`). A module can show its own Toast from Niagara's process using the Application context, on the main looper.

## (g) Obfuscation and stable anchors

- **How obfuscated it is**:
  - Classes are repackaged into `b` with long random names, and members come from a dictionary (`S6IAQCLljUFq4a51beP6ri`, `QCgZq9Gn9a7N1ia2hQxoHljG7Fg8`, … reused across classes). Expect every name to change between versions.
  - Kotlin `@Metadata` is stripped. Code is in one 10.8 MB `classes.dex` plus a tiny `classes2.dex`.
  - **Strings are not encrypted.** Enum constant names survive, as do Kotlin property and function reference names, for example `"shouldBeHiddenEverywhere"`, `"onDetailsClick(Lbitpit/launcher/details/DetailsItem;…)"`, `"makeUsageStatsIntent"`, `"expandList"` and `"shouldHidePrivateSpace"`.
- **DexKit anchors** for fingerprinting by usage:
  - **Profile enum**: strings `"PRIMARY"`, `"WORK"`, `"PRIVATE_SPACE"`.
  - **Profile registry**: `"android.os.usertype.profile.PRIVATE"`, `"android.os.usertype.profile.MANAGED"`, `"Can't set quiet mode for primary user"`, `"Android version below Android 9"`; calls to `LauncherApps.getLauncherUserInfo` and `LauncherUserInfo.getUserType`.
  - **Item base class**: `"Ii(l=***, i="`, `"SubId of "`, `" is not persistable because isPersistable is false."`, `"makeUsageStatsIntent"`. The **isPrivateSpace field** is the second boolean the constructor assigns from a comparison with the `PRIVATE_SPACE` enum constant; the first is isPrimary.
  - **Options enum**: `"favorites_add"`, `"edit_pop_up_expanded"`, `"edit_parent_pop_up"`, `"blacklist_add_advanced"`, `"launcher_pro_screen"`.
  - **Options gate method**: signature `(ItemBase, ArrayList)V`, reads the isPrivateSpace field, uses the string `"bitpit.launcher.key.SHOW_MOST_USED_APPS"`, and references the enum constants INSTALL, REMOVE_UNINSTALLED, NO_UNINSTALL, CALENDAR_LAUNCH_TIP, FAVORITES_ADD and ADD_TO_CATEGORY.
  - **Picker filter**: every method with signature `(ItemBase)Z` that reads the isPrivateSpace field. The favorites editor is found by `"select_favorites"`. The pop-up editor is found by the resource ids of `pop_up_edit`, `specific_app_shortcuts` and `app_options_edit_sesame_shortcuts`; resolve them at runtime with `Resources.getIdentifier` and match with `usingNumbers`.
  - **Sync and DB**: `"CheckAppsChangedTask: upsert="`, `"onPackageRemoved: "`, `"onPackagesUnavailable: "`, `"FeedEntity(type="`, plus the Room SQL strings, for example `"SELECT * FROM App\n        WHERE package_name = (?)\n            AND user = (?)"`, `"INSERT OR REPLACE INTO \`Feed\`"`, `"SELECT item_id AS id, item_type AS type FROM Feed"`.
  - **All-apps section**: `"private_space"` together with `R.string.private_space`.

## What this means for the design

The restriction lives entirely in the UI. Storage, loading, feeds, rendering and launching already handle private apps, which is why the hand-edited DB rows work. A module only needs to bypass the gate in two places:

1. The long-press options builder (`F5CB…S6IAQ…`).
2. The picker filter `gYACuz9hbjk08FgXc` in `HdxOm26AKWF` and in the override in `tNjvAAlk5hkIdUHaZLEjCI`. The favorites editor, folder editor, pop-up editor and Niagara-button pickers all use these.

**Do not flip `JivfzRelv6lvjdmUmyDxa` globally.** The all-apps Private Space section and search hiding depend on it. One way is to clear it only for the duration of those calls and then restore it. The field is `final`, so this relies on reflection being allowed to write final instance fields on ART, which is unverified here. The other way is to post-process the results: append the menu options, or override the filter's return value only for private apps.

## Home-screen tips (for the "unsupported Niagara version" notice)

Niagara's dismissable home-screen tips can be reused, but only by borrowing an existing tip type. All classes are in `/tmp/niagara-src/sources/b/` (Niagara 1.16.32).

- **Tip types** are the closed enum `aYQPXopnDPssiw7urjmwj5LOe2hSB` (59 constants with surviving names, e.g. `NOTIFICATION_NO_PERMISSION("notification_no_permission")`).
- **Providers** implement `YFGL91tb6uHEfmAWFq2ta`: one method answers "should show", the other builds the tip.
- **The tip manager** `kRCKGSQBLvaaeh63O` (a Koin singleton) holds in-memory (type, provider) pairs. `S6IAQCLljUFq4a51beP6ri(type, provider)` adds or removes a tip by type, and `J3yyuGPGpjCnaUPw5BZwjWSwvdQh()` re-checks every registered tip.
- **The home RecyclerView adapter** `MOctxdZAQ6hZ` (around lines 650–656) listens to the manager. Tip items are `q0fgwnNQPwpk1fZ3hQi` (`toString` `"Thsi{tt="`, view type 42, layout `home_tile_tip`). Swiping dismisses and tapping clicks.
- **The factory** `WgxmQrJ0PRm.S6IAQCLljUFq4a51beP6ri(factory, ctx, msgRes, CharSequence msg, btnRes, iconRes, onDismiss, onClick, mask)` takes free text with mask 210. NEW_YEAR_STATISTICS uses it that way (`rwhfWbimunI4v8fwG:40`).
- **Dismissal** isn't stored centrally; each provider keeps its own prefs key.
- **Hook plan:**
  1. Capture the manager, from its constructor or the `S6IAQ…(type, provider)` call made by startup in `CJZBqpf5fEMlLWhfpY`.
  2. On the main thread, register `XIAOMI_FULL_SCREEN_GESTURES_MIUI_12` with a `java.lang.reflect.Proxy` provider. Its owner, `Uhh4ZcYUUSmcTp:178`, only refreshes that type on MIUI 12+.
  3. Build the tip with the factory, resolving `button_dismiss` and `ic_ip_act_info` at runtime.
  4. Dispatch Proxy calls by return type, because method names change between versions.
- **Risks:**
  - The manager matches tips by type only, so the real owner could remove ours.
  - Click and dismiss send a consent-gated analytics event with the borrowed type's key.
  - The mask value may shift between versions.
- **DexKit anchors:** the strings `"xiaomi_full_screen_gestures_miui_12"`, `"terms_migration"`, `"Thsi{tt="` and `"bitpit.launcher.key.ONBOARDING_CHOOSE_FEATURES_TIP_DISMISSED"` (used by provider `PSm3GreNHezd2mFmiOoHri1u`, whose constructor takes the manager); the factory's signature (static, returns the tip class, has a CharSequence parameter); and the resource `home_tile_tip`.
- **Fallbacks:**
  - Niagara's own dialog `hjSzsYhD7GJxr0IoZ8ztrSGH.qxwCegY8aElo6DGrOJdOOl3e`, with builder `mcZgStaicQbyH99InXxEbNZjK6vgM`. It's more fragile: it runs through coroutines and takes default-argument masks.
  - A plain Toast. Niagara has no Snackbar.
