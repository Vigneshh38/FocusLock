# FocusLock

Open a blocked app -> you're sent home and the screen turns off.
To get an app back, remove it from the list inside FocusLock by entering every PIN in order.

## Run it
1. Open this folder in Android Studio -> let Gradle sync.
2. Plug in your phone (USB debugging on) -> Run.
3. First launch: choose how many PINs (2-5), set each one twice.
4. Tap "Enable blocker in Accessibility" -> FocusLock blocker -> On.
   - Android 13+: if the toggle is greyed out, go to Settings -> Apps -> FocusLock -> (3 dots) -> "Allow restricted settings", then try again.
5. Tap "Add apps", tick apps, "Block selected apps".

Requires Android 9+.
If you forget your PINs: Settings -> Apps -> FocusLock -> Clear storage (resets everything).
