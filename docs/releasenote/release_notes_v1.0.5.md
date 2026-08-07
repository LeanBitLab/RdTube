### 💖 Support Our Work
* We are committed to making our apps as powerful and polished as possible. As an entirely community-funded project, we rely on your support to keep going—please consider becoming a [sponsor](https://github.com/sponsors/LeanBitLab). A huge thank you to all our current supporters!

## 🚀 What's New in v1.0.5

### ✨ Highlights & Fixes
- **⚡ Extreme R8 Minification & APK Shrinkage**: Enabled R8 bytecode minification and resource shrinking with tuned ProGuard rules (`kotlinx.serialization`, Compose runtime, `Media3` reflection extractors), reducing release build size to an ultra-lean **3.1 MB**.
- **⚡ Baseline Profiles AOT Optimization**: Integrated `androidx.profileinstaller` and custom startup rules (`baseline-prof.txt`) to pre-compile critical user journeys on device installation for instant app TTI (Time to Interactive).
- **⬇️ Smart Cache-First Video Downloader**: Upgraded video export to stream via `MediaCacheManager`'s `CacheDataSource`. Saves videos directly to `Downloads/RedditTube` from local disk cache with 0 re-download network overhead.
- **🎛️ Predictive Back & System Gesture Exclusion**: Applied `Modifier.systemGestureExclusion()` to 48dp volume/brightness edge sliders to prevent system edge-back gestures from hijacking side drags, and enabled native Android 13/14+ Predictive Back (`android:enableOnBackInvokedCallback="true"`).
- **🎬 Navigation 3 Slide & Fade Transitions**: Configured custom `transitionSpec` on `NavDisplay` using combined `slideInHorizontally` + `fadeIn` and `slideOutHorizontally` + `fadeOut` for smooth screen switching.
- **⌨️ IME Soft Keyboard Layout Resizing**: Applied `.imePadding()` to `SearchPage` so input fields and search result lists smoothly resize above the software keyboard.
- **⏯️ Replay Button & Auto-Play Guard Clause**: Tapping the screen or play button when a video ends now executes `seekTo(0)` and `play()`, accompanied by a center Replay action button. Enforced duration/active page checks to eliminate phantom skips.
- **📖 Redesigned Ultra-Compact User Guide**: Transformed the About tab into a single-screen, high-density reference card detailing edge drag controls, feed swipe actions, double-tap seek, auto-next, smart save, and rotation lock.

## 📦 Downloads

| File | Description |
| :--- | :--- |
| **`app-release.apk`** | **Official Release APK** for RdTube v1.0.5 |
