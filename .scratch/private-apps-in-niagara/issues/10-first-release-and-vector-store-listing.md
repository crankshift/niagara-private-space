# 10: First release and Vector store listing

**What to build:** Niagara Private Space is published and appears in Vector's module store. This ticket is mostly the user's own steps.

**Blocked by:** 02: Fingerprint check against a local Niagara APK; 03: Private apps in every app picker; 04: Private apps in suggestions and app shortcuts; 05: Hide locked private apps from favorites; 06: Tapping a locked private app unlocks it, then opens it; 07: Folders and pop-ups while locked; 08: Unsupported-version notice; 09: Release pipeline

**Status:** ready-for-human

- [ ] **Device check:** the full check is run on the phone, with the user's go-ahead, and passes. The store page names that Niagara version as tested.
- [ ] **Submission:** the user submits the module at modules.lsposed.org/submission with description "Niagara Private Space", and accepts the store org's invite.
- [ ] **Description:** the store repo's GitHub description reads "Niagara Private Space", set on the website (⚙ next to About) if it's blank. The agent verifies it through the API.
- [ ] **Push:** the repo is pushed to `crankshift/niagara-private-space`, after the user approves.
- [ ] **First release:** the first tag is pushed, and the release workflow publishes to both repos. This is the workflow's first real tag-triggered run, so it's watched.
- [ ] **Listing:** the module appears on modules.lsposed.org. Vector's store follows within about 8 days, and this is confirmed later.
