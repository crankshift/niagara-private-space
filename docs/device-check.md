# Device check

The on-device check proves the module works against the Niagara build that is actually installed. It covers what unit tests cannot: Niagara's real menus and pickers, locking and unlocking Private Space, and rebooting. The check is done when every step below passes on the test phone. Then record the Niagara version you tested against in `README.md`.

**Always ask the user before running it.** It locks and unlocks Private Space, restarts Niagara and, in later steps, reads Niagara's database as root. It is optional.

Test phone: rooted with KernelSU and running Vector. Private Space is user 10 and the work profile is user 11. Niagara is `bitpit.launcher`:

```sh
P=bitpit.launcher
M=io.github.crankshift.niagaraprivatespace
VECTOR="su -c /data/adb/modules/zygisk_vector/cli"   # run through: adb shell "$VECTOR …"
```

## Set up

1. **Connect.** The phone appears in `adb devices`. If it is listed twice (Wi-Fi debugging can do that), set `ANDROID_SERIAL` to one of the two; Gradle's `installDebug` honours it too.
2. **Note Niagara's version.**
   ```sh
   adb shell dumpsys package $P | grep -E 'versionName|versionCode'
   ```
3. **Install, enable and restart Niagara.**
   ```sh
   ./gradlew installDebug
   adb shell "$VECTOR modules enable $M"
   adb shell "$VECTOR scope add $M $P/0"   # first install only
   adb shell am force-stop $P
   adb shell input keyevent KEYCODE_HOME
   ```
   A release build signed with the module's release key cannot be installed over a debug build (or the reverse); uninstall the old one first.
4. **Read the log.** Run `adb logcat -c` before the restart in step 3, then `adb logcat -d -s NiagaraPrivateSpace` (the module writes it through libxposed's `log`; on the test phone logcat's 256 KiB main buffer had dropped it within about six minutes, so read it straight after the restart. `vector-cli log cat` showed the module loading but not this line, checked once; whether the Vector manager's module log holds it is unverified). It shows one line like `Niagara 1.16.32 (1638): active, found: …(PICKER_FILTER_OVERRIDES=3); N hooks installed`. On Niagara 1.16.32 the expected counts are listed under "Expected log line" at the end of this file. A line like `Niagara <version>: inactive, missing: …` means a fingerprint no longer matches this Niagara version: stop, record the missing parts, and fix the finder. `finding the … failed` lines above it say why.

Only tap the screen with `adb shell input tap` after confirming which app is in front (`adb shell dumpsys activity activities | grep -m1 topResumedActivity`).

## Add a private app to favorites from its long-press menu

Private Space must be unlocked.

1. Open the app list and scroll to the Private Space section at the bottom.
2. Long-press a private app that is not yet in favorites. The menu offers the same options as for a work app, including **Favorite**, **Edit pop-up** and **Add to category**. (**Set as music app** appears only for apps Niagara considers music-capable.)
3. Choose **Favorite**. The app appears in favorites with Android's Private Space badge.
4. Tap it in favorites. It opens normally.
5. **Stock behaviour is intact:** the Private Space section is still at the bottom of the app list, the private app is not duplicated in the main list, and a personal app's long-press menu is unchanged.

## Private apps in every app picker

Private Space must be unlocked. Menu names follow Niagara 1.16.32; the path to a picker can differ between versions.

1. **Favorites editor.** Open the favorites editor ("Your favorites"). Private apps are listed with Android's Private Space badge, alongside personal and work apps. Select one and confirm. It appears in favorites.
2. **Folder editor, with the hand-made folders.** Open the editor of a folder that already holds private apps (for example "Ciklum"). Its private apps are shown as selected. Select one more private app, save, and reopen the folder. Every app that was there is still there, and the new one too. To be sure nothing was dropped, compare the folder's rows in Niagara's database as root before and after (the `Feed` table, rows whose `id` is the folder's packed id). Niagara's database file is probably `launcher.db` in `/data/data/bitpit.launcher/databases/` (the name appears in Niagara's backup code); check with `ls` first.
3. **Pop-up editor.** Long-press a favorite, choose **Edit pop-up**, and add a private app. It shows in the pop-up. Remove it again and confirm the pop-up behaves as before.
4. **Niagara button.** In Niagara's settings, set the Niagara button's tap action to a private app, then the swipe-up action. Both pickers list private apps. Put your previous actions back afterwards.
5. **Music apps.** The music-apps picker lists private apps.
6. **Hide apps.** The hide-apps picker lists private apps. Hide one, check that it leaves the Private Space section of the app list, then show it again.
7. **Leisure Apps.** Add an app to the **Leisure Apps** category. The picker lists private apps and personal apps, and still does not list work apps.
8. **Notification summary.** The picker for the notification summary's apps lists private apps.
9. **Widget picker (unchanged).** The widget picker still offers no widgets from private apps.
10. **Private Space section (unchanged).** The Private Space section at the bottom of the app list still holds the private apps and none of them shows in the main list.

Steps that change data (1, 2, 3, 4, 6 and 7 above) should be undone afterwards: remove the app you added from favorites, restore the folder to its earlier contents, set the Niagara button actions back, show the app you hid, and take the app out of Leisure Apps. Steps 6 and 7 name where the result should appear; if Niagara 1.16.32 puts it somewhere else, record that instead of treating it as a failure.

## Suggestions and app shortcuts

Private Space must be unlocked unless a step says otherwise.

1. **Suggestions.** With "most used apps" on, use a private app often enough for Niagara to suggest it. It appears below favorites. Long-press it: the menu offers **Stop suggesting**. Choose it, and the app leaves the suggestions. Long-press the app in the app list and choose **Suggest again**; it returns.
2. **App shortcuts, pinned to favorites.** Long-press a private app that has app shortcuts (a messaging app's "New chat", for example). Long-press one of its shortcuts and pin it to favorites. It appears in favorites and opens the right screen of the private copy.
3. **App shortcuts, pinned to the app list.** Do the same and pin a shortcut to the app list. It appears in the app list. If Android refuses to pin a shortcut from a private profile, record exactly what happens (an error, nothing, a toast) in the research notes and the spec; that is an answer, not a failure of the module.
4. **Add shortcut screen.** Open Niagara's **Add shortcut** screen. It lists private apps' shortcut entries next to the others. Pick one of a private app's entries and confirm that what it adds works (this is where the module changes a flag for good; see ADR 0001).
5. **Locked.** Lock Private Space and go back to Niagara's main view. No private app is suggested below favorites. Open the **Add shortcut** screen: no private app's entries are listed.

Steps 1, 2, 3 change what Niagara shows: undo them afterwards (suggest the app again if you stopped it, unpin the shortcuts).

## Hiding locked private apps from the home page

This section reads Niagara's database as root and locks, unlocks and reboots the phone, so it needs your go-ahead. The database is probably `/data/data/bitpit.launcher/databases/launcher.db` (check with `ls`). The earlier research read the database with read-only SELECTs, so some `sqlite3` binary was reachable through root; if none is, `adb pull` the copied files from `/data/local/tmp/nps/` and read them on the Mac. The table and column names are those in `docs/research/niagara-internals.md` (`Feed`: `id`, `type`, `item_index`, `item_id`, `item_type`; `App`: `id`, `package_name`, `user`, `flags`).

```sh
DB=/data/data/bitpit.launcher/databases/launcher.db
# Read a copy, so root does not leave -wal or -shm files next to Niagara's live database.
snapshot() {
  adb shell "su -c 'mkdir -p /data/local/tmp/nps && cp -f $DB $DB-wal $DB-shm /data/local/tmp/nps/ 2>/dev/null; chmod -R a+r /data/local/tmp/nps'"
  adb shell "sqlite3 -readonly /data/local/tmp/nps/launcher.db 'SELECT id, type, item_index, item_id, item_type FROM Feed ORDER BY id, type, item_index; SELECT id, package_name, user, flags FROM App WHERE user = 10 ORDER BY id;'"
}
```

1. **Before.** Favorites hold at least one private app, one of them with a pop-up attached (ticket 01). Run `snapshot > before.txt` and note the private apps' positions on the home page.
2. **Lock.** Lock Private Space. The private favorites disappear, together with the pop-up attached to one of them. The other favorites keep their order. No private app is among the suggestions below favorites, and none appears in the media section (play audio through headphones with a private music app set as a music app, if you have one).
3. **Unlock.** Unlock Private Space. Every private favorite is back in its old position, with its pop-up, without restarting Niagara.
4. **Edit while locked.** Lock again. Open the favorites editor, reorder or add a personal app, and save. Private apps are neither offered as candidates nor listed among the apps already chosen: the module skips the picker's row for a locked private app, and leaves the stored selection alone (since ticket 07's follow-up; see the ticket 07 notes). Record what you see; if a private app is still listed, the row step did not catch that list, and the placements are safe all the same. Unlock: every private favorite is still there. If you only added or removed a personal app, each stays in its old position. A reorder is different: Niagara reorders its whole stored list, hidden apps included, so dragging an app across a hidden favorite, or using the editor's sort action, can move that favorite by one place or more. Nothing may be missing. Record what moved. This is the check that only rendered rows were filtered. Afterwards undo your edit.
5. **Reboot while locked.** Lock, reboot the phone, unlock. Every placement is still there. Optional, for Niagara's ageing of missing apps (a row gets bit 256 after three days and 512 after fourteen): leave Private Space locked for several days, then run `snapshot` and check that no private app gained bit 256.
6. **After.** Run `snapshot > after.txt` and compare with `before.txt`: the `Feed` rows are the same apart from changes you made on purpose, and the private apps' `flags` (user 10) never gained bit 256 or 512 (Niagara's "missing app" cleanup). Bit 1024 is set while locked and cleared again when unlocked. If 256 appears after a long time locked, Niagara's startup scan stopped seeing the private apps while locked and the cleanup would eventually delete placements: record it and stop.
7. **Suspended private apps (test phone).** Many private apps on the test phone are suspended by root. While Private Space is unlocked their rows must still show.

## Tapping a locked private app unlocks it, then opens it

Ask first (it locks and unlocks Private Space). Locked private apps are hidden from favorites, suggestions and the media section (ticket 05), and once ticket 07 is in, from folders and pop-ups too, so the Niagara button is how you reach one. In Niagara's settings set the Niagara button's **tap** action to a private app (the picker lists private apps since ticket 03). If you have Niagara Pro, set the **swipe-up** action to a second private app. Put your previous actions back afterwards.

```sh
adb logcat -c   # then watch: adb logcat -s NiagaraPrivateSpace
```

1. **Unlocked.** With Private Space unlocked, tap the Niagara button. The private app opens straight away, as it did before.
2. **Locked, then unlock.** Lock Private Space and go to Niagara's main view. Tap the Niagara button. Android's unlock screen appears. Authenticate. The private app opens without another tap. Note how long it takes after you authenticate, and whether Niagara's home page shows first. Private Space is unlocked afterwards.
3. **Swipe up** (Pro). Lock again and swipe up on the button. The same happens for the swipe-up action's app.
4. **Cancel.** Lock, tap the button, and back out of the unlock screen. The phone stays on Niagara's main view, nothing opens, and Private Space is still locked.
5. **A forgotten tap stays forgotten.** Do step 4, wait more than a minute, then unlock Private Space from the Private Space section of the app list (its **Unlock private space** header). No app opens. (Android tells nobody that you backed out of the unlock screen, so the module forgets the tapped app after a minute. Unlocking within that minute, after cancelling, does open it: that is the known cost.)
6. **A second tap replaces the first.** With the tap action on app A and the swipe-up action on app B: lock, tap the button and back out, swipe up and authenticate. Only B opens.
7. **Another app in between.** Lock, tap the button and back out, open any personal app from the app list, return, unlock through the app list header. No private app opens.
8. **Personal and work apps.** With the module on, open a personal app and a work app from the app list and from favorites. They open as before: the hook sits on every app launch.
9. **Search.** Lock Private Space and search for a private app's name. Niagara hides locked private apps from search, so none should be offered. If one is, tapping it must bring up the unlock screen and then open it. Record which.
10. **Log.** `adb logcat -d -s NiagaraPrivateSpace` tells the story of each tap. Expected after a successful tap on a locked private app: `asked Android to unlock Private Space`, then `Private Space is available (android.intent.action.PROFILE_ACCESSIBLE): opening <component>`. There must be no `asking Android to unlock Private Space failed` and no `handling a profile broadcast failed` line (read its stack trace if there is: it says whether the user lookup or the launch failed). Other lines you may see: `Private Space is available but not accessible yet: waiting for PROFILE_ACCESSIBLE` (Android announced the unlock before the user's storage was ready), `Private Space is available (…), no app is pending` (the timeout passed, or a tap replaced the app), `Private Space locked: forgetting any pending app`. If the app does not open after you authenticate, these lines say which signal Android sent or did not send. Record them.

What this tests: that Android lets Niagara send the unlock request from a tap (AOSP's source says it does while Niagara is the foreground default launcher); that the module's receiver gets `PROFILE_ACCESSIBLE` (the module chose it because it says the user is running and unlocked, but where Android sends it was not checked in source) or `PROFILE_AVAILABLE`; and that a launch started from the receiver is not blocked as a background start (not checked in source either). The module waits for `PROFILE_ACCESSIBLE` rather than `PROFILE_AVAILABLE` alone because AOSP announces `PROFILE_AVAILABLE` right after it starts the user, before the user is necessarily unlocked, which is why `PROFILE_AVAILABLE` counts only if the user is already unlocked.

Known gap: while Android is still starting Private Space's user after you authenticate, Android no longer reports Private Space as locked, so a tap in that moment opens nothing and forgets the pending app. Tap again.

## Folders and pop-ups while locked

Ask first (it locks and unlocks Private Space). Use the hand-made folders: "Ciklum" holds 14 private apps and one personal app, "DataArt" mixes work, private and personal apps. Take a database snapshot (`snapshot` from "Hiding locked private apps from the home page") before and after, to see that the stored contents never change.

1. **Unlocked.** Open "Ciklum" and "DataArt". Every app shows, as before.
2. **Locked, folders.** Lock Private Space. Open "Ciklum": its personal app shows, and below it a row reads "Unlock Private Space to see your apps". Open "DataArt": the work and personal apps show in their old order, and the message row is below them. If you have a folder of private apps only, it shows just the message. The folders themselves stay on the home page.
3. **Locked, pop-ups.** Open the pop-up of a favorite that is not private (swipe on it) and that holds a private app: the same message shows in place of the private app. A pop-up that holds no private app is unchanged. The pop-up of a private favorite goes with its favorite (ticket 05).
4. **A real empty pop-up.** (Niagara's own row, not ours.) Open a pop-up that is empty in Niagara's database. It shows Niagara's own empty-pop-up text, not the unlock message.
5. **Tap the message.** Android's unlock screen appears. Authenticate. **Record:** whether the folder or pop-up stays open behind the unlock screen, and whether it shows the private apps as soon as you are back, or only after you reopen it. Expected: the apps appear without restarting Niagara.
6. **Cancel.** Lock, open "Ciklum", tap the message and back out. The folder still shows the message, nothing opened, Private Space is still locked.
7. **Unlock message after a tap that is still pending.** Lock, tap the Niagara button (action: a private app) and back out of the unlock screen, then within a minute open "Ciklum", tap the message and authenticate. The private app does not open on its own; the folder shows its apps.
8. **Long-press the message.** Niagara's row for an empty pop-up opens the pop-up editor for its host when long-pressed, and the module leaves that alone. Record what the editor lists. Before the picker row step (a follow-up to this ticket) it listed the private apps already in the folder.
9. **After.** Unlock, reopen "Ciklum" and "DataArt": every app is back in its old order. `snapshot` again: the `Feed` rows for both folders are unchanged. Do not leave the phone locked with a folder open.

The part that this section cannot test is the fallback when a Niagara update takes the message row away: private apps then hide without a message, and the log line says so. Only the unit tests cover it.

## Unsupported-version notice

Ask first (it restarts Niagara and writes to Niagara's data as root). The module has two debug switches, Android system properties read once when Niagara starts and off unless set to `1`. `debug.*` properties can usually be set from the adb shell; if the shell is refused, set them as root (`su -c setprop …`), which is what the steps below use. Whether Niagara's process may read them is not known; if the first step shows no `debug switches on` line, report that and stop.

```sh
SETPROP='su -c setprop'
PREFS=/data/data/bitpit.launcher/shared_prefs/niagara_private_space.xml
```

1. **Force "unsupported".** `adb shell "$SETPROP debug.niagaraps.unsupported 1"`, delete the module's own preferences (`adb shell "su -c 'rm -f $PREFS'"`), then restart Niagara (`am force-stop`, then home). The log shows `debug switches on: unsupported` and `inactive, missing: APP_LAUNCH`. Niagara behaves as stock: long-press a private app and **Favorite** is not offered.
2. **The tip.** On Niagara's home page a tip reads "Niagara Private Space doesn't support Niagara 1.16.32 yet", with an info icon and a **Dismiss** button. The log shows `showing the unsupported-version tip`. If the tip is missing, wait a few seconds with the home page open: the module waits for the home page and gives up after a minute.
3. **Dismiss it** by swiping it away. It disappears. Show it again (delete the preferences and restart) and dismiss it with the button this time. Record whether either of the two ways leaves anything behind.
4. **Once per version.** Restart Niagara: the tip does not return, and the log has no `showing` line. The preferences file holds the version code (`adb shell "su -c 'cat $PREFS'"` shows `last_notified_niagara_version_code` 1638).
5. **A new version.** Edit that number to something else (for example 1): after a restart the tip shows again, as it would for a new Niagara version.
6. **The Toast.** Also set `debug.niagaraps.no_tip_system 1`, delete the preferences, restart: a Toast with the same text appears, once, and the log shows `showed the unsupported-version notice as a Toast`. No tip appears.
7. **Switches off.** Clear both properties (`setprop … ""`), delete the preferences, restart: the module is `active` again, no notice appears, and the Expected log line is back.
8. **Niagara stays up.** Through steps 1 to 6 Niagara never crashes or restarts on its own, and the log has no `the unsupported-version tip failed`. If it does, the log has the stack trace; the module takes the tip back and falls to the Toast, so the phone should show the Toast instead.

What this proves: that Niagara's tip system accepts the tip the module registers (built with Niagara's factory, from a provider and callbacks that are dynamic proxies); that the dismiss and button callbacks remove it; that mask 210 gives the text, button and icon intended; and that the preferences file lives in Niagara's data and is never read by Niagara. None of it has run on a phone.

## Release build

Run it once before every release. Every section above uses the debug build, and the release build is minified by R8, which can remove or rename code the debug build keeps, so a missing keep rule would show only here. The release is built from the same source as the one CI publishes, with the release key from `~/.gradle/gradle.properties`.

```sh
./gradlew assembleRelease
adb shell pm list packages $M          # then, if it is listed: adb uninstall $M (a release build cannot go over a debug build)
adb install app/build/outputs/apk/release/app-release.apk
adb shell "$VECTOR modules enable $M"
adb shell "$VECTOR scope add $M $P/0"
adb logcat -c
adb shell am force-stop $P
adb shell input keyevent KEYCODE_HOME
adb logcat -d -s NiagaraPrivateSpace
```

1. The log line is the one under "Expected log line" (`active`, the same found list, `17 hooks installed`). If it says `inactive`, or a `finding the … failed` line appears that the debug build did not have, the minified build lost something: stop, record the line, and fix the keep rule before releasing.
2. Do the steps of "Add a private app to favorites from its long-press menu" once more with this build.
3. Reinstalling the debug build afterwards (`./gradlew installDebug`) needs the release build uninstalled first.

## Expected log line

On Niagara 1.16.32 (1638) with the pickers in place (ticket 03) the module should log `active` with `PICKER_FILTER_OVERRIDES=3` and `6 hooks installed`: the options builder, the base picker filter, three picker filter overrides and the Leisure Apps filter. After the suggestion and shortcut hooks (ticket 04) the totals become `PICKER_FILTER_OVERRIDES=3` and `9 hooks installed`: the six above plus the suggestion check, the options sheet and the Add shortcut screen's items. After the home page rows (ticket 05) they become `OTHER_HOME_ROWS=2` and `12 hooks installed`: the favorites rows, the suggestions rows and the media rows. After the app launch hook (ticket 06) it is `13 hooks installed`. After folders and pop-ups (ticket 07) it is `16`: the row builder, and the bind and click methods of the unlock message row. The picker row step, added after ticket 07, makes it `17`. The found list names every piece that resolved, fields included (the tip system's too: `TIP_MANAGER`, `TIP_REGISTER`, `TIP_FACTORY`, `TIP_HOME_PAGE`, `TIP_PAUSED` and `TIP_FACTORY_INSTANCE`): `APP_LAUNCH`, `LAUNCH_TARGET`, `LAUNCH_COMPONENT`, `FOLDER_AND_POP_UP_ROWS`, `FOLDER_AND_POP_UP_ROW_ITEM`, `EMPTY_POP_UP_ROW`, `EMPTY_POP_UP_ROW_BIND`, `EMPTY_POP_UP_ROW_CLICK`, `EMPTY_POP_UP_ROW_TEXT`, `POP_UP_HOST`, `POP_UP_NOTIFICATIONS` and `PICKER_ROW`. If a Niagara update takes the unlock message row away, the line still starts with `active` and ends the found list with `; without UNLOCK_MESSAGE_ROW (missing: …)`, and the count is two lower. A lost tip system shows the same way (`; without TIP_SYSTEM (missing: …)`), and costs no hook while the module is active. If the unlock message row was found but could not be installed, the line ends `; UNLOCK_MESSAGE_ROW left out at install` instead, after a warning that says why. The tip system adds no hooks while the module is active. When the module is inactive the only hook it installs is on the tip manager's constructor. Later tickets add hooks; their device-check sections say what the totals become.

