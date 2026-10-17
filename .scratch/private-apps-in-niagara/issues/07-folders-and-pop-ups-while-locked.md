# 07: Folders and pop-ups while locked

**What to build:** While Private Space is locked, folders and pop-ups stay visible. Their private apps are hidden, and a row reads "Unlock Private Space to see your apps", below any personal or work apps. Tapping that row brings up Android's unlock screen, and the private apps appear once the user unlocks. If a Niagara version breaks the borrowed message row, private apps are hidden with no message, and the rest of the module keeps working.

**Blocked by:** 05: Hide locked private apps from favorites; 06: Tapping a locked private app unlocks it, then opens it

**Status:** ready-for-agent

- [ ] **Decision module:** decides the rows of a folder or pop-up and whether to add the unlock message, with and without the message row available. Unit tests cover:
  - a folder of only private apps;
  - a mixed folder;
  - an unlocked folder;
  - a folder with no private apps.
- [ ] **Rendering only:** the hook filters the rows built by the shared folder and pop-up list controller, never the stored contents.
- [ ] **Message row:** a borrowed instance of Niagara's empty-pop-up row, with its text and tap replaced. Tapping it uses the unlock request from ticket 06.
- [ ] **Hook classes:** the row filter is **essential**. The message row is **optional**: when it's missing, the decision module falls back to hiding without a message, and the activation log line records it.
- [ ] **On the phone:** the user's hand-made folders (e.g. "Ciklum") show the unlock message while locked and their private apps after unlocking from it. Whether the folder or pop-up stays open behind the unlock screen is verified and recorded.
- [ ] **Device check:** steps added.
