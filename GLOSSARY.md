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

**Niagara-button action**:
The app the Niagara button opens on tap or on swipe up.
_Avoid_: gesture, button shortcut

**Placement**:
An app's position in favorites, a folder or a pop-up, or as a Niagara-button action.
_Avoid_: entry, spot, slot, item

### The module

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
A piece of Niagara code the module must locate, and usually change. A hook is either found or missing.
_Avoid_: patch, target

**Resolution report**:
The finder's list of which hooks were found. The decision module turns it into activate or don't.
_Avoid_: scan result

