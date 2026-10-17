# 06: Tapping a locked private app unlocks it, then opens it

**What to build:** When the user triggers a private app while Private Space is locked, Android's unlock screen appears. Once the user unlocks, the app opens. This covers a **Niagara-button action** (tap or swipe up) and any other place Niagara still shows a locked private app, such as search. Cancelling the unlock opens nothing and changes nothing.

**Blocked by:** 03: Private apps in every app picker; 05: Hide locked private apps from favorites

**Status:** ready-for-agent

- [ ] **Decision module:** decides what a tap does (open now, or unlock first and open later), and handles pending launches. Unit tests cover:
  - a pending app opens once Private Space becomes available;
  - a cancelled unlock forgets the pending app;
  - a second tap replaces the pending app;
  - an unlocked private app opens directly.
- [ ] **Unlock request:** Android's quiet-mode request for the private user, called directly, with Niagara's own unlock path as the fallback.
- [ ] **Opening after unlock:** the module itself opens the pending app when Private Space becomes available.
- [ ] **Finder:** resolves Niagara's single app-launch method.
- [ ] **Hook is essential:** it's added to the essential set.
- [ ] **On the phone:** with a private app set as the Niagara-button tap action and Private Space locked, tapping the button shows the unlock screen and then opens the app. Cancelling leaves the phone on the home screen.
- [ ] **Device check:** steps added.
