# 08: Unsupported-version notice

**What to build:** When the module can't activate on a Niagara version, the user sees one dismissable home-screen tip, in Niagara's own tip style: "Niagara Private Space doesn't support Niagara <version> yet". It appears at most once per Niagara version. If Niagara's tip system can't be found either, a short Toast shows instead. Vector's log always gets a line.

**Blocked by:** 01: Add a private app to favorites from its long-press menu

**Status:** ready-for-agent

- [ ] **Decision module:** decides whether the notice is due from the current Niagara versionCode and the last one notified. Unit-tested: shown once per version, shown again for a new version, never shown when the module activated.
- [ ] **Tip:**
  - It's registered with Niagara's tip manager under the borrowed `XIAOMI_FULL_SCREEN_GESTURES_MIUI_12` type.
  - The provider and its dismiss and click callbacks are dynamic proxies dispatched by return type.
  - The message is free text, and the button and icon resources are resolved at runtime.
- [ ] **Fallback:** a Toast from Niagara's application context when the tip manager or factory can't be found.
- [ ] **Storage:** the last-notified versionCode lives in a module-owned preferences file inside Niagara's data, never in Niagara's own keys.
- [ ] **On the phone:** a forced-unsupported debug path shows the tip, which can be dismissed and doesn't come back after restarting Niagara. Forcing the tip system missing shows the Toast.
- [ ] **Device check:** steps added.
