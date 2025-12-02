# Edge-to-Edge Quick Reference Guide

## What is Edge-to-Edge?
Edge-to-edge is a modern Android UI pattern where your app's content extends behind the system bars (status bar and navigation bar), creating an immersive, full-screen experience.

## Implementation in Android Synapse

### 1. BaseActivity
All activities in the app extend `BaseActivity`, which automatically enables edge-to-edge:

```kotlin
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    protected fun applyWindowInsets(rootView: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
```

### 2. Theme Configuration
Both light and dark themes are configured for edge-to-edge:

**Light Theme:**
- Transparent status and navigation bars
- Light status bar icons
- Light navigation bar icons
- No contrast enforcement

**Dark Theme:**
- Transparent status and navigation bars
- Dark status bar icons
- Dark navigation bar icons
- No contrast enforcement

### 3. Creating New Activities

#### Basic Activity
```kotlin
class MyNewActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_new)
    }
}
```

#### Activity with Window Insets Handling
```kotlin
class MyNewActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_new)
        
        // Apply insets to root view if content needs padding
        val rootView = findViewById<View>(R.id.root)
        applyWindowInsets(rootView)
    }
}
```

### 4. Layout Considerations

#### Using CoordinatorLayout
```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">
    
    <!-- Your content here -->
    
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### Using ConstraintLayout
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Add top padding for status bar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:fitsSystemWindows="true"
        app:layout_constraintTop_toTopOf="parent" />
    
    <!-- Your content here -->
    
</androidx.constraintlayout.widget.ConstraintLayout>
```

### 5. Common Patterns

#### AppBar with Edge-to-Edge
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar_layout)
    appBarLayout.fitsSystemWindows = true
}
```

#### Bottom Navigation with Edge-to-Edge
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
    ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(0, 0, 0, systemBars.bottom)
        insets
    }
}
```

### 6. Testing Edge-to-Edge

1. **Light Theme**: Check that status bar icons are dark on light background
2. **Dark Theme**: Check that status bar icons are light on dark background
3. **Navigation**: Verify navigation bar is transparent
4. **Content**: Ensure content doesn't overlap with system bars
5. **Gestures**: Test gesture navigation on devices that support it

### 7. Troubleshooting

#### Content Hidden Behind Status Bar
```kotlin
// Apply window insets to your root view
val rootView = findViewById<View>(R.id.root)
applyWindowInsets(rootView)
```

#### Wrong Status Bar Icon Color
Check your theme's `android:windowLightStatusBar` attribute:
- `true` for light theme (dark icons)
- `false` for dark theme (light icons)

#### Navigation Bar Not Transparent
Ensure your theme has:
```xml
<item name="android:navigationBarColor">@android:color/transparent</item>
<item name="android:enforceNavigationBarContrast">false</item>
```

## Benefits
- ✅ Modern, immersive UI
- ✅ More screen space for content
- ✅ Follows Material Design 3 guidelines
- ✅ Better user experience
- ✅ Consistent across all screens
