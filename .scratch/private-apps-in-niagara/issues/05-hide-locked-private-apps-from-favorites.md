# 05: Hide locked private apps from favorites

**What to build:** While Private Space is **locked**, private apps disappear from favorites, along with any pop-up attached to them. When the user unlocks, they come back in their old positions. Locking, unlocking and rebooting never remove a **placement**.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **Locked detection:** based on "is Private Space" plus Android's quiet-mode state for the private user, or Niagara's locked flag. Never Niagara's "hidden everywhere" state, which is also true for suspended apps while unlocked. Unit tests cover a suspended private app staying visible while unlocked.
- [ ] **Decision module:** decides which rendered favorites rows to keep. Unit-tested for: locked private hidden; unlocked private shown; personal and work never hidden.
- [ ] **Rendering only:** the hook filters only the rows being rendered, never the stored favorites list. After locking, editing favorites and unlocking, every private placement is still there.
- [ ] **Updates:** favorites update on lock and unlock without restarting Niagara.
- [ ] **Hook is essential:** it's added to the essential set.
- [ ] **On the phone:** the user locks Private Space for a while, reboots, and unlocks. Every placement survives, confirmed by reading Niagara's database as root. Done only with the user's go-ahead.
- [ ] **Device check:** steps added.
