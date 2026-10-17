# Change Niagara's profile flags for the length of one check

Niagara marks every item with one final boolean, "is Private Space", and checks it in a handful of menu, picker and suggestion methods. To let a **private app** through such a check, the module changes the flags on that one item so that it looks like a work app (or, for Leisure Apps, a personal app, which also sets Niagara's "is primary user" flag), lets Niagara's own method run, and puts the flags back in a `finally`.

We chose this over overriding the method's result because Niagara's methods go on to apply other rules after the Private Space check (the pop-up editor refuses the host app itself, the options builder adds a dozen entries). Overriding the result would mean re-implementing those rules from obfuscated code. Changing the flags lets Niagara's code do it, and handles private apps exactly as it handles **work apps**.

We chose it over changing the flags for good because the Private Space section in the app list and hiding private apps from search read the same flag, and both must keep working.

**Costs:**

- The flags are `final`. Reflection can write it after `setAccessible(true)` on the JVM, and ART is expected to allow it, but that is checked only on the phone. If a write is refused, the hook throws, the framework's protective mode runs Niagara's own method, and private apps stay blocked. It fails closed.
- While the flags are changed, another thread reading the same item's flags would see "not private". The window is one call (for a picker, one app at a time as it filters its list). The module's own changes are serialised with a lock, so they cannot undo each other, but Niagara's reads are not locked.
- One check sits inside a coroutine body that loops over items built in the same method (the Add shortcut screen), so there is no per-item call to wrap. **The one exception to "for one check":** the module changes the flags of that screen's items for good, as soon as they are built. In the 1638 bytecode the constructor of those items has a single caller, the Add shortcut coroutine, so the items exist for that screen. What the screen does with an item afterwards was not traced, which is why this is the only place the module changes a flag for good, and why ticket 04's device check looks at it.
