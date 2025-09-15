-repackageclasses
-ignorewarnings
-dontwarn
-dontnote
# Keep all Activities
-keep public class * extends android.app.Activity

# Specifically keep your N3 activity (if you know its original name)
-keep class com.synapse.social.studioasinc.N3 { *; }

# Keep gRPC classes for Firebase Firestore
-keep class io.grpc.** { *; }
-keep class io.grpc.android.** { *; }