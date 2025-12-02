# 🤝 Contributing to Synapse

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

## Code Style Requirements

To maintain code quality and consistency, please follow these guidelines:

- **Kotlin Style Guide**: Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
  - Use meaningful variable and function names
  - Prefer `val` over `var` when possible
  - Use data classes for models
  - Leverage Kotlin extensions and Android KTX
  
- **UI Toolkit**: Use ViewBinding for XML-based layouts, or Jetpack Compose for new UI components. Avoid `findViewById` or synthetic imports.

- **Coroutines for Async Operations**: Use Kotlin coroutines instead of callbacks

- **MVVM Architecture Pattern**: Follow the established architecture
  - ViewModels manage UI state using StateFlow/LiveData
  - Repositories abstract data layer (Supabase, local storage)
  - Separate UI logic from business logic
  - Use lifecycle-aware components

## Setting up your Development Environment

### Prerequisites
- **Android Studio** (latest stable version recommended)
- **JDK 17** or higher
- **Git**
- **Supabase Account** (for backend configuration)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/StudioAsInc/android-synapse.git
   cd android-synapse
   ```

2. **Configure Supabase**

   Synapse requires Supabase credentials for backend connectivity. You can configure these in two ways:

   **Option 1: gradle.properties (Recommended for local development)**

   Create or edit `gradle.properties` in the project root:

   ```properties
   SUPABASE_URL=your_supabase_project_url
   SUPABASE_ANON_KEY=your_supabase_anon_key
   SUPABASE_SYNAPSE_S3_ENDPOINT_URL=your_s3_endpoint
   SUPABASE_SYNAPSE_S3_ENDPOINT_REGION=your_s3_region
   SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID=your_s3_access_key_id
   SUPABASE_SYNAPSE_S3_ACCESS_KEY=your_s3_access_key
   ```

   **Option 2: Environment Variables (Recommended for CI/CD)**

   Set the same variables as environment variables in your system.

   > **Security Note**: Never commit credentials to version control. Both `gradle.properties` and `local.properties` are in `.gitignore`.

   **Getting Supabase Credentials**:
   1. Create a free account at [supabase.com](https://supabase.com)
   2. Create a new project
   3. Go to Project Settings → API
   4. Copy your Project URL and anon/public key
   5. For S3 storage, configure in Project Settings → Storage

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

4. **Sync and Build**
   - Let Gradle sync the project
   - Build the project: `Build > Make Project`
   - Run on emulator or device

### Project Structure

```plaintext
app/src/main/
├── java/com/synapse/social/studioasinc/
│   ├── adapter/           # RecyclerView adapters
│   ├── backend/           # Supabase service layer
│   ├── chat/              # Chat feature components
│   ├── data/              # Repositories & data sources
│   ├── domain/            # Business logic
│   ├── fragments/         # Fragment components
│   ├── home/              # Home feed features
│   ├── model/models/      # Data models
│   ├── presentation/      # ViewModels
│   ├── util/              # Utilities & extensions
│   ├── widget/            # Custom views
│   ├── *Activity.kt       # Activity classes
│   └── SynapseApp.kt      # Application class
└── res/                   # Resources (layouts, drawables, etc.)
```

### Running Tests

```bash
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## Development Workflow

1. **Fork the Project** from the main repository
2. **Create your Feature Branch**
   
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. **Make your Changes**
   - Write clean, well-documented code
   - Follow the code style requirements above
   - Test your changes thoroughly
   
4. **Commit your Changes**
   
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
   
   - Use clear, descriptive commit messages
   - Reference issue numbers when applicable (e.g., "Fix #123: Resolve chat crash")

5. **Push to the Branch**
   
   ```bash
   git push origin feature/AmazingFeature
   ```

6. **Open a Pull Request**

## Pull Request Requirements

Before submitting a PR, ensure you have:

- [ ] **Clear Description**: Explain what changes you made and why
- [ ] **Issue Reference**: Link to related issues (e.g., "Closes #123")
- [ ] **Testing**: Describe how you tested your changes
- [ ] **Screenshots**: Include screenshots for UI changes
- [ ] **Code Quality**: Ensure code follows style guidelines
- [ ] **Build Success**: Verify the project builds without errors
- [ ] **No Breaking Changes**: Or clearly document them if necessary

## Code Review Process

1. A maintainer will review your PR within a few days
2. Address any feedback or requested changes
3. Once approved, a maintainer will merge your PR
4. Your contribution will be included in the next release

Thank you for contributing to Synapse! 🎉
