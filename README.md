<div align="center">
  <img src="https://i.postimg.cc/cCHjZYMf/20250906-224245.png" width="120" height="120" style="border-radius:50%">

  # **Synapse**
  
  **Express yourself in a better way ✨**

  [![Website](https://img.shields.io/badge/Website-Visit-blue)](https://dl-synapse.pages.dev)
  [![Docs](https://img.shields.io/badge/Docs-Read-green)](https://dl-synapse.pages.dev/docs)
  [![Report Bug](https://img.shields.io/badge/Report_Bug-Here-red)](https://github.com/StudioAsInc/android-synapse/issues/new?template=bug_report.md)
  [![Request Feature](https://img.shields.io/badge/Request_Feature-Here-yellow)](https://github.com/StudioAsInc/android-synapse/issues/new?template=feature_request.md)
  [![GitHub contributors](https://img.shields.io/github/contributors/StudioAsInc/android-synapse)](https://github.com/StudioAsInc/android-synapse/graphs/contributors)
  [![GitHub last commit](https://img.shields.io/github/last-commit/StudioAsInc/android-synapse)](https://github.com/StudioAsInc/android-synapse/commits/)
  [![Total downloads](https://img.shields.io/github/downloads/StudioAsInc/android-synapse/total)](https://github.com/StudioAsInc/android-synapse/releases)
  [![Repository Size](https://img.shields.io/github/repo-size/StudioAsInc/android-synapse)](https://github.com/StudioAsInc/android-synapse)

</div>

---

## 📚 Table of Contents
- [🚀 Introduction](#-introduction)
- [✨ Features](#-features)
- [🛠️ Tech Stack](#-tech-stack)
- [🏁 Getting Started](#-getting-started)
- [🤝 Contributing](#-contributing)
- [📜 License](#-license)
- [💬 Community & Support](#-community--support)
- [🙏 Acknowledgments](#-acknowledgments)

---

## 🚀 Introduction  
**Synapse** is an open-source social media platform built with Kotlin for Android, using Supabase as the backend. Designed with privacy, real-time communication, and a lightweight user experience at its core, Synapse provides a secure, ad-free environment where users can connect and express themselves freely.

**Architecture**: MVVM with Repository pattern, leveraging Kotlin coroutines for async operations and StateFlow for reactive UI updates.

> [!WARNING]
> The project is currently undergoing stabilization following a recent migration from Firebase to Supabase. Some features may be unstable as we complete the transition.

---

## ✨ Features
- **Real-time Communication**: Enjoy seamless chat, video calls (coming soon), and microblogging.
- **Zero Ads, Non-Profit Model**: We prioritize our users' experience over profits.
- **35GB+ Free Storage**: Ample space for your media, posts, and communities.
- **Lightweight & Optimized**: Runs smoothly on any device without compromising performance.
- **End-to-End Encrypted Chats**: Your conversations are private and secure.
- **Modular Architecture**: A flexible and scalable codebase that is easy to maintain and contribute to.

---

## 🛠️ Tech Stack

### Android App
- **Language**: Kotlin with Android KTX
- **Architecture**: MVVM + Repository pattern
- **UI**: ViewBinding, Material Design 3, Navigation Component
- **Async**: Kotlin Coroutines + Flow
- **Image Loading**: Glide
- **Markdown**: Markwon
- **Media**: Media3

### Backend (Supabase)
- **Database**: PostgreSQL via Postgrest
- **Authentication**: GoTrue (email, OAuth)
- **Storage**: Supabase Storage for media
- **Real-time**: Supabase Realtime for live updates

### Build Configuration
- Target SDK: 32
- Min SDK: 26
- Compile SDK: 36
- Build System: Gradle with Kotlin DSL

---

## 🏁 Getting Started

### Prerequisites
- **Android Studio** (latest stable version recommended)
- **JDK 11** or higher
- **Git**
- **Supabase Account** (for backend configuration)

### Installation

1. **Clone the repository**
   ```sh
   git clone https://github.com/StudioAsInc/android-synapse.git
   cd android-synapse
   ```

2. **Configure Supabase**
   
   Create a `local.properties` file in the project root (if it doesn't exist) and add your Supabase credentials:
   ```properties
   SUPABASE_URL=your_supabase_project_url
   SUPABASE_ANON_KEY=your_supabase_anon_key
   ```
   
   > **Note**: Never commit `local.properties` to version control. It's already in `.gitignore`.

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

4. **Sync and Build**
   - Let Gradle sync the project
   - Build the project: `Build > Make Project`
   - Run on emulator or device

### Project Structure
```
app/src/main/
├── java/com/studioas/synapse/
│   ├── auth/          # Authentication flows
│   ├── profile/       # User profiles
│   ├── feed/          # Home feed & posts
│   ├── chat/          # Messaging features
│   ├── data/          # Repositories & data sources
│   ├── models/        # Data models
│   └── utils/         # Utilities & extensions
└── res/               # Resources (layouts, drawables, etc.)
```

### Running Tests
```sh
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

---

## 🤝 Contributing
Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

For detailed contribution guidelines, please see our [Contribution Guide](Docs/CONTRIBUTE.md).

### How to Contribute
1. **Fork the Project**
2. **Create your Feature Branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit your Changes** (`git commit -m 'Add some AmazingFeature'`)
4. **Push to the Branch** (`git push origin feature/AmazingFeature`)
5. **Open a Pull Request**

---

## 📜 License  
Synapse is distributed under a [custom open-source license](Docs/LICENSE.md). See `Docs/LICENSE.md` for more information.

---

## 💬 Community & Support  
| Channel | Purpose |
|---------|---------|
| [GitHub Issues](https://github.com/StudioAsInc/android-synapse/issues) | Report bugs & request features |
| [Discussions](https://github.com/StudioAsInc/android-synapse/discussions) | Q&A and community talks |
| [Wiki](https://github.com/StudioAsInc/android-synapse/wiki) | Setup guides & documentation |

---

## 🙏 Acknowledgments  
- Our **core team** at StudioAs Inc.  
- **Open-source contributors** worldwide  
- **Early testers** shaping Synapse's future  

---

## ❓ FAQ

<details>
<summary><strong>Is Synapse completely free to use?</strong></summary>
<br>
Yes, Synapse is free and operates on a non-profit model. We do not have ads or premium features that require payment.
</details>

<details>
<summary><strong>What makes Synapse different from other social platforms?</strong></summary>
<br>
Synapse prioritizes user privacy and experience. We offer an ad-free environment, end-to-end encrypted chats, generous free storage (35GB+), and our platform is fully open-source.
</details>

<details>
<summary><strong>What platforms is Synapse currently available on?</strong></summary>
<br>
Synapse is available for Android and has a web version. You can find links to both on our official <a href="https://dl-synapse.pages.dev">website</a>.
</details>

<details>
<summary><strong>How is my privacy protected on Synapse?</strong></summary>
<br>
We use end-to-end encryption for all private chats, meaning only you and the recipient can read the messages. We are committed to minimizing data collection.
</details>

<details>
<summary><strong>Is the project stable?</strong></summary>
<br>
Currently, the project is undergoing a major backend migration from Firebase to Supabase, which may cause some instability. We are working hard to stabilize it.
</details>

<details>
<summary><strong>Can I contribute to the project?</strong></summary>
<br>
Absolutely! We welcome contributions from everyone. Please check out our <a href="Docs/CONTRIBUTE.md">Contribution Guide</a> to get started.
</details>

<details>
<summary><strong>What technology does Synapse use?</strong></summary>
<br>
The Android app is built with Kotlin using MVVM architecture, Kotlin Coroutines, and Material Design 3. The backend is powered by Supabase (PostgreSQL, Auth, Storage, Realtime).
</details>

<details>
<summary><strong>Can I host my own instance of Synapse?</strong></summary>
<br>
While self-hosting is a goal for the future, it is not officially supported at this time. The project's open-source nature, however, allows for community experimentation.
</details>

<details>
<summary><strong>Who is behind Synapse?</strong></summary>
<br>
Synapse is a project developed and maintained by <a href="https://studioas.dev">StudioAs Inc.</a> and a dedicated community of open-source contributors.
</details>

<details>
<summary><strong>Where can I report a bug or suggest a feature?</strong></summary>
<br>
You can report bugs or request new features by creating an issue on our <a href="https://github.com/StudioAsInc/android-synapse/issues">GitHub repository</a>.
</details>

---

<div align="center">
  
  **A Project by [StudioAs Inc.](https://studioas.dev)**  
  *"Empowering connections through transparency"*

  [⭐ Star on GitHub](https://github.com/StudioAsInc/android-synapse) • [Join Our Community](https://web-synapse.pages.dev) • [Contact](mailto:mashikahamed0@gmail.com)

</div>
