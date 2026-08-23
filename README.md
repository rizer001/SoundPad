# 🔊 Soundpad

**Free, open-source soundpad for gamers and streamers.**

Soundpad is a lightweight, cross-platform soundboard application designed for ease of use. Play sounds instantly with hotkeys, organize your sounds into categories, and route audio to any application.

## ✨ Features

- 🎵 **Multi-format support** — MP3, WAV, OGG, FLAC, M4A, AAC, WMA
- ⌨️ **Global hotkeys** — Play sounds even when the app is minimized
- 📂 **Categories** — Organize sounds into groups (Memes, Alerts, Music, etc.)
- 📋 **Presets** — Save and load sound collections as JSON files
- 🔊 **Master volume** — Control overall output volume
- 🎤 **Virtual audio cable** — Route audio to Discord, OBS, Teams, etc.
- 🌙 **Dark/Light theme** — Choose your preferred look
- 🔍 **Search** — Instantly find sounds by name
- 📦 **Drag & drop** — Add sounds by dropping files into the app
- 💾 **Lightweight** — ~30MB RAM usage, fast startup

## 🚀 Getting Started

### Prerequisites

- **Java 17+** (JDK)
- **Gradle 8+**

### Build

```bash
cd Soundpad
./gradlew build
```

### Run

```bash
./gradlew run
```

### Package

```bash
# Windows (MSI/EXE)
./gradlew packageMsi
./gradlew packageExe

# Linux (DEB/RPM)
./gradlew packageDeb
./gradlew packageRpm
```

## 📁 Project Structure

```
Soundpad/
├── src/main/kotlin/com/rizer01/soundpad/
│   ├── Main.kt                    # Entry point
│   ├── audio/                     # Audio playback engine
│   │   └── AudioPlayer.kt        # Sound player with FFmpeg support
│   ├── hotkey/                    # Global hotkey management
│   │   └── HotkeyManager.kt      # JNativeHook integration
│   ├── model/                     # Data models
│   │   └── Models.kt             # SoundFile, Category, Preset, Settings
│   ├── store/                     # State management
│   │   ├── SoundStore.kt         # Sound library state
│   │   ├── PresetStore.kt        # Preset persistence
│   │   └── SettingsStore.kt      # Settings persistence
│   └── ui/                        # Compose UI
│       ├── SoundpadApp.kt        # Main app composable
│       ├── theme/                # Material 3 theme
│       │   ├── Color.kt
│       │   └── Theme.kt
│       └── components/           # UI components
│           ├── SoundGrid.kt
│           ├── SoundCard.kt
│           ├── Sidebar.kt
│           ├── SearchBar.kt
│           ├── StatusBar.kt
│           ├── SettingsDialog.kt
│           ├── HotkeyEditor.kt
│           └── AddSoundDialog.kt
├── src/main/resources/
│   ├── logback.xml               # Logging config
│   └── icons/                    # App icons
├── build.gradle.kts
└── LICENSE                       # AGPLv3
```

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.1 |
| UI | Compose Desktop + Material 3 |
| Audio | javax.sound + FFmpeg (javacv) |
| Hotkeys | JNativeHook |
| Serialization | kotlinx.serialization |
| Build | Gradle Kotlin DSL |

## 📄 License

This project is licensed under the **GNU Affero General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

- **GitHub:** [rizer01](https://github.com/rizer01)
- **Discord:** [Join our server](https://discord.gg/rizer01)
