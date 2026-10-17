# Changelog

Each `## <versionName>` section becomes the notes of that version's release.

## 0.1.0

First release.

### Private apps in Niagara
Niagara can already store, show and launch private apps in favorites, folders and pop-ups; it only blocks them in its menus and app pickers. This module lifts that block and hides private apps again while Private Space is locked:
- A private app's long-press menu offers what a work app's offers (Favorite, Edit pop-up and so on)
- The pickers for favorites, folders, pop-ups, Niagara-button actions, music apps, hidden apps, Leisure Apps and the notification summary list private apps
- Suggestions, app shortcuts and the Add shortcut screen include private apps
- While Private Space is locked, its apps are hidden from the home page, from folders and pop-ups (which show "Unlock Private Space to see your apps" instead) and from the pickers' lists of chosen apps. Nothing stored is removed
- Tapping a locked private app (the Niagara button, for example) brings up Android's unlock screen and opens the app once Private Space is unlocked
- If a Niagara update means the module no longer recognises the code it changes, it changes nothing and tells you once per Niagara version, with a tip on Niagara's home page (or a Toast)

There is no settings screen, no toggle and no launcher icon: the module is on when Niagara is in its scope.

### Requirements
- Vector v2.2 or newer (libxposed API 101+)
- Android 15 or newer
- Niagara Launcher, with apps in Android's built-in Private Space

Built against Niagara 1.16.32 (1638); not yet tested on a phone.
