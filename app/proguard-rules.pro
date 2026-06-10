# Add project specific ProGuard rules here.

# Keep data classes used in DataStore serialization
-keep class com.bl4ckswordsman.nightjar.data.** { *; }

# Keep service and receiver classes (Android references them by name)
-keep class com.bl4ckswordsman.nightjar.service.** { *; }
-keep class com.bl4ckswordsman.nightjar.receiver.** { *; }

# Hilt — keep generated classes (consumer rules from hilt-android handle most)
-keepclasseswithmembernames class * {
    @dagger.* <fields>;
}
-keep,allowobfuscation @dagger.hilt.* class *

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Navigation — keep route destinations
-keep class * extends androidx.navigation.NavArgs

# Suppress notes for generated Hilt code
-dontnote dagger.**
-dontnote hilt.**

# Preserve line numbers in stack traces for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile