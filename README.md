# RdTube

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/rdtube_banner_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/images/rdtube_banner_light.svg">
  <img alt="RdTube Banner" src="docs/images/rdtube_banner_light.svg">
</picture>

<div align="center">

[![Latest Release](https://img.shields.io/github/v/release/LeanBitLab/RdTube?style=flat-square&color=4f46e5&label=Release)](https://github.com/LeanBitLab/RdTube/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/LeanBitLab/RdTube/total?style=flat-square&color=059669&label=Downloads)](https://github.com/LeanBitLab/RdTube/releases)
[![Stars](https://img.shields.io/github/stars/LeanBitLab/RdTube?style=flat-square&color=d97706&label=Stars)](https://github.com/LeanBitLab/RdTube/stargazers)
[![License: GPL v3](https://img.shields.io/badge/License-GPL_v3-blue.svg?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0)
[![Sponsor](https://img.shields.io/badge/Sponsor-LeanBitLab-db2777?style=flat-square&logo=githubsponsors&logoColor=white)](https://github.com/sponsors/LeanBitLab)
[![Donate on Open Collective](https://img.shields.io/badge/Donate-Open_Collective-1f6feb?style=flat-square&logo=opencollective&logoColor=white)](https://opencollective.com/leanbitlab-org)

**A private, fast, and sleek open-source video client for Reddit.**  
*Pure OLED pitch black, high-speed feed prefetching, and zero tracking.*

[Screenshots](#-screenshots) • [Download](#-download) • [Features](#-features) • [Authentication & Security](#-authentication--security) • [Setup Guide](#-setup--building) • [LeanBitLab Projects](https://github.com/LeanBitLab#-android-projects)

</div>

---

## 🚀 Overview

**RdTube** is a modern, privacy-respecting video client for Reddit crafted in 100% Kotlin & Jetpack Compose. Engineered with a hardware-accelerated Media3 ExoPlayer engine, RdTube turns Reddit video browsing into a high-speed, clutter-free YouTube Shorts-style experience.

Browse anonymously without creating accounts or registering API keys, or connect securely via official Reddit OAuth to unlock unrestricted access across your favorite subreddits.

---

## 📸 Screenshots

<table>
  <tr>
    <td><img src="docs/images/R1.png" width="180" alt="Explore Feed"/></td>
    <td><img src="docs/images/R2.png" width="180" alt="Video Player"/></td>
    <td><img src="docs/images/R3.png" width="180" alt="Search & Subscriptions"/></td>
    <td><img src="docs/images/R4.png" width="180" alt="About & Settings"/></td>
    <td><img src="docs/images/R5.png" width="180" alt="Features & Menu"/></td>
  </tr>
</table>

---

## ✨ Features

### 🎬 Media3 Video Playback Engine
- **Hardware-Accelerated ExoPlayer**: Silky-smooth 60fps streaming with dynamic buffer prefetching and lifecycle-aware memory reclamation.
- **Continuous Edge Gestures**: Ultra-smooth vertical edge drag sliders for brightness (left) and media volume (right).
- **Playback Flexibility**: Seamless single-video loop toggle, auto-next clip advance, dynamic speed control (0.5x–2.0x), and quality resolution selection.
- **Direct Video Export**: Download full MP4 videos directly to your device storage with merged high-bitrate audio.

### ⚡ Feed Performance & Architecture
- **Instant Parallel Coroutines**: High-throughput multi-subreddit API requests loading video streams 3x–5x faster than standard web clients.
- **Dynamic Sort Controls**: 1-tap sort trigger at the top right supporting Hot, New, Top (Day/Week/Month/Year/All-time), and Rising.
- **5-Slot Floating Navigation Dock**: High-speed bottom pill dock with 850f spring physics for instant switching between Explore, Subs, Search, Library, and About.
- **Dedicated Search Page**: Quick search subreddits with live suggestions, trending lists, and local search history.

### 🔒 Privacy, Security & Local Persistence
- **100% Anonymous & Zero Tracking**: No account required to browse standard feeds.
- **100% Strictly Local Storage**: Liked videos, watch history, and subscriptions are stored locally in private device storage (`SharedPreferences`). They are **never** synced, uploaded, or sent to Reddit's online servers or any cloud database.
- **Pure OLED Pitch Black Aesthetics**: High-contrast monochromatic UI built for battery efficiency on AMOLED and OLED panels.

---

## 🔐 Authentication & Security

### Official OAuth 2.0 Authorization (Read-Only)
Reddit requires user account authorization for unmoderated or age-restricted media streams. RdTube provides a seamless 1-tap authorization flow:
- **Official Read-Only Scopes**: Uses Reddit's official OAuth 2.0 endpoint with read-only permissions (`identity`, `read`, `mysubreddits`).
- **Zero Account Syncing**: Logging in is strictly for token authorization to fetch mature/unrestricted media from Reddit API. It never alters, overwrites, or syncs with your online Reddit upvotes, saved items, or history.
- **Zero Password Exposure**: Authentication happens entirely inside your browser directly on Reddit servers; RdTube never sees or stores your password.
- **Zero Account Ban Risk**: Third-party client usage is standard and permitted under Reddit's developer API terms.

---

## 📥 Download

<table border="0">
  <tr>
    <td align="center" valign="middle">
      <a href="https://github.com/LeanBitLab/RdTube/releases/latest">
        <img alt="Get it on GitHub" src="docs/images/get-it-on-github.png" height="80">
      </a>
    </td>
    <td align="center" valign="middle">
      <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/LeanBitLab/RdTube">
        <img alt="Get it on Obtainium" src="docs/images/get-it-on-obtainium.png" height="55">
      </a>
    </td>
  </tr>
</table>

> [!TIP]
> **Automatic Updates**: RdTube features a built-in GitHub Releases OTA updater in the About page to notify you of new versions and download updates with a single tap.

---

## 🛠️ Setup & Building

### Prerequisites
- **Android Studio**: Ladybug / Meerkat or latest stable
- **JDK**: Java 17 or higher
- **Android SDK**: API 36 (Minimum Supported SDK: 24 / Android 7.0+)

### Quick Build
```bash
# Clone the repository
git clone https://github.com/LeanBitLab/RdTube.git
cd RdTube

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test
```

---

## 📱 More Android Projects by LeanBitLab

Discover our complete suite of privacy-first, open-source Android applications:  
👉 **[Explore All LeanBitLab Android Projects](https://github.com/LeanBitLab#-android-projects)**

---

## 🤝 Community & Contributing

We welcome contributions, bug reports, and suggestions!
- **Bug Reports & Issues**: [Open an Issue on GitHub](https://github.com/LeanBitLab/RdTube/issues)
- **Official Telegram Channel**: [@LeanBitLab](https://t.me/LeanBitLab)
- **Reddit Community**: [r/LeanBitLab_](https://www.reddit.com/r/LeanBitLab_/)
- **X (Twitter)**: [@LeanBitLab](https://x.com/LeanBitLab)
- **YouTube**: [@LeanBitLab](https://www.youtube.com/@LeanBitLab)
- **Official Website**: [leanbitlab.github.io](https://leanbitlab.github.io/LeanBitLab/)

---

## 💖 Support the Project

Building and maintaining privacy-focused, high-performance open-source applications requires continuous development, testing across multiple Android devices, and infrastructure.

If RdTube improves your daily video browsing, please consider sponsoring our work!

<div align="left">
  <a href="https://github.com/sponsors/LeanBitLab">
    <img src="https://img.shields.io/static/v1?label=Sponsor%20on%20GitHub&message=%E2%9D%A4&logo=GitHub&color=%23db2777" height="38" alt="Sponsor LeanBitLab on GitHub"/>
  </a>
  &nbsp;&nbsp;
  <a href="https://opencollective.com/leanbitlab-org">
    <img src="https://img.shields.io/static/v1?label=Donate%20on&message=Open%20Collective&logo=opencollective&logoColor=white&color=%231f6feb" height="38" alt="Donate to LeanBitLab on Open Collective"/>
  </a>
</div>

---

## ⚖️ License

RdTube is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.  
See the [LICENSE](LICENSE) file for details.
