### 💖 Support Our Work

As an open-source, community-funded project, we operate on a very limited budget. If RdTube helps you daily, please consider supporting us on [GitHub Sponsors](https://github.com/sponsors/LeanBitLab) or [Open Collective](https://opencollective.com/leanbitlab-org). Sharing RdTube with friends and family makes a huge difference!

## RdTube 1.0.9

---

### Changed
- **Auto-Hiding Search Chrome (#26)**: The search bar, tabs and navigation dock now hide while you scroll search results and return when you scroll back.
- **Cleaner Player Controls (#26)**: The center play/pause button hides after 1.5 seconds while the progress bar stays visible for 4 seconds.
- **Faster Navigation and Animations (#26)**: Going back from the player no longer uses the slow 700 ms default slide, and animations are shorter across the app.
- **Lower Memory and Disk Use (#26)**: Saved feed caches are now capped instead of growing forever, in-memory caches are bounded, and duplicate network requests are reduced.
- **Smoother Playback Gestures (#26)**: The brightness gesture now saves its level once per swipe instead of on every frame, and watch and like history is saved off the main thread.

### Fixed
- **Share Button Cut Off (#26)**: The player quick-action row no longer cuts off the share button on narrow screens (reported on Samsung Galaxy S26, One UI 9, Android 17).
- **Search Results Wiped While Typing (#26)**: Typing fast in search no longer clears the results already shown.
- **Stale Feed Responses (#26)**: Switching subreddit or sort no longer lets an old response overwrite the current feed, and a race on feed paging cursors is fixed.
- **Download Cleanup (#26)**: Downloads now remove temporary files and failed entries.

---

### Downloads

| File | Description | Architecture |
| :--- | :--- | :---: |
| **`app-release.apk`** | Release APK for RdTube v1.0.9 | Universal (arm64-v8a, armeabi-v7a, x86_64) |
