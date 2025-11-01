# Tutorial Missions - Quick Reference Card

## 🚀 Quick Start (3 Steps)

```kotlin
// 1. Launch Tutorial UI
startActivity(Intent(this, TutorialActivity::class.java))

// 2. Track User Actions
getTutorialTracker().trackNavigation("HomeActivity")
getTutorialTracker().trackPostCreated()

// 3. Observe Progress
TutorialManager.getInstance(context).userProgress.collect { progress ->
    println("Level ${progress.getLevel()}, XP: ${progress.totalXp}")
}
```

## 📍 Common Tracking Calls

### Navigation
```kotlin
getTutorialTracker().trackNavigation("HomeFragment")
getTutorialTracker().trackNavigation("ChatActivity")
getTutorialTracker().trackNavigation("InboxChatsFragment")
getTutorialTracker().trackNavigation("NotificationsFragment")
getTutorialTracker().trackNavigation("CreatePostActivity")
getTutorialTracker().trackNavigation("SearchActivity")
getTutorialTracker().trackNavigation("ReelsFragment")
```

### Data Creation
```kotlin
getTutorialTracker().trackPostCreated()
getTutorialTracker().trackCommentCreated()
getTutorialTracker().trackMessageSent()
getTutorialTracker().trackProfilePhotoUpdated()
getTutorialTracker().trackBioUpdated()
getTutorialTracker().trackCoverPhotoUpdated()
```

### Feature Usage
```kotlin
getTutorialTracker().trackPostLiked(totalLikes)
getTutorialTracker().trackUserFollowed(totalFollows)
getTutorialTracker().trackPostSaved()
getTutorialTracker().trackImageSent()
getTutorialTracker().trackReactionAdded()
getTutorialTracker().trackHashtagSearch()
```

### Advanced Features
```kotlin
getTutorialTracker().trackMarkdownPostCreated()
getTutorialTracker().trackPollCreated()
getTutorialTracker().trackPostScheduled()
getTutorialTracker().trackPrivacySettings()
```

## 🎮 Manager API

```kotlin
val manager = TutorialManager.getInstance(context)

// Start mission
manager.startMission("mission_id")

// Complete step
manager.completeStep("mission_id", "step_id")

// Get missions
manager.getMissionsByCategory(MissionCategory.SOCIAL)
manager.getCompletedMissions()
manager.getInProgressMissions()
manager.getAvailableMissions()

// Reset (testing only)
manager.resetAllMissions()

// Observe state
manager.missions.collect { missions -> }
manager.currentMission.collect { mission -> }
manager.userProgress.collect { progress -> }
```

## 📊 ViewModel API

```kotlin
val viewModel = ViewModelProvider(this)[TutorialViewModel::class.java]

// Actions
viewModel.startMission("mission_id")
viewModel.completeStep("mission_id", "step_id")
viewModel.filterByCategory(MissionCategory.SOCIAL)

// Queries
viewModel.getFilteredMissions()
viewModel.getCompletedMissions()
viewModel.getInProgressMissions()
viewModel.getAvailableMissions()
viewModel.getStatistics()

// Observe
viewModel.uiState.collect { state -> }
viewModel.missions.collect { missions -> }
viewModel.userProgress.collect { progress -> }
```

## 🎯 Mission IDs

| ID | Title |
|----|-------|
| `welcome_to_synapse` | Welcome to Synapse |
| `setup_profile` | Complete Your Profile |
| `create_first_post` | Share Your First Post |
| `build_connections` | Build Your Network |
| `master_messaging` | Master Messaging |
| `discover_content` | Discover Amazing Content |
| `advanced_features` | Unlock Advanced Features |

## 🏷️ Categories

```kotlin
MissionCategory.GETTING_STARTED  // 🚀
MissionCategory.SOCIAL           // 👥
MissionCategory.MESSAGING        // 💬
MissionCategory.CONTENT_CREATION // ✨
MissionCategory.PROFILE          // 👤
MissionCategory.ADVANCED         // ⚡
```

## 🎚️ Difficulty Levels

```kotlin
MissionDifficulty.BEGINNER      // 🟢 Green
MissionDifficulty.INTERMEDIATE  // 🟠 Orange
MissionDifficulty.ADVANCED      // 🔴 Red
```

## 🔍 Verification Types

```kotlin
VerificationType.MANUAL          // User marks complete
VerificationType.ACTION          // Specific action
VerificationType.NAVIGATION      // Navigate to screen
VerificationType.DATA_CREATION   // Create data
VerificationType.FEATURE_USAGE   // Use feature (with count)
```

## 📱 UI Components

```kotlin
// Launch main tutorial
startActivity(Intent(this, TutorialActivity::class.java))

// Launch demo/testing
startActivity(Intent(this, TutorialDemoActivity::class.java))

// Embed fragment
supportFragmentManager.beginTransaction()
    .replace(R.id.container, TutorialMissionsFragment.newInstance())
    .commit()

// Show mission detail
supportFragmentManager.beginTransaction()
    .replace(R.id.container, MissionDetailFragment.newInstance(missionId))
    .commit()
```

## 💾 Data Models

```kotlin
// Mission
data class TutorialMission(
    val id: String,
    val title: String,
    val description: String,
    val category: MissionCategory,
    val difficulty: MissionDifficulty,
    val steps: List<MissionStep>,
    val rewards: MissionRewards,
    val isCompleted: Boolean,
    val currentStep: Int
)

// Step
data class MissionStep(
    val id: String,
    val title: String,
    val description: String,
    val instruction: String,
    val verificationType: VerificationType,
    val verificationData: String?,
    val estimatedMinutes: Int,
    val isCompleted: Boolean
)

// Rewards
data class MissionRewards(
    val xp: Int,
    val badge: String?,
    val unlockFeature: String?,
    val title: String?
)

// Progress
data class UserProgress(
    val totalXp: Int,
    val badges: List<String>,
    val unlockedFeatures: List<String>,
    val completedMissionsCount: Int
)
```

## 🧪 Testing

```kotlin
// Reset all progress
TutorialManager.getInstance(context).resetAllMissions()

// Test tracking
getTutorialTracker().trackPostCreated()

// Verify completion
val mission = manager.missions.value.find { it.id == "mission_id" }
assert(mission?.isCompleted == true)

// Launch demo activity
startActivity(Intent(this, TutorialDemoActivity::class.java))
```

## 📈 Statistics

```kotlin
val stats = viewModel.getStatistics()

stats.totalMissions           // Total number of missions
stats.completedMissions       // Number completed
stats.inProgressMissions      // Number in progress
stats.totalXp                 // Total XP earned
stats.level                   // Current level
stats.badges                  // Number of badges
stats.getCompletionPercentage() // Overall completion %
```

## 🎁 Rewards System

```kotlin
// XP System
Level = (Total XP / 100) + 1
XP to Next Level = (Level * 100) - Total XP

// Available Rewards
- XP: 50-200 per mission
- Badges: 7 unique badges
- Titles: Display titles
- Unlocks: Advanced features
```

## 🔗 File Locations

```
app/src/main/java/com/synapse/social/studioasinc/
├── model/TutorialMission.kt
├── domain/TutorialManager.kt
├── domain/TutorialMissions.kt
├── presentation/TutorialViewModel.kt
├── fragments/TutorialMissionsFragment.kt
├── fragments/MissionDetailFragment.kt
├── adapters/TutorialMissionsAdapter.kt
├── util/TutorialTracker.kt
├── TutorialActivity.kt
└── TutorialDemoActivity.kt

Docs/
├── TUTORIAL_MISSIONS_GUIDE.md
├── TUTORIAL_MISSIONS_README.md
└── TUTORIAL_QUICK_REFERENCE.md
```

## 💡 Pro Tips

1. **Always track after action completes** - Don't track before the action succeeds
2. **Use appropriate verification types** - Match tracking method to verification type
3. **Test with demo activity** - Use TutorialDemoActivity for quick testing
4. **Reset for testing** - Use resetAllMissions() to test from scratch
5. **Check mission IDs** - Ensure mission/step IDs match exactly
6. **Handle counts properly** - For count-based tracking, pass cumulative totals
7. **Observe in lifecycle scope** - Use lifecycleScope for StateFlow collection

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Mission not completing | Check verification type matches tracking method |
| Progress not saving | Verify SharedPreferences permissions |
| UI not updating | Ensure StateFlow collection in lifecycle scope |
| Wrong chat ID format | Verify verificationData string matches exactly |

## 📚 Documentation

- **Full Guide**: `Docs/TUTORIAL_MISSIONS_GUIDE.md`
- **Quick Start**: `Docs/TUTORIAL_MISSIONS_README.md`
- **This Reference**: `Docs/TUTORIAL_QUICK_REFERENCE.md`
- **Examples**: `util/TutorialIntegrationExample.kt`

---

**Print this card for quick reference during development!**
