### 💖 Support Our Work
* We are committed to making our apps as powerful and polished as possible. As an entirely community-funded project, we rely on your support to keep going—please consider becoming a [sponsor](https://github.com/sponsors/LeanBitLab). A huge thank you to all our current supporters!

## 🚀 What's New in v1.0.6

### ✨ Highlights & Features

- **🖤 Pitch Black OLED & Monochrome Minimalist Theme**: Overhauled the entire UI with a pure pitch-black OLED background (`0xFF000000`), crisp monochrome white accents, glass borders, and an updated "Signal Play" adaptive app icon.
- **🔑 Custom Reddit API Keys & Client ID Overrides**: Added full support for custom Reddit OAuth Client IDs, User Agents, and Redirect URIs with an in-app advisory to use personal Client IDs, inspired by Continuum.
- **🔓 1-Tap Reddit OAuth Authorization**: Seamless 1-tap browser login to unlock unrestricted/mature subreddits, media streams, and personalized subreddit subscriptions with 0 tracking.
- **🔍 Dual Segmented Search Page**: Dedicated Search tab with segment switching between **Reddit-Wide Video Search** and **Subreddit Search**, complete with 1s debounced search, instant Enter action, and dedicated pagination.
- **💬 Instant Top Comments Viewer**: View up to 100 top comments sorted by score in a high-speed bottom sheet with dedicated non-blocking parallel fetching (~200–400ms).
- **⚡ 1-Call Multi-Subreddit Feed Acceleration**: Subreddit feeds now query Reddit's native multi-subreddit endpoint, loading combined explore and subscribed feeds in **~300ms in a single HTTP request**.
- **📜 Expandable Video Titles**: Long video titles (>55 chars) now feature smooth animated expansion (`animateContentSize()`) with a **Show more / Show less** toggle.
- **🎛️ Comprehensive Content & Playback Preferences**: Added customizable preferences for Thumbnail Quality, Prefetch Depth, Default Audio Unmuted, Haptic Feedback, and Auto Update Checks.
- **🧭 Sort Caching & Concurrency Safety**: Fixed feed caching to isolate caches per sort mode (Hot, New, Rising, Top Day/Week/Month/Year/All) and synchronized OAuth token acquisition to prevent cold-start rate-limiting.
- **🛡️ R8 Full Mode & Bytecode Optimization**: Enabled R8 Full Mode and configured ProGuard rules to automatically strip debug logs (`Log.v`, `Log.d`, `Log.i`) in release builds.

## 📦 Downloads

| File | Description |
| :--- | :--- |
| **`app-release.apk`** | **Official Release APK** for RdTube v1.0.6 |
