### 💖 Support Our Work

As an open-source, community-funded project, we operate on a very limited budget. If RdTube helps you daily, please consider supporting us on [GitHub Sponsors](https://github.com/sponsors/LeanBitLab) or [Open Collective](https://opencollective.com/leanbitlab-org). Sharing RdTube with friends and family makes a huge difference!

## 🚀 RdTube 1.0.8

---

### ✨ Added
- **Sound on GIFs & Direct Stream Resolution**: Added audio stream extraction and direct high-definition playback for animated GIFs and external GIF providers, with seamless fallback to standard media streams.
- **Media Format & Duration Badges**: Added visual indicators on feed thumbnails showing `GIF` for animated clips and `VID mm:ss` with duration for videos, making content types immediately recognizable.
- **Relative Age Badges**: Added clean, minimal relative upload date badges (e.g., `2h`, `3d`, `5mo`) overlaying video thumbnails across feeds.
- **Subreddit-Scoped Video Search**: Integrated in-community searching directly inside the Search tab. Browsing a subreddit automatically sets an `In r/<name> ✕` scope chip, allowing effortless searches within the active community.
- **Related Community Autocomplete**: Integrated Reddit prefix autocomplete with directory search and reduced debounce to 300ms, returning exact matches and related subreddits instantly as you type.
- **Hardware Volume Auto-Unmute**: Raising the device volume using hardware volume buttons now automatically unmutes playback.
- **Dual Debug App Support**: Added `.debug` applicationIdSuffix and distinct "RedditTube (Debug)" naming so debug and release builds can be installed side-by-side on the same device.

### 🐛 Fixed
- **Search Loading UX & Flashing States**: Added responsive loading spinners to Subreddit and Video searches, eliminating premature "No search results found" flashes while results are fetching.
- **Topic & Title Ellipsis Truncation**: Standardized popular search tags, recent search chips, and community names to a single line with ellipsis (`...`) truncation, preventing multiline text wrapping.
- **Unified Navigation Back Stack**: Replaced cyclic tab navigation with a bounded, non-cyclical back stack, ensuring pressing Back moves sequentially through previous tabs without navigation loops.
- **Screen Keep-On During Playback**: Applied window keep-screen-on flags during active video playback to prevent the display from sleeping during long clips.
- **Brightness Restoration on Exit**: Ensured user and system brightness overrides are cleanly cleared upon exiting video playback.
- **Background Pause State Preservation**: Retained user-paused state when backgrounding the app, preventing videos from unexpectedly resuming playback when returning to the app.
- **Auto-Next Playback Advancement**: Resolved an edge-case conflict between loop preferences and completion position thresholds that prevented auto-advancing to the next video.
- **Watch History Re-Watch Bumping**: Re-watching a video now updates its timestamp and bumps it to the top of the history list, keeping both watched and liked lists ordered newest-first.

### 🔄 Changed
- **Unified Search Bar**: Consolidated all in-subreddit and feed search bars into the main bottom Search tab, removing confusing duplicate top search pills across screens.
- **Optimized Scrubbing Gesture Sensitivity**: Fine-tuned player slider touch responsiveness and description drawer expansion gestures for smoother navigation.
- **DASH Audio Query Handling & Comment Throttling**: Hardened DASH audio stream URL parsing against query parameters and throttled duplicate background comments requests.

---

### 📦 Downloads

| File | Description | Architecture |
| :--- | :--- | :---: |
| **`app-release.apk`** | **Official Release APK** for RdTube v1.0.8 | Universal (arm64-v8a, armeabi-v7a, x86_64) |
