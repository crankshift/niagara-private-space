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

1. **Connect.** The phone appears in `adb devices`.
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
4. **Read the log.** Run `adb logcat -c` before the restart in step 3, then `adb logcat -d -s NiagaraPrivateSpace` (the same line is in the Vector manager's module log). It shows one line like `Niagara 1.16.32 (1638): active, found: …(PICKER_FILTER_OVERRIDES=3); N hooks installed`. On Niagara 1.16.32 the expected counts are listed under "Expected log line" at the end of this file. A line like `Niagara <version>: inactive, missing: …` means a fingerprint no longer matches this Niagara version: stop, record the missing parts, and fix the finder. `finding the … failed` lines above it say why.

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

## Expected log line

On Niagara 1.16.32 (1638) with the pickers in place the module should log `active` with `PICKER_FILTER_OVERRIDES=3` and `6 hooks installed`: the options builder, the base picker filter, three picker filter overrides and the Leisure Apps filter. Later tickets add hooks; their device-check sections say what the totals become.

