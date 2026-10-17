# Clear Niagara's Private Space flag for the length of one check

Niagara marks every item with one final boolean, "is Private Space", and checks it in a handful of menu, picker and suggestion methods. To let a **private app** through such a check, the module clears the flag on that one item, lets Niagara's own method run, and puts the flag back in a `finally`.

We chose this over overriding the method's result because Niagara's methods go on to apply other rules after the Private Space check (the pop-up editor refuses the host app itself, the options builder adds a dozen entries). Overriding the result would mean re-implementing those rules from obfuscated code. Clearing the flag lets Niagara's code do it, and handles private apps exactly as it handles **work apps**.

We chose it over clearing the flag for good because the Private Space section in the app list and hiding private apps from search read the same flag, and both must keep working.

**Costs:**

- The flag is `final`. Reflection can write it after `setAccessible(true)` on the JVM, and ART is expected to allow it, but that is checked only on the phone. If a write is refused, the hook throws, the framework's protective mode runs Niagara's own method, and private apps stay blocked. It fails closed.
- While the flag is cleared, another thread reading the same item's flag would see "not private". The window is one call, on the item the user is acting on.
- Two checks sit inside a coroutine body that loops over items built in the same method (the "Add shortcut" screen). There is no per-item call to wrap, so the module cannot lift that one.
