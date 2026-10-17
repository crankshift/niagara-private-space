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

## Locked-state UI hooks

Checked 2026-10-17 against the same 1.16.32 / 1638 tree. Framework behaviour is taken from AOSP `main` (fetched from android.googlesource.com into `/tmp/aosp-ref/`), not from the phone's LineageOS 24 build. Lines marked "AOSP" or "unverified" need a check on the device.

### Building blocks shared by all five items

- **How to tell a locked private item.** Don't use `shouldBeHiddenEverywhere` (`U2m3eJXDAz.InBLWr22CpY()`) on its own. It means `isPrivateSpace && !isActive`, and an App is inactive whenever any of flags 128, 256, 1024 or 8192 is set. On this phone most private apps have flag 128 (suspended by root), so the getter is true for them **even while Private Space is unlocked**. Use `item.JivfzRelv6lvjdmUmyDxa && privateProfileInQuietMode` instead. The second part is `UserManager.isQuietModeEnabled(privateUser)` or Niagara's `GIMQ3HGidL8HUz6rU0DZZV5VKZ2gf.MSIC9osuP4uEvtwoX8Ls()`. For App items, `(AAqdJflv6N9m3Mj.MUf8FpkRe6c & 1024) != 0` gives the same answer; `MUf8FpkRe6c` is the flags field that `cNZSwVhgw3bj87Y()` masks with 9600.
- **Items are immutable snapshots.** The fields of `AAqdJflv6N9m3Mj` are final. Locking or unlocking runs `hKms7hC9dI3Pvae`, which sets or clears flag 1024 and so produces new item instances.
  - Every feed swaps the new instances in: `kwpCNXnI9hAtHMzdAza1dSx.S6IAQCLljUFq4a51beP6ri(Set,Set,Set)`, line 57.
  - The home page receives the changed set in `MOctxdZAQ6hZ.S6IAQCLljUFq4a51beP6ri(Set,Set,Set)`, line 312.
  - Items are compared by id (`U2m3eJXDAz.equals`), not by instance.

  As a result, a filter applied while rendering runs again on every lock change, with no extra wiring.
- **Never filter the feed objects; filter only the rows that get rendered.** Any edit writes the feed contents back to the `Feed` table (`jITGD5xX7idP.oxtZuOAT85CRE5mHS8QhcvJYV` → `bscGd6DHAcw6NpYRID6mLD0P`). If private items were removed from a `kwpCNX…` list, their rows would be deleted for good.
- **How rows are built.** Every list (home, all apps, pop-ups) is a RecyclerView made of Views, not Compose.
  - Rows extend `B9zGC8TUdjpfkuyD` (`toString` `"Hsi(v=ViewType(value=…"`). On a row, `oPDb6Z177Hc5VQA2DsMW4sume(holder)` binds, `w2Vmv4h09bCLYbaCMjXej4UT8ig(launcher, holder, view)` handles a click, and `LCedKJPufl5CNh(…, holder, view)` handles a long press.
  - One view-holder factory serves every list: `ubmKlkaTNDK.S6IAQCLljUFq4a51beP6ri(viewType, parent, …)`.
  - App rows are `iCGfF43vNUxiruOZg3ze` (`toString` `"Iihsi(i="`); their field `InBLWr22CpY` holds the item.
  - The `launcher` passed to click methods is the `ZFV8X6OyWGv` instance.

### 1. Hide locked private apps from favorites

**Verdict: straightforward.**

- **Where the list is built.**
  - The home page is `MOctxdZAQ6hZ`. It extends the list-page base `gn0reobn9PUJUfMhB5UWke4gAmKv` and watches feeds `{4, 6, 9, 1, 7, 10, 11, 5}` (line 55).
  - Its row list `BAMV7IJGaZ7NApujltQtFHXDR` is built from scratch by `mGz9jlkOfqfyxUtEPeq2xbdIN(boolean)` (line 584) and updated item by item by the method `mNpUVZNXiGnHL2PBGAfwsz3M(Collection)` (line 660).
  - Both loop over three section objects. They are held in the field of the same name, `mNpUVZNXiGnHL2PBGAfwsz3M`, and share the base class `lz52wt20HhAYu7RtBmmeYY`.
  - For each item they call `pTm4A75ae4760qpkoAJylH5XGIQ7(section, item)` (line 862), which calls `section.JYS5UmjqF9G0tu10FGEt(item)`. If that returns null, the full build skips the row and the incremental update removes it.
  - The three sections:
    - `AE2p6j3B0dXc1aDntWXLI` is the media section (row kind 10).
    - **`ERSwXVDPQC31V89EIE` is favorites and wraps feed 4.** Its `JYS5UmjqF9G0tu10FGEt(item)` (line 15) logs `"requestHsi: %s index: %s"`. It returns null if the item isn't in feed 4. Otherwise it returns `new JHHu6NDaJMWholqg7b(0, 11, feedIndex, item, …)`: view type 0 (`home_tile_default`), row kind 11, sorted by the item's index in the feed.
    - `rb6tIkUr06qHHxyZ3roP2299jJq0` is most-used suggestions (row kind 13): feed 6 minus feed 4, at most 8 rows.
- **How locked private apps look today.** This comes from the code; I haven't seen it on the device.
  - They show as ordinary favorites rows. The label is unchanged and the **icon is greyed out**.
  - The icon path is `AAqdJflv6N9m3Mj.gb8Ti3xtLlUZ7FMQBPq` → `UFrPni1ntXYKJaAguWIdelrJ8vVFw.S6IAQCLljUFq4a51beP6ri`. At line 59 that calls `drawable.S6IAQCLljUFq4a51beP6ri(item.cNZSwVhgw3bj87Y())`.
  - `i6mVpHINrC5t1z4gKLMJtV.S6IAQCLljUFq4a51beP6ri(false)` applies the ColorMatrix from `EllHuScbQ8oh6drtuqd9fH5U` case 10: saturation 0 and RGB scaled by 0.5.
  - I found no dimming of the label in the row binder (`iCGfF43vNUxiruOZg3ze.oPDb6Z177Hc5VQA2DsMW4sume`).
  - Tapping still tries to launch the app (see item 4).
  - Root suspension (flag 128) greys this phone's private apps even while Private Space is unlocked.
- **Hook: an after-hook on `ERSwXVDPQC31V89EIE.JYS5UmjqF9G0tu10FGEt(item)`.** If the item is a locked private item, set the result to null.
  - This covers the first build, incremental updates and lock/unlock changes.
  - It touches neither the favorites editor nor the DB.
  - Folders are Shortcut items owned by the primary user, so they are never hidden.
  - Gaps in the feed indexes are harmless because rows are sorted with the comparator `BMbeto36oYbZk`.
- **DexKit anchors:** the method with `usingStrings("requestHsi: %s index: %s")`, which is the only one in the dex. Its parameter type is the item base class. Its return type is the home row class `JHHu6NDaJMWholqg7b`.

### 2. Folders and pop-ups: hide private apps and add an unlock row

**Verdict: possible but fragile.** Filtering the rows is simple. The extra row has to borrow an existing row class, because a module can't subclass Niagara's obfuscated row base from its own class loader.

- **Folders and pop-ups share one implementation.**
  - Folders are `uDqjucuiv9L8` items of type 3 (`ULNuaNwRhnIuhgDeV52bYgCY7E`, line 184). Both they and app pop-ups open through the static method `m286sjorFJB.mZcY0deygk6u3(manager, host, launcher, row, view)` (line 29).
  - That method builds `gcZiy9TwlTwZ`, the only pop-up controller. It sets `q6GDDRWaiG40m = "popup"` and contains the error string `"Popups can only be shown inside PopupDialogHosts"`.
  - The views are bound by `PFJK7adz8XdunnDNZfa` from layout `popup_dialog`, whose `R.id.recycler_view` is a RecyclerView. It is not Compose.
- **How the content is built.**
  - `PskLZXQFY5DQLPMH.x84QUk1KZDQn9Oaki3QtLcQFH19KN(host, cont)` (line 744, a suspend function) loads the items:
    - Folders and action items: the host's type-1 feed.
    - App hosts without flag 4096: the app's deep shortcuts as well.

    The result is stored in `gcZiy9TwlTwZ.Nwc69xS7qC5SA1NCRAW9m3R`.
  - **`gcZiy9TwlTwZ.myg4xWl4jc7ydenlyX4YW()`** (line 597, signature `()ArrayList`) turns that collection into rows: either `hIcfUC0Zhx` or its subclass `kHgU41mpJYiavM1clRp2`, which also shows a notification. `hIcfUC0Zhx` uses view type 47 (`popup_tile_default`) and the sort key `Mc0hzz07iIHSIkCTZvQ9HEWMCC7uq(3, index, 4, 0)`.
  - The rows go to `WqSpbqcSfgIDD5GcS2VOTd.Sg107GnZGk9KhAtv(widgetRow, itemRows, notificationRows)` (line 59), which passes them to a sorted adapter. If everything is empty, it shows the fixed empty-state row instead (line 74).
  - `myg4x…()` is the only row builder, whichever way the pop-up is filled:
    - first display (lines 303–465);
    - a notification refresh (`LCedKJPufl5CNh`);
    - a feed refresh: `dNFMiwZEuo9Ffgr4ZVOa` case 5, started from `gcZiy9TwlTwZ.oxtZuOAT85CRE5mHS8QhcvJYV` when the host's type-1 feed changes.
- **Today,** locked private apps show as ordinary pop-up rows with greyed icons, through the same icon path as favorites. Tapping them tries to launch them (see item 4).
- **Hook 1, the filter: an after-hook on `gcZiy9TwlTwZ.myg4xWl4jc7ydenlyX4YW()`.** Remove rows whose `iCGfF43vNUxiruOZg3ze.InBLWr22CpY` is a locked private item.
- **Hook 2, the extra row.** If any rows were removed, append one marker row.
  - **The donor class is `SC5C9fM6MDiATyqqWpPGHl6A`,** the pop-up's own empty-state row.
    - Its constructor is `(itemBase host, kTFwF1y7rMXtRP5GeY9zDF8hak8o notifRepo)`.
    - It uses view type 50 (`popup_tile_empty_state`): a centred `Text.Secondary` TextView `header_text_view` on a ripple background. Its holder is `LyL6014v1HRMU`, whose field `rI081ztdGp2Ah` is that TextView.
    - It has the fixed id `412316860418L` and the sort key `Mc0hzz07…(4, 0, 6, 0)`. So it sorts after the app rows, which are in section 3.
  - Build it with `(gcZiy.mGe4G6pRBJSs1GrIePH /* the host */, gcZiy.E8kxVwxqnFT88c3v)`, and keep the instance in a weak identity set.
  - **Binding:** hook `SC5C9….oPDb6Z177Hc5VQA2DsMW4sume(holder)`. For marker instances, set the text ("Unlock Private Space to see your apps") and skip the original method.
  - **Clicks:** hook `SC5C9….w2Vmv4h09bCLYbaCMjXej4UT8ig(launcher, holder, view)`. For marker instances, start the unlock (item 3).
    - Clicks do reach this method: `ubmKlkaTNDK` registers click listeners for view type 50, and `yoVdwFMC8PI5JR` (the base class of `WqSpbq…`) passes the click on to `row.w2Vmv4h…(launcher, …)` (in `z980KXEmDIlcwTHyeDEpOq1lRCKN`, case 5).
  - **Long press:** on the donor row, `LCedKJPufl5CNh` opens the pop-up editor for the host. Leave it as it is or suppress it.
  - The marker can't clash with Niagara's own empty-state row, which is only shown when the list is empty; the marker makes the list non-empty.
- **After unlocking (inferred).**
  - `hKms7…` changes the items, the host's type-1 feed swaps them in (`kwpCNX….ursJAofciE(list, true)`), and `gcZiy.oxtZuOAT85…` reloads the pop-up through case 5.
  - The filter now lets the apps through and adds no marker.
  - **Unverified:** whether the pop-up stays open while ConfirmDeviceCredential covers HomeActivity. If it closes, the user simply reopens it.
- **Why it is fragile.**
  - In the jadx output, the variant field `InBLWr22CpY` of `SC5C9…` is set to 0 for both constructors, which looks like a decompiler artifact. On the device, check that an instance built with the two-argument constructor takes the empty-state branch, or always skip the original bind method for markers.
  - The sort-key class and the fixed id are implementation details that can change.
  - Message-hub pop-ups (action 34) use a different empty-state row: `S0bE6peMMTjNrmFr5bN`, view type 80, with the same id 412316860418L.
- **DexKit anchors.**
  - **Controller:** the class with `usingStrings("Popups can only be shown inside PopupDialogHosts")` (it also uses `"popup"`). In it, the method with no parameters that returns `java.util.ArrayList`.
  - **Donor row:** the class with a method that has `usingNumbers(412316860418L)` and also uses the resource id of `R.string.pop_up_empty_state`, resolved at runtime. `S0bE6…` shares the number but not the string. The class has a two-parameter constructor whose first parameter is the item base class.
  - **Row base methods, by signature:** the abstract bind `(holderBase)V` and the click `(launcherIface, holderBase, View)V`.

### 3. The unlock call

**Verdict: straightforward. Call the framework directly; Niagara's wrapper can be reused if needed.**

- **Niagara's chain.**
  - When Private Space is locked, `wc9ii3Nltew959oWw` adds the section header to the all-apps list. The header is action item 30 (`L2xwse7AF7diDQfniAObAGTfxccU`), with analytics key `"private_space_unlock"`, label `R.string.unlock_private_space` and icon `ic_ip_act_lock_open`. Action 31 is "lock", 32 is "settings" and 33 is "install".
  - Its click handler, `L2xw….ULNuaNwRhnIuhgDeV52bYgCY7E`, calls **`ZFV8X6OyWGv.mZcY0deygk6u3(Boolean.FALSE, PRIVATE_SPACE)`** (line 156), a plain `void` method that launches a coroutine. From there:
    1. `Doeh9A5xDrvo5HMupKmJsn`, case 7;
    2. `MQsblrH1MD9ZYSx.gBppIyS9iwNrDwNPE9sGWiCc(registry, errUi, errReporter, Boolean, kind, cont)`, which catches `SetQuietModeException` and shows Niagara's error UI;
    3. `GIMQ3HGidL8HUz6rU0DZZV5VKZ2gf.myg4xWl4jc7ydenlyX4YW(Boolean, kind, cont)`, a suspend function that throws for the primary user and below Android 9;
    4. off the main dispatcher (inferred), the lambda `bhy75HKiWNVJcn4MG` calls **`UserManager.requestQuietModeEnabled(false, user)`** for every profile of kind PRIVATE_SPACE.
  - The search entry "Unlock private space" (`uTRxllORq4JEtizXzxiQD4S1MkbQU`) calls the same `MQsbl…` wrapper directly (`sRV210pgJkOc6`, case 4).
- **After the call, Niagara does nothing itself.** It ignores the return value and doesn't wait.
  - The UI updates when `PROFILE_AVAILABLE` arrives: `c1f9HeekFzJZbklkcB2` → `hKms7…` clears flag 1024 → the lists rebuild.
  - The search entry adds one step. Right after the call it scrolls the all-apps list to its last row (action 21, settings), via `bFYO3uFYGyPedDd33U5ZmF3rZ8uYX.mZcY0deygk6u3(item, list)`. That is where the private section will appear.
- **Framework rules (AOSP `UserManagerService`).**
  - Niagara has neither `MODIFY_QUIET_MODE` nor `MANAGE_USERS`. `requestQuietModeEnabled(false, privateUser)` is allowed only because Niagara is the foreground default launcher (`ensureCanModifyQuietMode`). So make the call while HomeActivity is resumed, for example from a click.
  - For a profile with `authAlwaysRequiredToDisableQuietMode` (true here), it starts ConfirmDeviceCredential and **returns false**. The profile starts after the user authenticates.
  - The overload that takes an `IntentSender target`, which would launch something after unlocking, requires `MANAGE_USERS`. Niagara can't use it, so "unlock, then launch" has to be done by the module.
- **Recommendation: call the framework directly.**
  - Find the private profile the way Niagara does: go through `UserManager.getUserProfiles()` and pick the profile for which `LauncherApps.getLauncherUserInfo(u).getUserType()` is `"android.os.usertype.profile.PRIVATE"`.
  - Then call `requestQuietModeEnabled(false, user)` and catch `SecurityException`. It is one binder call, so the main thread is fine.
  - That is three lines with no obfuscated dependencies.
  - Reusing `ZFV8X6OyWGv.mZcY0deygk6u3` is the fallback, for when the module already holds the launcher object (every click hook receives it) and wants Niagara's error dialog. Its enum argument is `hE1r6UMnZ5lo8vIKm6.Maiu1xWivs2Pl`; find the constant by its name, `"PRIVATE_SPACE"`.
- **DexKit anchors for the wrapper:** the class with `usingStrings("open_action_item")` (it also uses `"bitpit.launcher.key.HIDE_PRIVATE_SPACE"`); in it, the method taking `(java.lang.Boolean, <profile enum>)` and returning void.

### 4. Niagara button (feeds 10 and 11), and any tap on a locked private app

**Verdict: straightforward.** It takes one launch hook, plus a pending-launch helper in the module.

- **What happens today.**
  - **Tap:** `MOctxdZAQ6hZ.Sg107GnZGk9KhAtv(View)` (line 342) reads the first item of feed 10 and calls `U2m3eJXDAz.xXleTt96TbxvixkKaSOY8aqVQs5L(item, launcher, view, null, 12)`, which calls `item.ULNuaNwRhnIuhgDeV52bYgCY7E(…)`.
  - **Swipe up:** `MOctxdZAQ6hZ.iFQzqlZm1M(FloatingActionButton, float)` (line 518) does the same with feed 11 and the same static helper. It first checks for Pro and otherwise shows the dialog `"niagara_button_swipe_pro"`.
  - Neither path checks whether the app is active or locked.
  - The button shows an icon-pack glyph (`w37b4qe5Lc1SRS0P6oRn.mZcY0deygk6u3`), not the greyed app icon.
  - For an App, `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E` (line 73) calls `LauncherApps.startMainActivity(component, privateUser, bounds, opts)` (line 86). Only if that throws does it show the toast `error_app_not_available` (line 92).
  - What the framework does with it (AOSP):
    - `LauncherAppsService.startActivityAsUser` doesn't check quiet mode.
    - `ActivityStartInterceptor.interceptQuietProfileIfNeeded` redirects the launch to `UnlaunchableAppActivity`.
    - `UnlaunchableAppActivity` **finishes immediately for any profile that isn't a managed (work) profile**; its log message says "called for a non-managed-profile".

    So the tap **silently does nothing**. Unverified on LineageOS 24. Taps on locked private apps in favorites, folders, pop-ups and search behave the same way.
- **Hook: a before-hook on `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E(launcher, view, row, bundle)`.** If `this` is a locked private item:
  1. Start the unlock (item 3).
  2. Remember the component and user from the field `LymwWLwVTddmNDuus7mlek0` (`XVYC3fHMxlmXmjh`: `.S6IAQ…` is the ComponentName, `.QCgZq9…` the UserHandle).
  3. Skip the original method.

  This one hook covers the button, favorites (if they aren't hidden), pop-ups and search.
- **Launching afterwards.**
  - Register a receiver with Niagara's context for `Intent.ACTION_PROFILE_AVAILABLE` (API 35+; Niagara also registers for it, in `aqwafnQ2sdQfHwxEb1`), and match `EXTRA_USER`.
  - When it arrives, call `LauncherApps.startMainActivity(component, user, null, null)` on the main thread.
  - Drop the pending launch after a timeout, on the next `PROFILE_UNAVAILABLE`, or when the user cancels.
  - Niagara is the home app and is back in front after the credential screen, so background-launch limits shouldn't block the launch (unverified).
- **DexKit anchors.**
  - **Launch method:** the only method in the dex that calls `android.content.pm.LauncherApps#startMainActivity` (match with `invokeMethods`).
  - **Button methods,** only needed for behaviour specific to the button: the class with `usingStrings("updateHome with ", "AtAGlanceHsi not found, hsiList size: ")`. The swipe method takes a `FloatingActionButton` parameter and uses `"coin_flipped"`.

### 5. Widgets from private apps

**Verdict: not feasible from inside Niagara.** AOSP's widget service refuses Private Space widget providers to a launcher running in the parent user. It would take an additional hook in system_server, which would be fragile.

- **How Niagara hosts widgets.**
  - **Classes:**
    - `dAdtYU8sqdkZ7XkIoP7wEiGoyKLs extends AppWidgetHost`, with host id **4354**;
    - the host view `bk5t8GFSiLR7An extends AppWidgetHostView`, plus `cbLpEB5YrC678gjFEc`, a placeholder used while the view is deferred;
    - `cD1RRoz9pWprj3r8J8JD1lE48z`, which manages widgets and their storage.
  - **Rows:** rows of the `Widget` table load as `hqnHp1ofHs9LuDUBfLe`. The user comes from the row's serial number, as for Apps. In the flags field `ldxl6vaCmxIjgB1`, 128 means "hosted in a pop-up"; 256 and the low 6 bits are restore states (inferred from the view chooser).
  - **Locking has no effect on Niagara's side.** Widget items never become inactive (the base `cNZSwVhgw3bj87Y()` returns true), and `hKms7…` doesn't touch Widget rows.
  - **Choosing the view:** `Sbh8OzlPn74fRThXc859FSDhQNS3X.FcT8vexLQsFH8TD9PCCur(widget)` (`toString` `"Wvh(i="`) picks the view for a widget:
    - flags & 256: the loading view `D79l4i9CKLQ`;
    - flags & 63: the "Tap to restore" view `XUxPEFyJuS` (layout `widget_could_not_be_restored`);
    - `getAppWidgetInfo(id)` is null, or the provider doesn't match: the loading view **plus a restore coroutine**, `hvUajZaEhulfdFU7hMaAPpH3HtL`, which sets flag 256 and binds the widget again;
    - otherwise: the live `bk5t8…` view from `createView`.
  - **Cleanup:** `cD1RRoz….ursJAofciE()` deletes host widget ids that have no row in the DB.
- **What blocks private widget providers.**
  - **In Niagara,** the picker coroutine `XoYnnUj9fSu` (its default case):
    1. gets the providers from `cD1RRoz….qxwCegY8aElo6DGrOJdOOl3e(null, true)` (line 619), which calls `getInstalledProvidersForProfile` for every profile;
    2. wraps each one in `KO684KCEFfCiDTCrQIufIPRxpJoR`, a class used only by the picker, whose label comes from `PackageManager.getUserBadgedLabel`;
    3. at line 108, skips any provider whose `JivfzRelv6lvjdmUmyDxa` (isPrivateSpace) is set.

    To bypass it, add an after-hook on the `KO684…` constructor that clears that final field on the new instance. This writes a final instance field through reflection, which is still unverified on ART.
  - **The framework blocks it anyway** (AOSP `AppWidgetServiceImpl`).
    - Both `getInstalledProvidersForProfile` and `bindAppWidgetId` require `SecurityPolicy.canAccessProvider(pkg, profileId)`.
    - For a profile other than the caller's, that requires the package to be on `DevicePolicyManagerInternal.getCrossProfileWidgetProviders(profileId)`.
    - `DevicePolicyManagerService` returns an empty list when the profile has no profile owner, and Private Space has none.
    - So the provider list for user 10 comes back empty, and binding fails. The work profile can differ, because Intune may allowlist providers.
    - Lifting the block needs a hook in system_server (`AppWidgetServiceImpl$SecurityPolicy.canAccessProvider`), meaning a second module scope, `android`. That is outside this research.
- **If private widgets were present, this is what locking would do (AOSP).**
  - `onUserStopped` keeps the providers; the code comment says hosts show the widgets masked while the user is stopped.
  - `maskWidgetsViewsLocked` sends the host `work_widget_mask_view`, which shows the app icon and a badge. Tapping it opens `UnlaunchableAppActivity`, which does nothing for a private profile.
  - A placeholder can be built inside Niagara:
    - add an after-hook on `Sbh8….FcT8vexLQsFH8TD9PCCur(widget)`;
    - for a private widget while locked, either lay a clickable TextView over the returned host view, or substitute an `XUxPEFyJuS` and rewrite its text in an after-hook on `XUxPEFyJuS.JYS5UmjqF9G0tu10FGEt(widget)`.
  - The system removes the mask on unlock, so the module only has to remove its overlay on `PROFILE_AVAILABLE`.
  - **Risk (unverified):** after a reboot, Private Space's credential-encrypted storage is locked. If `getAppWidgetInfo` then returns null, Niagara's restore coroutine would start rewriting the private Widget row, and the module would have to stop it.
- **DexKit anchors.**
  - `Sbh8…`: `usingStrings("Widget frame must not have more than one widget view child.")`.
  - Picker coroutine: `usingStrings("org.kustom.widget", "com.weawow")`.
  - Provider class: constructor `(int, AppWidgetProviderInfo, …)` that calls `PackageManager.getUserBadgedLabel`.
  - Host: the class that extends `android.appwidget.AppWidgetHost`.
  - Restore placeholder: the class that uses the resource ids of `widget_could_not_be_restored` and `backup_tap_to_restore`.

### Summary

| Item | Verdict | Hook point | Main DexKit anchor |
|---|---|---|---|
| 1 Favorites | Straightforward | after `ERSwXVDPQC31V89EIE.JYS5UmjqF9G0tu10FGEt(item)` → null | `"requestHsi: %s index: %s"` |
| 2 Folders and pop-ups | Possible but fragile | after `gcZiy9TwlTwZ.myg4xWl4jc7ydenlyX4YW()`, plus bind and click hooks on donor row `SC5C9fM6MDiATyqqWpPGHl6A` | `"Popups can only be shown inside PopupDialogHosts"`; `412316860418L` + `pop_up_empty_state` |
| 3 Unlock | Straightforward | framework `requestQuietModeEnabled(false, user)`; fallback `ZFV8X6OyWGv.mZcY0deygk6u3(FALSE, PRIVATE_SPACE)` | `"open_action_item"` |
| 4 Niagara button and taps | Straightforward | before `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E`, plus a `PROFILE_AVAILABLE` receiver that launches the app | sole caller of `LauncherApps.startMainActivity` |
| 5 Widgets | Not feasible from inside Niagara (framework allowlist) | only with a system_server hook; then an overlay via `Sbh8….FcT8vexLQsFH8TD9PCCur` | `"Widget frame must not have more than one widget view child."` |

**Still unverified:**

- Every AOSP-based claim, on LineageOS 24: the silent no-op when tapping a locked private app, the cross-profile widget allowlist, and widget masking.
- Whether pop-ups survive the credential screen.
- Which branch the two-argument `SC5C9…` constructor takes.
- Whether reflection can write the final field.
- Whether `getAppWidgetInfo` returns null after a reboot while Private Space is locked.

## Bytecode facts for the finder (dexdump of 1638, checked while implementing)

Added 2026-10-17 from `dexdump -d` of `classes.dex` in `/tmp/niagara-apk/1638/base.apk` (Android SDK build-tools 37), because jadx could not decompile two of the readers below. Names are the obfuscated 1638 names.

- **All readers of the item base class's Private Space flag** (`U2m3eJXDAz.JivfzRelv6lvjdmUmyDxa`, a `public final boolean`):

  | Reader | Signature | Role |
  |---|---|---|
  | `U2m3eJXDAz.InBLWr22CpY` | `()Z` | the `shouldBeHiddenEverywhere` getter itself |
  | `F5CBVETNWGKDUskOP3paHecjTS.S6IAQ…` | `(Item, ArrayList)V` | long-press options builder |
  | `HdxOm26AKWF.gYAC…` | `(Item)Z` | picker filter, base |
  | `tNjv…`, `IOoBH…`, `Chs0o…`, `p673N…` `.gYAC…` | `(Item)Z` | picker filter overrides |
  | `gqD7qG2N3MYoNNJ.JtqOZV…` | `(rYumPf…)Z` | most-used suggestions check |
  | `VTeRaLtcXgU.<init>` | constructor | options sheet; the shortcut pin check |
  | `XWa5EoybyzcxRoHWOT1gSM7xX.x84QUk…` | `(Object)Object`, coroutine body | "Add shortcut" screen |
  | `wc9ii3Nltew959oWw.MUf8…` | `(vbj7…)Object`, coroutine body | all-apps list; must keep seeing the flag |
  | `XoYnnUj9fSu.x84QUk…` | coroutine body | widget picker (out of scope) |
  | `VZbTWt3LDLJsFtQocAKcmCr.x84QUk…` | coroutine body | not decompiled; app-sync results dispatch, not a placement path |
  | `sqDBLpb7YUn5.S6IAQ…` | `(Item, FPjPz…)Object` | not decompiled; reads Private Space existence and lock state, then the flag; not a placement path |

  Classifying the readers by **signature** is therefore reliable: `(Item)Z` is a picker filter, `(Item, ArrayList)V` is the options builder, a constructor is the options sheet, and the one `(X)Z` with a non-item parameter is the suggestions check.
- **The item base class has exactly two boolean fields**, both `public final`: `Dm93ilxRylvqICs` (primary user) and `JivfzRelv6lvjdmUmyDxa` (Private Space). Its constructor writes the primary-user flag first. The primary-user flag is read by `Chs0o…` (Leisure Apps), `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E` (the launch method), `XWa5…`, `cq8j2XazriANl`, `XdC4xEWPwtfMXKon8vnBenKHTui32`, `VG6pYj5ff5lD9VROuoTkpZE` and `p673N….WuObUi…`.
- **Row providers on the home page.** `lz52wt20HhAYu7RtBmmeYY` is abstract and has three overrides of `(Item)JHHu6NDaJMWholqg7b` named `JYS5UmjqF9G0tu10FGEt`: `AE2p6j3B0dXc1aDntWXLI` (media), `ERSwXVDPQC31V89EIE` (favorites) and `rb6tIkUr06qHHxyZ3roP2299jJq0` (most-used suggestions). Only the suggestions class and `gqD7q…` share both `"bitpit.launcher.key.SHOW_MOST_USED_APPS"` and `"bitpit.launcher.key.HIGH_USAGE_COUNT_v2"`.
- **Picker family.** The subclasses of `HdxOm26AKWF` are `Chs0o…`, `IOoBH…`, `l53VGFijSISw4` (abstract), `WttjFn…`, `ji6Mq…`, `p673N…`, `qGiFZh…` and, through `l53VGFijSISw4`, `tNjv…`. The base's strings `"sort_apps"` and `"show_hidden_apps"` appear nowhere else.
- **The "Add shortcut" screen check cannot be bracketed, but its items can be changed when they are built.** The check sits inside the coroutine body of `XWa5…`, in a loop over a list built in the same method, so there is no per-item call to wrap. The items in that list are instances of `xN5QNYCoLolalmHhfg19i` (one per shortcut-config activity, built from a `LauncherActivityInfo`) and `JeOzdhYhiDnetlhSKW1fR` (pinned shortcuts from the database). `xN5Q…`'s constructor `(LauncherActivityInfo, int, aWjsinykF66w)` is the only constructor of an item subclass that takes a `LauncherActivityInfo`, and the only place it is called is that coroutine. So an after-hook on that constructor can make an unlocked private app's items look like work apps, with no effect elsewhere. Pinned shortcuts (`JeOzd…`) are not covered: private ones cannot exist yet, since pinning them was blocked. Ticket 04.

## DexKit on the desktop JVM

Checked 2026-10-17 while implementing ticket 01.

- The only DexKit 2.x artifact on Maven Central is `org.luckypray:dexkit` (an AAR). Its native libraries are Android ELF files for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`; there is no macOS library, and no desktop JAR is published (the GitHub project has no release assets either).
- DexKit's source tree does have a desktop target (`dexkit/src/main/cpp/CMakeLists.txt`, with a macOS deployment target). Using it means building a `libdexkit.dylib` from source with CMake and a JDK that ships JNI headers (the Android Studio JBR has none; Gradle's cached Temurin 21 does).
- **Not done:** that build needs compiling code cloned from GitHub, which the agent was not permitted to do unattended. So DexKit has not been run against Niagara on the Mac. Until someone builds the library, the check moves to the phone: the module logs `active, found: …` or `inactive, missing: …` on every start of Niagara.
- **DexKit's `usingFields` order** (read from `Core/dexkit/dex_item.cpp` in the DexKit source): field uses are appended in instruction order, one entry per instruction, without de-duplication, and `FieldData.readers` lists the methods with an `iget*` or `sget*` of the field. The finder relies on both.
- **What was checked instead:** every query the finder makes was emulated over the dex dump above (strings, signatures, field readers), and each matched exactly as intended on 1638.

## Home page sections, as built for ticket 05

Checked 2026-10-17 on the 1638 dexdump and the jadx tree.

- `lz52wt20HhAYu7RtBmmeYY` is the abstract section base. Its three subclasses each override `JYS5UmjqF9G0tu10FGEt(Item)` returning the row type `JHHu6NDaJMWholqg7b`: the media section `AE2p6j3B0dXc1aDntWXLI`, the favorites section `ERSwXVDPQC31V89EIE` and the most-used suggestions section `rb6tIkUr06qHHxyZ3roP2299jJq0`. (A fourth subclass, `VwveaytoBMh7dtuVcOUazw`, has no such method.)
- `MOctxdZAQ6hZ.pTm4A75ae4760qpkoAJylH5XGIQ7(section, item)` calls the section's method and returns null when it does; both the full build and the incremental update skip a null row. So a row provider that answers null leaves the item out of the page, and the stored lists are untouched.
- **The media section is a leak the spec did not list.** `AE2p6…` shows apps from the music-apps feed whenever headphones or a speaker are connected, with no check of the item's state. Once the music-apps picker lists private apps (ticket 03), a locked private music app would appear there. The module therefore filters every section's rows, not just favorites.
- The suggestions section keeps a list rebuilt only when its feeds change (`rb6tI….qxwCegY…`), so a private app already in the suggestions feed stays there when Private Space locks. Filtering its rows covers that.
- **Known leak, not fixed.** The favorites and folder editors draw their current selection straight from the stored feed with no lock check (`ho3Z3OuW2p`, around the code that renders the selection; `HdxOm26AKWF` seeds the selection unfiltered, which is why private placements survive editing). So while Private Space is locked, those editors list the private apps already placed. Removing them from the selection would delete the placements on save. A hook on the row-adding step (the static method `HdxOm26AKWF.HdbD73X0ZC0vBBjEb96BB`, called for each selected and candidate app) might skip the row for a locked private app without touching the selection; not tried.
- **Lock and the missing-app ageing.** The "Unverified" note under (e) about `getActivityList` while locked was settled by a short lock on 2026-10-17, according to the spec; a lock of days is not covered (see the optional step in `docs/device-check.md`).
- **Suggestions refill.** Hiding locked favorites frees room under the suggestion section's cap of eight rows, so other (non-private) suggestions can appear while Private Space is locked. Harmless.

## Launch and unlock, as built for ticket 06

Checked 2026-10-17 on the 1638 dexdump and the jadx tree, and against the SDK's `android.jar` (API 37).

- **The launch method** is `AAqdJflv6N9m3Mj.ULNuaNwRhnIuhgDeV52bYgCY7E(launcher, View, row, Bundle)V`, the only caller of `LauncherApps.startMainActivity` in the dex. Before it launches it records the launch with `YWlkXh9CRmXR4Z.S6IAQ(item.FcT8…)` and afterwards calls `ZFV8X6OyWGv.S6IAQ(…)`; both are skipped when the module takes over a tap on a locked private app.
- **What the launch method reads.** The component name is the one `ComponentName` field it reads (`XVYC3fHMxlmXmjh.S6IAQ…`, in a holder of component and user), and the holder is the field of `AAqdJflv6N9m3Mj` of that holder's type (`LymwWLwVTddmNDuus7mlek0`). Each is the only match in the dexdump.
- **Two broadcasts, not one.** `Intent.ACTION_PROFILE_AVAILABLE` ("quiet mode ended") and `Intent.ACTION_PROFILE_ACCESSIBLE` ("running and unlocked") both exist in the SDK, with `ACTION_PROFILE_UNAVAILABLE` and `ACTION_PROFILE_INACCESSIBLE` as their opposites. The research above named only `PROFILE_AVAILABLE`, which Niagara listens to. The module opens the pending app on `PROFILE_ACCESSIBLE`, because only then can the private user's apps start. AOSP `UserManagerService.broadcastProfileAvailabilityChanges` sends the availability pair (lines 1789–1817 of the copy in `/tmp/aosp-ref`); where the accessibility pair is sent was not checked in source, and which of the two Android delivers on LineageOS 24 is unverified.

