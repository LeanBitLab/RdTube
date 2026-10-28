### 💖 Support Our Work

As an open-source, community-funded project, we operate on a very limited budget. If RdTube helps you daily, please consider supporting us on [GitHub Sponsors](https://github.com/sponsors/LeanBitLab) or [Open Collective](https://opencollective.com/leanbitlab-org). Sharing RdTube with friends and family makes a huge difference!

## 🚀 RdTube 1.0.7

---

### ✨ Added
- **Top Search Bar & Zero-Shift Tab Navigation**: Moved the search bar to the top of the Search page directly above the segmented tab bar. Standardized content padding and spacing across both Subreddits and Videos tabs, eliminating layout jumping and keyboard occlusion.
- **Granular Video Search Filters**: Integrated filter pills directly into video search results for Relevance, Hot, Top (Today, Week, Month, Year, All-Time), New, and Comments, constructing Reddit search parameters on the fly.
- **Unified Search History for Videos & Subreddits**: Expanded recent search history recall across both Video and Subreddit searches, complete with individual chip deletion ('✕') and tactile haptic feedback.
- **Auto-Mute NSFW Videos Preference**: Added an option under Playback & Audio settings to automatically start mature (18+) videos muted, protecting against sudden loud audio in public.
- **30% Watch Progress History Tracking**: Refined the automatic watch history threshold to 30% (`(duration * 30L) / 100L`) with high-frequency 250ms polling and a `DisposableEffect` to ensure watched clips are reliably recorded even when swiped away quickly.
- **Categorized Solid Surface Settings**: Replaced cluttered settings with 5 clean, foldable category cards (App Preferences, Reddit Account & API Keys, Help & Reference, Official Channels & Socials, Version & Release Notes) styled with modern solid OLED surfaces.
- **Conditional API Key Overrides**: Advanced Reddit Client ID, User Agent, and Redirect URI configuration rows are now cleanly encapsulated in an animated expansion that is visible only when "Enable Overrides" is toggled on.

### 🐛 Fixed
- **Instant Reactive History Clearing**: Introduced `watchedIdsFlow: StateFlow<Set<String>>` in `MainScreenViewModel` and re-keyed `LibraryPage` watch history, ensuring tapping "Clear History" wipes the UI immediately without having to switch tabs or reopen pages.
- **Feed Rotation Lock Persistence**: Hoisted rotation lock state to the feed level backed by SharedPreferences, ensuring orientation lock persists across video swipes instead of reverting to portrait on every clip change.
- **High-Resolution Thumbnail Loading**: Fixed repository parsing logic to prioritize unescaped high-resolution preview source images over 140x140 legacy Reddit thumbnails.
- **R8 Compose Null-Safety in Release Builds**: Fixed an R8 release-mode crash (`NullPointerException` on bitmap evaluation during `AnimatedVisibility` exit fade) in `ThumbnailImage`, ensuring stable production builds.
- **Back Navigation History Stack**: Implemented a comprehensive `tabBackStack` so pressing Back on custom subreddits or deeper pages returns sequentially to the previous tab rather than dumping the user directly to the first tab.

### 🔄 Changed
- **App-Wide Solid OLED Surface Unification**: Fully eliminated 1px wireframe border outlines across feeds, navigation bars, bottom sheets, player HUDs, chips, and dialogs in favor of modern, solid OLED surfaces (`#141414`, `#1E1E1E`, `#222222`).
- **Updated Reddit API Policy Advisory**: Updated in-app advisory notice and documentation to reflect Reddit's Developer Terms (defaulting standard accounts to a 0-app baseline), emphasizing that RdTube works out of the box with built-in defaults while personal overrides remain available for approved developers.
- **Optimized Content Area Spacing**: Reduced list bottom content padding from 150dp to 96dp following the search bar repositioning, significantly maximizing visible feed space.

---

### 📦 Downloads

| File | Description | Architecture |
| :--- | :--- | :---: |
| **`app-release.apk`** | **Official Release APK** for RdTube v1.0.7 | Universal (arm64-v8a, armeabi-v7a, x86_64) |
