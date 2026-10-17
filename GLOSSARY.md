# Niagara Private Space

An Xposed module for the Vector framework that lets Niagara Launcher place private apps in favorites and folders, the same way it places personal apps.

## Language

### Spaces and apps

**Private Space**:
Android's built-in hidden, lockable profile (Android 15+). Only the AOSP feature, not OEM vaults.
_Avoid_: private profile, hidden space, vault, secure folder

**Private app**:
An app installed inside Private Space. The same app may also exist as a separate personal app.
_Avoid_: hidden app, private space app

**Personal app**:
An app installed in the phone's main profile.
_Avoid_: default app, normal app, regular app, main app

**Work app**:
An app installed in a managed work profile. It's neither a personal app nor a private app.
_Avoid_: business app, managed app

**Locked**:
The state of Private Space in which its apps are paused and must not be visible. Its opposite is **unlocked**.
_Avoid_: quiet mode, paused, disabled

**Available**:
Private Space is available when it is unlocked and its apps can start. Android announces this a moment after the unlock, and the module opens the pending app then.
_Avoid_: accessible, ready

### Niagara

**Favorites**:
Niagara's home-screen list of apps the user has chosen.
_Avoid_: home screen, dock, pinned apps

**Folder**:
A named group of apps that sits in favorites.
_Avoid_: group, stack

**Pop-up**:
A set of extra apps attached to a single favorite.
_Avoid_: swipe apps, sub-favorites

**Host**:
The favorite or folder that a pop-up, or a folder's contents, belongs to.
_Avoid_: owner, parent

**Picker**:
A Niagara screen where the user chooses apps: the favorites editor, the folder and pop-up editor, the Niagara-button action pickers, and the pickers for music apps, hidden apps, Leisure Apps and the notification summary.
_Avoid_: selector, chooser

**Leisure Apps**:
Niagara's category of distracting apps. Its picker asks for a personal app, which work apps do not satisfy.
_Avoid_: distraction apps

**Suggestions**:
The apps Niagara shows below favorites because the user opens them most. The user can stop and resume suggesting an app.
_Avoid_: recommendations, most-used row

**App shortcut**:
A shortcut an app offers for one of its actions, such as "New chat". It can be pinned to favorites or the app list.
_Avoid_: deep link, quick action

**Tip**:
A dismissable card on Niagara's home page, in Niagara's own style, with a message, a button and an icon. Niagara registers tips with its tip manager by type.
_Avoid_: banner, notification

**Tip system**:
Niagara's tip manager and what the module needs from it to show one tip: the manager's constructor, its registration method, the factory that builds a tip, and three fields (the home page, a paused flag, the home page's factory). It is an optional part: without it the notice is a Toast.
_Avoid_: tip engine

**Long-press options**:
The menu Niagara shows when the user long-presses an app, folder or shortcut. Niagara's code calls it the options sheet.
_Avoid_: context menu, popup menu

**Add shortcut screen**:
The Niagara screen that lists shortcuts apps offer, for adding to favorites.
_Avoid_: shortcut picker

**Listed**:
Shown by a Niagara list that offers apps by themselves, or as a row of the home page: suggestions, the Add shortcut screen, pickers, favorites. A private app is listed only while Private Space is unlocked.
_Avoid_: visible, shown

**Home page**:
Niagara's main view. It is built from sections: favorites, suggestions and the media section, which shows music apps when headphones or a speaker connect. Each section renders one row per app it shows.
_Avoid_: home screen (the phone's own), launcher page

**Niagara-button action**:
The app the Niagara button opens on tap or on swipe up.
_Avoid_: gesture, button shortcut

**Placement**:
An app's position in favorites, a folder or a pop-up, or as a Niagara-button action.
_Avoid_: entry, spot, slot, item

### The module

**Notice**:
What the module tells the user when it does not support their Niagara version: "Niagara Private Space doesn't support Niagara <version> yet". It is a borrowed tip, or a Toast when Niagara's tip system cannot be used, and it is shown once per Niagara version.
_Avoid_: warning, alert

**Unlock message**:
The row a folder or pop-up shows below its other apps while Private Space is locked, in place of its private apps. It reads "Unlock Private Space to see your apps", and tapping it sends the unlock request. It is built from Niagara's own row for an empty pop-up, so a Niagara update can take it away; the private apps then hide without a message.
_Avoid_: placeholder, banner

**Pending app**:
The private app the user tapped while Private Space was locked. The module sends the unlock request and opens the app itself once Private Space is available. It is forgotten when it opens, when another tap replaces it or opens something else, when Private Space locks again, when the unlock request fails or is replaced by an unlock message's request, and after a minute.
_Avoid_: queued app, deferred launch

**Unlock request**:
The module's call asking Android to unlock Private Space, which brings up Android's credential screen. The request itself says nothing about the outcome.
_Avoid_: unlock call, unlock intent

**Debug switch**:
One of two Android system properties, read once when Niagara starts and off unless set to `1`, that make the module pretend its code or Niagara's tip system is missing, so the notice can be seen on the phone without a Niagara update.
_Avoid_: debug flag, test mode

**Decision module**:
The pure Kotlin part of the module that holds every decision, without Android, Niagara or Xposed types. The unit tests exercise it.
_Avoid_: logic, rules engine

**Finder**:
The part of the module that locates the Niagara code the module changes, using fingerprints, and produces a resolution report.
_Avoid_: scanner, resolver

**Fingerprint**:
How the finder recognises one piece of Niagara's code despite its obfuscated names: a string, a signature or a relation to other code that survives a Niagara update.
_Avoid_: signature, pattern

**Hook**:
A piece of Niagara code the module must locate, and usually change. A hook is either found or missing, and is **essential** unless it belongs to an **optional part**.
_Avoid_: patch, target

**Optional part**:
A piece of the module that a Niagara update may take away without costing the module its purpose: the unlock message, and the tip system the notice borrows. The module runs without it, and the activation log line says so. One missing hook of a part costs the whole part.
_Avoid_: feature flag, plugin

**Resolution report**:
The finder's list of which hooks were found. The decision module turns it into activate or don't.
_Avoid_: scan result

### Releasing

**Store repo**:
The repo under the Xposed Modules Repo organization that Vector's store lists the module from (`Xposed-Modules-Repo/io.github.crankshift.niagaraprivatespace`). The store's submission form creates it. The release workflow copies the store page and each release's APK into it. The store leaves out a repo whose GitHub description is blank, so it must read "Niagara Private Space". The workflow's `MODULES_REPO` variable, its "Modules Repo" step names and the `MODULES_REPO_TOKEN` secret keep Origram's name for it.
_Avoid_: org repo, mirror

**Store page**:
What the store shows for the module: `README.md`, `SUMMARY`, `SOURCE_URL` and `SCOPE`, kept in `modules-repo/` and never with a `HIDE` file. Its README names the last Niagara version the module was tested with.
_Avoid_: store description, store files

**Release key**:
The module's own signing key, kept outside the repo. Installed copies accept updates only from the same key, and the release workflow refuses an APK whose certificate is not the one pinned as `CERT_SHA256`.
_Avoid_: upload key, signing certificate

**Dry run**:
A manual run of the release workflow that builds and checks the signed APK without publishing anything.
_Avoid_: test release
