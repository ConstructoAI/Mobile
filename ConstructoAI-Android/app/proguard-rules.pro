# Proguard rules for ConstructoAI

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep application class
-keep class ca.constructoai.app.** { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
