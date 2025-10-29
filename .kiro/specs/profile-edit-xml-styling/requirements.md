# Requirements Document

## Introduction

This document outlines the requirements for refactoring the ProfileEditActivity to use XML-based styling instead of programmatic styling in Kotlin code. Currently, the edit profile screen applies visual styling (backgrounds, strokes, corner radius, colors) through Java/Kotlin code using GradientDrawable and other drawable manipulation. This approach violates Android best practices and makes the code harder to maintain, test, and theme properly. The refactoring will move all styling to XML drawable resources and use theme attributes instead of hardcoded color values.

## Glossary

- **ProfileEditActivity**: The Android Activity class that handles the user interface for editing user profile information
- **GradientDrawable**: An Android drawable class used programmatically to create shapes with gradients, strokes, and corner radii
- **XML Drawable**: A drawable resource defined in XML format in the res/drawable directory
- **Theme Attribute**: A reference to a color or style defined in the app's theme (e.g., ?attr/colorSurface)
- **Hardcoded Color**: A color value directly specified in code using hex notation (e.g., 0xFFEEEEEE)
- **Material Design**: Google's design system that provides guidelines for Android UI components
- **ViewBinding**: Android's type-safe way to reference views without findViewById

## Requirements

### Requirement 1

**User Story:** As a developer, I want all visual styling to be defined in XML resources, so that the code is more maintainable and follows Android best practices

#### Acceptance Criteria

1. WHEN THE ProfileEditActivity initializes views, THE ProfileEditActivity SHALL NOT apply any background drawables programmatically
2. WHEN THE ProfileEditActivity initializes views, THE ProfileEditActivity SHALL NOT call methods that create GradientDrawable instances
3. WHEN THE ProfileEditActivity initializes views, THE ProfileEditActivity SHALL reference XML drawable resources for all view backgrounds
4. WHEN THE ProfileEditActivity needs to change view appearance based on state, THE ProfileEditActivity SHALL use state list drawables defined in XML
5. THE ProfileEditActivity SHALL NOT contain utility methods for creating drawables programmatically (createGradientDrawable, createStrokeDrawable, viewGraphics)

### Requirement 2

**User Story:** As a developer, I want all colors to use theme attributes instead of hardcoded values, so that the app properly supports light and dark themes

#### Acceptance Criteria

1. THE XML drawable resources SHALL NOT contain hardcoded color values in hex format
2. THE XML drawable resources SHALL reference theme attributes for all color values (e.g., ?attr/colorSurface, ?attr/colorOnSurface)
3. WHEN THE app theme changes between light and dark mode, THE ProfileEditActivity views SHALL automatically update their colors
4. THE ProfileEditActivity Kotlin code SHALL NOT contain any hardcoded color values in hex format (0xFFFFFFFF, 0xFFEEEEEE, etc.)
5. THE XML layout file SHALL NOT contain hardcoded color values for any view attributes

### Requirement 3

**User Story:** As a developer, I want input field error states to be handled through XML state list drawables, so that the styling is consistent and declarative

#### Acceptance Criteria

1. WHEN THE username input has an error, THE username input field SHALL display a red stroke using an XML state list drawable
2. WHEN THE nickname input has an error, THE nickname input field SHALL display a red stroke using an XML state list drawable
3. WHEN THE biography input has an error, THE biography input field SHALL display a red stroke using an XML state list drawable
4. THE ProfileEditActivity SHALL NOT programmatically change input field backgrounds in error handling methods
5. THE error state styling SHALL be defined in XML drawable resources with error state selectors

### Requirement 4

**User Story:** As a developer, I want all rounded corners and strokes to be defined in XML shape drawables, so that the visual design is centralized and reusable

#### Acceptance Criteria

1. THE input fields (username, nickname, biography) SHALL use XML shape drawables with 28dp corner radius and 3dp stroke
2. THE gender selection container SHALL use XML shape drawables with 28dp corner radius and 3dp stroke
3. THE region selection container SHALL use XML shape drawables with 28dp corner radius and 3dp stroke
4. THE profile image history container SHALL use XML shape drawables with 28dp corner radius and 3dp stroke
5. THE cover image history container SHALL use XML shape drawables with 28dp corner radius and 3dp stroke
6. THE profile image card SHALL use XML shape drawables with 300dp corner radius (circular)
7. THE profile relative card SHALL use XML shape drawables with 28dp corner radius

### Requirement 5

**User Story:** As a developer, I want the code to be cleaner and more focused on business logic, so that it is easier to understand and maintain

#### Acceptance Criteria

1. THE ProfileEditActivity initializeLogic method SHALL NOT contain any drawable creation or styling code
2. THE ProfileEditActivity SHALL NOT contain more than 5 lines of code related to view styling in the initialize method
3. WHEN THE ProfileEditActivity is refactored, THE total lines of code SHALL decrease by at least 50 lines
4. THE ProfileEditActivity SHALL focus on business logic (validation, data loading, saving) without visual styling concerns
5. THE XML layout file SHALL reference all drawable resources using @drawable/ syntax
