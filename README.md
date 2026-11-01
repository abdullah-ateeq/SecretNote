# 🔐 SecretNote

**SecretNote** is a secure, modern, and feature-rich Android note-taking application built with **Jetpack Compose** and **Material 3**. It prioritizes user privacy and data security by leveraging Android's robust cryptographic libraries, biometric authentication, and local database encryption.

---

## ✨ Features

- **🔒 Advanced Security & Encryption**:
  - **Biometric Authentication**: Secure app access using fingerprint or facial recognition (`androidx.biometric`).
  - **Encrypted Local Storage**: Notes are encrypted securely using Android Keystore and `Security Crypto` (`EncryptedSharedPreferences` / custom cryptographic managers).
  - **App Lock**: Auto-lock protection when moving to the background or timing out.
- **🎨 Modern Material 3 UI**:
  - Clean, minimalist, and responsive user interface built entirely with **Jetpack Compose**.
  - Dynamic color support and seamless light/dark theme handling.
- **📝 Comprehensive Note Management**:
  - Create, edit, search, and delete notes effortlessly.
  - Advanced search bar with real-time filtering.
  - Flexible sorting options (by date, title, priority, etc.).
- **⚙️ Settings & Customization**:
  - Toggle biometric lock, manage security preferences, and configure app behavior.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow and clean separation of concerns.
- **Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) for structured local data.
- **Security**: 
  - `androidx.biometric` for authentication.
  - `androidx.security:security-crypto` for encrypted storage.
- **State & Navigation**:
  - Jetpack Navigation Compose
  - Kotlin Coroutines & Flow (`StateFlow`, `SharedFlow`)
- **Testing**: Comprehensive unit and instrumentation tests (`JUnit`, `Coroutines Test`, `Compose UI Testing`).

---

## 📂 Project Structure

```text
com.example.secretnote/
│
├── data/
│   ├── crypto/         # Encryption & Android Keystore management
│   ├── local/          # Room database, Entities, and DAOs
│   ├── preferences/    # App settings & DataStore repository
│   └── repository/     # Data repositories implementation
│
├── domain/
│   └── model/          # Domain models and business data classes
│
├── security/           # Biometric authentication & App Lock managers
│
├── ui/
│   ├── navigation/     # Nav graph and screen route definitions
│   ├── screens/        # Feature screens (Home, Editor, Auth, Settings, About, Splash)
│   │   ├── auth/
│   │   ├── editor/
│   │   ├── home/
│   │   ├── settings/
│   │   └── splash/
│   └── theme/          # Material 3 colors, typography, and themes
│
└── util/               # Helper utilities (Date formatting, etc.)
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala or newer recommended)
- **JDK 17**
- **Android SDK** (Compile SDK 37, Min SDK 26)

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/SecretNote.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the app on an emulator or a physical Android device supporting biometrics (API 26+).

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
