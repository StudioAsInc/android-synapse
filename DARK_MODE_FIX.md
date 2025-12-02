# Dark Mode Fix for AuthActivity

## Issue
AuthActivity was displaying light colors in dark mode, causing poor contrast and readability issues.

## Root Cause
The background gradient (`bg_auth_gradient.xml`) used hardcoded light color values that didn't adapt to dark mode:
- `auth_gradient_start`: `#F9FAEF` (light green-white)
- `auth_gradient_center`: `#E8F5E9` (light green)
- `auth_gradient_end`: `#C8E6C9` (light green)

## Solution
Changed gradient colors to use Material Design 3 theme attributes that automatically adapt to dark mode:

### Light Mode (values/colors.xml)
```xml
<color name="auth_gradient_start">@color/md_theme_background</color>
<color name="auth_gradient_center">@color/md_theme_surfaceContainerLow</color>
<color name="auth_gradient_end">@color/md_theme_surfaceContainer</color>
```

### Dark Mode (values-night/colors.xml)
```xml
<color name="auth_gradient_start">@color/md_theme_background</color>
<color name="auth_gradient_center">@color/md_theme_surfaceContainerLow</color>
<color name="auth_gradient_end">@color/md_theme_surfaceContainer</color>
<color name="glassmorphic_overlay">#F01A1C16</color>
<color name="password_weak">#EF5350</color>
<color name="password_fair">#FFA726</color>
<color name="password_strong">#66BB6A</color>
```

## Changes Made

### 1. `/app/src/main/res/values/colors.xml`
- Changed `auth_gradient_start` from hardcoded `#F9FAEF` to `@color/md_theme_background`
- Changed `auth_gradient_center` from hardcoded `#E8F5E9` to `@color/md_theme_surfaceContainerLow`
- Changed `auth_gradient_end` from hardcoded `#C8E6C9` to `@color/md_theme_surfaceContainer`

### 2. `/app/src/main/res/values-night/colors.xml`
- Added dark mode variants for auth gradient colors
- Added dark mode `glassmorphic_overlay` with darker tint
- Added dark mode password strength colors with better contrast

## Result
- AuthActivity now properly adapts to system dark mode
- Background gradient uses appropriate dark colors in dark mode
- Password strength indicators use darker, more visible colors
- Maintains proper contrast and readability in both modes

## Testing
Test the following scenarios:
1. Open AuthActivity in light mode - should show light gradient
2. Switch to dark mode - should show dark gradient
3. Toggle between sign in/sign up modes in both themes
4. Verify password strength indicator colors in dark mode
5. Check all text remains readable in both modes
