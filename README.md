# RdTube

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/rdtube_banner_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="docs/images/rdtube_banner_light.svg">
  <img alt="RdTube Banner" src="docs/images/rdtube_banner_light.svg">
</picture>

[![Download](docs/badges/download.svg)](https://github.com/LeanBitLab/RdTube/releases/latest) [![Downloads](docs/badges/downloads.svg)](https://github.com/LeanBitLab/RdTube/releases) [![Stars](docs/badges/stars.svg)](https://github.com/LeanBitLab/RdTube/stargazers)

**RdTube** is a sleek, privacy-conscious video browsing client for Reddit. Designed with Jetpack Compose and Media3 ExoPlayer, it provides a fast, hassle-free YouTube Shorts-style video experience out of the box — **no API keys required, and no user account needed.**

## Screenshots

<table>
  <tr>
    <td><img src="docs/images/R1.png" height="500" alt="Explore Feed"/></td>
    <td><img src="docs/images/R2.png" height="500" alt="Video Player"/></td>
    <td><img src="docs/images/R3.png" height="500" alt="Search & Subscriptions"/></td>
    <td><img src="docs/images/R4.png" height="500" alt="About & Settings"/></td>
    <td><img src="docs/images/R5.png" height="500" alt="Features & Menu"/></td>
  </tr>
</table>

## Why RdTube?

- **🔑 No API Key Required** - Works instantly right after installation with zero setup or API client registration.
- **👤 No Account Required** - 100% anonymous browsing out of the box with zero sign-in or Reddit credentials required.
- **📱 Single-Column & Vertical Pager Feeds** - Smooth vertical video scrolling with instant background prefetching.
- **🎬 Media3 ExoPlayer Engine** - Integrated video player supporting dynamic playback speed, quality selector, and automatic lifecycle pause.
- **🔁 Auto-Play & Auto-Rotate** - Toggle continuous auto-next video playback and sensor orientation lock.
- **⬇️ Video Downloads** - Save Reddit videos directly to your device storage with audio-video merging.
- **🎛️ Volume & Brightness Gestures** - Intuitive edge drag gesture controls for volume and brightness.
- **⚡ Parallel Coroutine Fetching** - High-throughput multi-subreddit API requests (3x–5x faster feed load times).
- **🔍 Subreddit Search & Subscriptions** - Search subreddits instantly and manage custom subscriptions stored 100% locally.
- **📜 Local Persistence & History** - Watch history, liked videos, and preferences stay on your device.
- **🎨 Obsidian Dark Theme** - Glassmorphism UI, vibrant crimson accents (`#FF2A4B`), and rounded edge styling.

## Download

<table border="0">
  <tr>
    <td align="center" valign="middle">
      <a href="https://github.com/LeanBitLab/RdTube/releases/latest">
        <img alt="Get it on GitHub" src="https://raw.githubusercontent.com/LeanBitLab/RdTube/main/docs/images/get-it-on-github.png" height="90">
      </a>
    </td>
    <td align="center" valign="middle">
      <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/LeanBitLab/RdTube">
        <img alt="Get it on Obtainium" src="https://raw.githubusercontent.com/LeanBitLab/RdTube/main/docs/images/get-it-on-obtainium.png" height="60">
      </a>
    </td>
  </tr>
</table>

## Setup & Building

### Prerequisites
- **Android Studio**: Ladybug / Jellyfish or latest stable
- **JDK**: Java 17
- **Android SDK**: API 36 (Min SDK: 24)

### Quick Start
```bash
# Clone the repository
git clone https://github.com/LeanBitLab/RdTube.git
cd RdTube

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew test
```

## Contributing

Please report any issues or suggestions directly in our repository: [🐛 Open an Issue](https://github.com/LeanBitLab/RdTube/issues)

## License

RdTube is licensed under **GNU General Public License v3.0**.

See [LICENSE](/LICENSE) file.

## Credits

- Built with ❤️ by [LeanBitLab](https://github.com/LeanBitLab)

## 🛡️ LeanBitLab Ecosystem

Check out our other projects:
👉 **[LeanBitLab Projects](https://github.com/LeanBitLab#-current-projects)**

---

## 💖 Support the Development

Building and maintaining privacy-focused, high-performance apps takes significant time and effort. 

If RdTube makes your daily video browsing experience better, please consider supporting our development! Your sponsorship directly helps us keep the project **100% free, open-source, and independent**.

<div align="left">
  <a href="https://github.com/sponsors/LeanBitLab">
    <img src="https://img.shields.io/static/v1?label=Sponsor&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86" style="height: 40px;" alt="Sponsor on GitHub"/>
  </a>
</div>

---

*RdTube • Privacy-focused video client for Reddit*
