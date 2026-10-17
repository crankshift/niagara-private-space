# Fingerprint check

`FingerprintTest` runs the finder against a Niagara APK on the Mac and fails when any hook does not resolve to exactly one target. It is optional: it is skipped, never failed, while `NIAGARA_APK` is unset. A hook added by a later ticket is covered without any setup, because the test walks every `Hook`.

**Status: untested beyond the skip.** DexKit's Maven artifact ships Android libraries only (see "DexKit on the desktop JVM" in `research/niagara-internals.md`), so nobody has loaded DexKit on this Mac, and nothing below has been run end to end. Only the skip (no variables, or only `NIAGARA_APK`) has been exercised. Until a host library exists, the check is the module's log line on the phone (`docs/device-check.md`).

## Pull Niagara's APK

Keep it outside the repo. APKs are gitignored, and Niagara's APK must never be committed to this public repo. (The `adb pull` line is untested; `pm path` prints one `package:` line per split, and Niagara has only a base APK.)

```sh
mkdir -p ~/niagara-apk
adb pull "$(adb shell pm path bitpit.launcher | head -1 | cut -d: -f2 | tr -d '\r')" ~/niagara-apk/base.apk
```

## Build DexKit for this Mac

Needs CMake (`brew install cmake`) and a JDK that has an `include/` directory, such as the Temurin 21 under `~/.gradle/jdks` (Android Studio's JDK has none). The recipe follows DexKit's `dexkit/src/main/cpp/CMakeLists.txt` and **has not been run**. It pins the 2.3.0 tag, the version the module depends on.

```sh
git clone --depth 1 --branch 2.3.0 https://github.com/LuckyPray/DexKit.git ~/dexkit
JAVA_HOME=<a JDK with include/> cmake -S ~/dexkit/dexkit/src/main/cpp -B ~/dexkit-host -DCMAKE_BUILD_TYPE=Release
cmake --build ~/dexkit-host -j8   # produces ~/dexkit-host/libdexkit.dylib
```

## Run it

```sh
NIAGARA_APK=~/niagara-apk/base.apk DEXKIT_LIBRARY=~/dexkit-host/libdexkit.dylib \
  ./gradlew :app:testDebugUnitTest --tests '*FingerprintTest'
```

Run it after pulling a new Niagara build and before installing it. Gradle treats both files as inputs, so a new APK at the same path reruns the test.
