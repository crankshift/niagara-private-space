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

**Status:** done-pending-device-check

- [x] **Decision module:** answers picker eligibility, with private apps eligible wherever work apps are. Unit-tested.
- [x] **Leisure Apps:** Niagara requires a personal app here, even for work apps. The requirement is lifted for private apps only; work apps are unchanged.
- [x] **Finder:** resolves the base picker filter and each override that rejects private apps.
- [x] **Hooks are essential:** they're added to the essential set of the activation rule.
- [x] **Existing placements:** editing a folder or pop-up that already holds private apps (e.g. the user's hand-made folders) keeps them after saving.
- [x] **Unchanged:** the widget picker still excludes private widget providers (out of scope).
- [x] **Device check:** steps added for adding a private app to a folder, a pop-up, a Niagara-button action and the music apps.

## Notes

- **Pickers hide private apps while Private Space is locked.** Not in the ticket, but the spec's reason for hiding anything while locked ("nobody holding the phone should see which apps I keep there") applies to pickers too: stock never lists a private app, so lifting the block without a lock check would show locked apps' names in a picker. `isEligibleInPicker` and `profileSeenByPicker` therefore take `privateSpaceLocked`; a locked private app stays private to every picker and Niagara refuses it as before. The lock state comes from `PrivateSpaceState` (Android's quiet mode for the private profile; any failure counts as locked).
- **Decision shape.** `Decisions.isEligibleInPicker(kind, rule, privateSpaceLocked)` with `PickerRule.ANY_PROFILE` (personal and work apps qualify) and `PickerRule.PERSONAL_APPS_ONLY` (Leisure Apps). A private app qualifies under both; a work app only under the first. `Decisions.profileSeenByPicker(kind, rule, privateSpaceLocked)` says which profile a picker should see the app as so that Niagara's own check for the rule passes: a private app looks like a work app, or a personal app for Leisure Apps. The adapter asks only that, so swapping the two is caught by a test. Leisure Apps therefore needs both flags changed for one call (Private Space off, primary user on), which is why ticket 01 resolved the primary-user flag.
- **The pickers are found by structure, not by name.** Every picker filter is a method that takes an item, returns a boolean and reads the Private Space flag, so the finder takes the flag's readers with that signature (checked on 1638's bytecode: five of them). The **base** is the filter whose class another filter's class extends (`HdxOm26AKWF`); the **Leisure Apps** filter is the one that also reads the primary-user flag (`Chs0o…`); the **overrides** are the rest, in classes that extend the base's class (`tNjv…`, the pop-up and folder editor, `IOoBH…`, hide apps, and `p673N…`, notification summary). A filter outside that family would be left alone. Three hooks: `PICKER_FILTER`, `PICKER_FILTER_OVERRIDES` (several targets) and `PICKER_FILTER_LEISURE_APPS`; all essential. (`FingerprintTest` was reworded: it is "exactly once" for every hook except the overrides, which cover several pickers.) The favorites editor, the Niagara-button pickers and the music-apps picker inherit the base filter.
- **Why no per-picker names.** Hiding apps, folders and notification summary behave the same, so the module never needs to know which override is which. A Niagara update that adds another picker override is picked up by the same rule.
- **Existing placements.** The criterion rests on the research (`niagara-internals.md`, "Every app picker"): the picker copies the current feed into its selection unfiltered, and saving writes the selection back, which is why hand-made private rows survive editing today. The module changes only the candidate filter and never touches the selection. In the list builder (`ho3Z3OuW2p`, around the call of the filter) a candidate is also dropped when it is already in either of two lists rendered earlier; I did not trace what those lists hold, so "private apps already in a folder are not offered twice" is expected, not established. The device check (step 2 of the picker section) is where this is seen.
- **Widgets stay out.** The widget picker's check is in a coroutine in `XoYnnUj9fSu`, which this module does not hook.
- **The fingerprint test needs no change.** It asks whether each `Hook` was found, and the finder reports a single-target hook only when its fingerprint matched exactly once and `PICKER_FILTER_OVERRIDES` when it matched at least once (three on 1638). The finder now throws on a fingerprint that matched nothing or several targets and reports it through `onError`, so the module's log line says which one and why (this also tidied the finder ticket 01 wrote).
- **Review round.** /code-review found that the profile a picker sees was chosen outside the decision module (swapping WORK and PERSONAL passed every test), a test name that still said "exactly one" for a hook that covers several pickers, and claims in these notes that I had not traced. Fixed: `profileSeenByPicker`, the reworded test, the softened notes above. Added `Picker` and `Leisure Apps` to the glossary. Declined: bundling `(hooks, flags)` into a context object (two parameters; not worth a type yet).
- **Second review round.** Fixed: overlapping `whileSeenAs` calls on one item could restore each other's half-changed flags and leave a private app looking like a work app for good (the picker filters run in a coroutine, the long-press on the main thread). Calls are now serialised with a lock, and the two flags are restored in nested `finally` blocks so a failed write cannot leave the Private Space flag changed. Stock readers elsewhere are not locked, so they can still see the changed flags for one call; the ADR says so. The log line now shows how many methods multi-target hooks matched (`PICKER_FILTER_OVERRIDES=3` on 1.16.32), so a Niagara update that loses one override shows up there. Status changed to `done-pending-device-check`: the criteria rest on reading code until the device check has run.
- **Known limits.**
  - The pop-up and folder editor's filter is also used for private apps' deep shortcuts (`ho3Z3OuW2p`, around the filter calls), so those rows are unblocked there too. Ticket 04 covers shortcuts; the device check does not exercise this yet.
  - The notification-summary picker's "suggested" group (`p673N…WuObUi…`) still requires a primary-user app, so private apps appear in its full list but not in that group.
- **Device, 2026-10-17 (a first look, not the section).** On the test phone the favorites editor listed private apps with the Private Space badge alongside personal and work apps, with a private app and its personal copy as separate entries (step 1 of "Private apps in every app picker", seen while checking ticket 01). Steps 2 to 10 have not been run, so this ticket stays `done-pending-device-check`.
