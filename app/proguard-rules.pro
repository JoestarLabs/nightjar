# Add project specific ProGuard rules here.

# Preserve enum values if accessed reflectively
-keepclassmembers enum com.bl4ckswordsman.nightjar.data.SunsetAnimation { *; }

# Suppress notes for generated Hilt code
-dontnote dagger.**
-dontnote hilt.**

# Preserve line numbers in stack traces for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
