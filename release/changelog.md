### Original Changelog
- Update translation to fix crashes in some languages
- Some UI fixes
- Avoid calling `finishReceiver` for unordered broadcasts
- Clear application profile data before performing dexOpt
- Distinguish update channels when checking updates
- Fix hook/deoptimize static methods failed on some Android 13 devices
- Repository shows assets size and download counts
- Fix hooking proxy method
- Init resources hook when calling `hookSystemWideLayout`

### In my mod these are the changes:

- now user can **choose** if set automatic add new installed packages to module
- fix add column automatic_add when exist
