# 04: Private apps in suggestions and app shortcuts

**What to build:** Private apps can appear in Niagara's suggestions below favorites, and their long-press menu offers "Stop suggesting" and "Suggest again". A private app's app shortcuts (e.g. "New chat") can be pinned to favorites or to the app list, and the "Add shortcut" screen lists private apps. While Private Space is locked, private apps stay out of suggestions, as Niagara already does.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **Decision module:** answers whether an item may be suggested and whether its app shortcuts may be pinned, with private apps treated like work apps. Unit-tested.
- [ ] **Finder:** resolves the most-used suggestions check, the private-shortcut pin check and the "Add shortcut" screen check.
- [ ] **Hooks are essential:** they're added to the essential set.
- [ ] **While locked:** private apps aren't suggested. This is verified on the phone, not assumed.
- [ ] **Shortcuts on the phone:** whether pinning a private app's shortcuts works is verified on the phone. If Android refuses them, record that in the research notes and the spec.
- [ ] **Device check:** steps added.
