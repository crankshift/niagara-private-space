# Vector instantiates the module entry class by name (META-INF/xposed/java_init.list).
-keep class * extends io.github.libxposed.api.XposedModule {
    <init>();
}

# The libxposed API is provided by the framework at runtime (compileOnly).
-dontwarn io.github.libxposed.api.**

# DexKit's native library finds these classes and fields by name.
-keep class org.luckypray.dexkit.** { *; }

# ItemFlags.canWriteFinalFields finds its probe field by name.
-keepclassmembers class io.github.crankshift.niagaraprivatespace.hook.ItemFlags$Probe {
    <fields>;
}
