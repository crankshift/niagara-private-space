# Niagara Private Space

Niagara Private Space is an Xposed module that lets [Niagara Launcher](https://play.google.com/store/apps/details?id=bitpit.launcher) place **private apps** (apps in Android's Private Space) the way it places personal apps: in favorites, in folders and pop-ups, and as Niagara-button actions.

Niagara can already store, show and launch private apps in all those places. It only blocks them in its menus and app pickers. This module lifts that block, and hides private apps again while Private Space is locked.

It is not affiliated with Niagara Launcher.

**Status: work in progress, not released.** Written so far, and not yet run on a phone: the long-press menu of a private app offers what a work app's offers (Favorite, Edit pop-up and so on), and the favorites, folder, pop-up, Niagara-button, music, hide-apps, Leisure Apps and notification-summary pickers list private apps. Suggestions, app shortcuts and the Add shortcut screen are written too. Locked private apps are also hidden from the home page. Still to come: folders and pop-ups while locked, unlocking from a tap, and the notice for unsupported Niagara versions.

## Requirements

- A rooted Android 15+ device running [Vector](https://github.com/JingMatrix/Vector) v2.2 or newer (libxposed API 102; 101 also works). The original LSPosed is not supported.
- Niagara Launcher with Private Space set up in Android's built-in Private Space (not Samsung Secure Folder or other vendor vaults).

Built against Niagara 1.16.32 (1638). It has not been tested on a phone yet; the tested version will be named here after the device check.

## Install

1. Install the APK.
2. Enable the module in the Vector manager with Niagara (`bitpit.launcher`) in its scope.
3. Force-stop Niagara and open it again.

There is no settings screen, no toggle and no launcher icon: the module is on when Niagara is in its scope.

If a Niagara update changes the code the module recognises, the module changes nothing and Niagara behaves as stock. Vector's module log has a line that says what was missing.

## Build

Building requires the Android SDK (platform 37) and JDK 21.

```sh
./gradlew assembleDebug test
```

## License

GPL-3.0, see [LICENSE](LICENSE).
