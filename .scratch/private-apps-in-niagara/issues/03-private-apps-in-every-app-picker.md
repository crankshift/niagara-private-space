# 03: Private apps in every app picker

**What to build:** Every Niagara app picker lists private apps alongside personal and work apps. Covered:
- the favorites editor;
- the folder and pop-up editor;
- the Niagara-button tap and swipe-up action pickers;
- the music-apps picker;
- hide apps;
- Leisure Apps;
- notification summary.

The user can add a private app to a folder or pop-up, set it as a **Niagara-button action**, set it as a music app, add it to Leisure Apps, hide it from the app list, and pick it for the notification summary.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **Decision module:** answers picker eligibility, with private apps eligible wherever work apps are. Unit-tested.
- [ ] **Leisure Apps:** Niagara requires a personal app here, even for work apps. The requirement is lifted for private apps only; work apps are unchanged.
- [ ] **Finder:** resolves the base picker filter and each override that rejects private apps.
- [ ] **Hooks are essential:** they're added to the essential set of the activation rule.
- [ ] **Existing placements:** editing a folder or pop-up that already holds private apps (e.g. the user's hand-made folders) keeps them after saving.
- [ ] **Unchanged:** the widget picker still excludes private widget providers (out of scope).
- [ ] **Device check:** steps added for adding a private app to a folder, a pop-up, a Niagara-button action and the music apps.
