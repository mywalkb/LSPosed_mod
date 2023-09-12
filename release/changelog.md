### Original Changelog

### API Changes
* Implementation of [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API):
Currently, most part of the new API has been roughly stable (except `helper`). We hope developers can test the new API to provide feedback on possible issues. The modern API will be published to Maven Central with the release of LSPosed 2.0.0, so before this, you can make suggestions to help make it better.
* Allow hooking processes of the `android` package besides `system_server` ([See this commit](https://github.com/LSPosed/LSPosed/commit/6f6c4b67d736e96a61f89b5db22c2e9bbde19461)): For historical reasons, the package name of `system_server` was changed to `android` (See [this commit from rovo89](https://github.com/rovo89/XposedBridge/commit/6b49688c929a7768f3113b4c65b429c7a7032afa)). To correct this behavior, for legacy modules, no code adjustment is needed, but the system framework is displayed as `system` instead of `android` in manager, with a new package `android` which is responsible for system dialogs, etc. For modern modules, the meaning of `system` and `android` in the declared scope have the same meaning as they display in manager.
<details>

```
system_server: uid=1000 pkg=system  proc=system
ChooserActivity,ResolverActivity: uid=1000 pkg=android proc=android:ui,system:ui
```
</details>

### Changelog
* Fix manager failed to launch when typing secret code in dialer
* Fix notification on Samsung
* Add Vercel/Cloudflare fallback for module repository
* Magisk version requires 24.0+, and for Riru favor, requires Riru  26.1.7+
* Make dex2oat wrapper more compatible, e.g. on KernelSU
* Fix some hooks on Android 8.1
* Add more hints for creating the shortcut and notification
* Fix backup race, fix 'JNI DETECTED ERROR IN APPLICATION: java_object == null'
* Fix `processName` for `handleLoadedPackage`'s `lpparam`
* Fix `isFirstPackage` for `afterHookedMethod`
* Fix notification intent for Android 14
* Fix manager dark theme
* Unconditional allow create shortcut except default desktop is not supported
* Fix NPE due to null `getModule()` return value
* Fix the typo in `AfterHooker` class name
* A11y: Add label for search buttons
* Set EUID to 1000 to fix notification and get modules list for Flyme
* Fix a race by lock-free backup implementation
* Predefine some SQLite modes for better performance
* Set db sync mode for Android P+, fix some Oplus devices not working
* Skip secondary classloaders that do not include code
* Avoid NPE when rendering empty markdown, fix a manager crash
* Add Installed hint for repo modules
* [translation] Update translation from Crowdin
* Upgrade target SDK to 34
* Only clear module's `LoadedApks` rather than all
* Upgrade Dobby, fix native hook on arm32
* Show manager package name instead of version
* Always allow pinning shortcuts, regardless of whether they are pinned or not
* Fix ANR when the boot is completed for Android 14
* Fix `IActivityManager.bindService` for Android 14
* Don't shrink non-AdaptiveIcons
* Fix the task icon for the manager
* Enable Xposed API call protection by default

### In my mod these are the changes:
* [cli] Support KernelSU without magisk
* Fix: Add automatic app only new installation and not update
