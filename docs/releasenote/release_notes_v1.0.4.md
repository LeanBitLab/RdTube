### 💖 Support Our Work
* We are committed to making our apps as powerful and polished as possible. As an entirely community-funded project, we rely on your support to keep going—please consider becoming a [sponsor](https://github.com/sponsors/LeanBitLab). A huge thank you to all our current supporters!

## 🚀 What's New in v1.0.4

### ✨ Highlights & Fixes
- **🔊 System Media Volume Integration**: Volume gesture controls now adjust global Android system media volume via `AudioManager` (`audioManager.setStreamVolume(...)`), updating system volume levels across all videos.
- **☀️ Persistent Screen Brightness**: Custom screen brightness settings are now persisted to SharedPreferences (`"pref_brightness"`) and automatically restored across every video transition in the feed.
- **🚫 Zero-Conflict Edge Gestures**: Edge drag gestures now process on `PointerEventPass.Initial`, completely preventing `VerticalPager` from stealing or triggering vertical page scrolls while adjusting volume or brightness.
- **🔁 Live Loop Video Toggle Fix**: Resolved a bug where loop settings were overridden by an old auto-next handler. Added a live `SharedPreferences.OnSharedPreferenceChangeListener` so toggling "Loop Video" instantly updates player repeat modes.

## 📦 Downloads

| File | Description |
| :--- | :--- |
| **`app-release.apk`** | **Official Release APK** for RdTube v1.0.4 |
