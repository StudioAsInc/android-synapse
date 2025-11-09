# 🤝 Contributing to Synapse

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

## Code Style Requirements

To maintain code quality and consistency, please follow these guidelines:

- **Kotlin Style Guide**: Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
  - Use meaningful variable and function names
  - Prefer `val` over `var` when possible
  - Use data classes for models
  - Leverage Kotlin extensions and Android KTX
  
- **ViewBinding**: All UI code must use ViewBinding (no `findViewById` or synthetic imports)

- **Coroutines for Async Operations**: Use Kotlin coroutines instead of callbacks

- **MVVM Architecture Pattern**: Follow the established architecture
  - ViewModels manage UI state using StateFlow/LiveData
  - Repositories abstract data layer (Supabase, local storage)
  - Separate UI logic from business logic
  - Use lifecycle-aware components

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
