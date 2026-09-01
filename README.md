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

[Screenshots](#-screenshots) • [Download](#-download) • [Features](#-features) • [Setup & Building](#-setup--building) • [Notes & Privacy](#-notes--privacy) • [LeanBitLab Projects](https://github.com/LeanBitLab#-android-projects)

</div>

---

## 🚀 Overview

**RdTube** is a modern, privacy-respecting video client for Reddit crafted in 100% Kotlin & Jetpack Compose. Engineered with a hardware-accelerated Media3 ExoPlayer engine, RdTube turns Reddit video browsing into a high-speed, clutter-free YouTube Shorts-style experience.

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

- **Media3 ExoPlayer Engine**: Silky-smooth 60fps streaming with dynamic prefetching and lifecycle-aware memory management.
- **Continuous Edge Gestures**: Ultra-smooth vertical drag sliders for screen brightness (left) and media volume (right).
- **Playback Controls**: Single-video loop toggle, auto-next advance, playback speed (0.5x–2.0x), and resolution selection.
- **Direct Video Export**: Download full MP4 videos directly to device storage with merged high-bitrate audio.
- **High-Density Video Feeds**: Parallel coroutine fetching across top video subreddits for immediate 25+ card feeds.
- **Comprehensive Feed Sorting**: 1-tap sheet supporting Hot, New, Rising, and Top (Today, Week, Month, Year, All-Time).
- **Segmented Search & Pagination**: Search communities or Reddit-wide video clips with live suggestions and infinite scroll.
- **5-Slot Floating Dock**: Snappy navigation bar with 850f spring physics (Explore, Subs, Search, Library, About).
- **Double-Back Exit**: Intuitive swipe/press-back-twice safety mechanism to prevent accidental app exits.
- **Intelligent Cache Freshness**: Instant zero-latency cached feed display with 5-minute freshness management.
- **Pitch Black OLED Aesthetic**: Pure monochrome black theme optimized for battery saving on AMOLED displays.
- **100% Strictly Local Storage**: Likes, watch history, and subscriptions are saved locally in private storage—never tracked or uploaded.

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

---

## 🛠️ Setup & Building

### Prerequisites
- **Android Studio**: Ladybug / Meerkat or newer
- **JDK**: Java 17 or higher
- **Android SDK**: API 36 (Minimum Supported: API 24 / Android 7.0+)

### Building from Source
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

## 📝 Notes & Privacy

- **100% Community Funded**: RdTube is completely independent, free, and open-source with zero advertisements, sponsors, or analytics.
- **Official Read-Only OAuth 2.0**: Unrestricted and mature subreddits require Reddit authentication. RdTube uses standard read-only scopes (`identity`, `read`, `mysubreddits`) directly in your browser. Passwords are never seen or stored.
- **Zero Cloud Syncing**: Connecting an account serves solely to authenticate API media streaming. It never modifies, overwrites, or syncs your Reddit upvotes, saved items, or history.
- **Account Usage Precaution**: While read-only OAuth is standard under Reddit's developer API, Reddit policies can change. If you prefer extra caution, you can use a secondary Reddit account or browse 100% anonymously without logging in.
- **In-App OTA Updates**: Built-in GitHub Releases update checker in the About page notifies you of new versions and downloads APKs with a single tap.

---

## 📱 More Android Projects by LeanBitLab

Discover our complete suite of privacy-first, open-source Android applications:  
👉 **[Explore All LeanBitLab Android Projects](https://github.com/LeanBitLab#-android-projects)**

---

## 🤝 Community & Contributing

- **Bug Reports & Issues**: [Open an Issue on GitHub](https://github.com/LeanBitLab/RdTube/issues)
- **Official Telegram Channel**: [@LeanBitLab](https://t.me/LeanBitLab)
- **Reddit Community**: [r/LeanBitLab_](https://www.reddit.com/r/LeanBitLab_/)
- **X (Twitter)**: [@LeanBitLab](https://x.com/LeanBitLab)
- **YouTube**: [@LeanBitLab](https://www.youtube.com/@LeanBitLab)
- **Website**: [leanbitlab.github.io](https://leanbitlab.github.io/LeanBitLab/)

---

## 💖 Support the Project

If RdTube improves your daily video browsing, please consider supporting our independent development:

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
