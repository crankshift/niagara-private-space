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
4. **Read the log.** Run `adb logcat -c` before the restart in step 3, then `adb logcat -d -s NiagaraPrivateSpace` (the same line is in the Vector manager's module log). It shows one line like `Niagara 1.16.32 (1638): active, found: …; N hooks installed`. A line like `Niagara <version>: inactive, missing: …` means a fingerprint no longer matches this Niagara version: stop, record the missing parts, and fix the finder. `finding the … failed` lines above it say why.

Only tap the screen with `adb shell input tap` after confirming which app is in front (`adb shell dumpsys activity activities | grep -m1 topResumedActivity`).

## Add a private app to favorites from its long-press menu

Private Space must be unlocked.

1. Open the app list and scroll to the Private Space section at the bottom.
2. Long-press a private app that is not yet in favorites. The menu offers the same options as for a work app, including **Favorite**, **Edit pop-up** and **Add to category**. (**Set as music app** appears only for apps Niagara considers music-capable.)
3. Choose **Favorite**. The app appears in favorites with Android's Private Space badge.
4. Tap it in favorites. It opens normally.
5. **Stock behaviour is intact:** the Private Space section is still at the bottom of the app list, the private app is not duplicated in the main list, and a personal app's long-press menu is unchanged.
