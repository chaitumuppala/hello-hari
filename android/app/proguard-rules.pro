# React Native rules
-keep class com.facebook.react.** { *; }
-keep class com.facebook.hermes.** { *; }
-keep class com.swmansion.reanimated.** { *; }
-keep class com.facebook.jni.** { *; }

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

# Keep our package
-keep class com.hellohari.** { *; }

# Hermes flags
-keep class com.facebook.hermes.unicode.** { *; }
-keep class com.facebook.jni.** { *; }
