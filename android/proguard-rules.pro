# Gson & Generic Type Resolution
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes *Annotation*

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.** { *; }

# Core Models & Settings (Gson & Record Serialization)
-keep class me.englishhugging.core.model.** { *; }
-keepclassmembers class me.englishhugging.core.model.** { *; }
-keep class me.englishhugging.core.settings.** { *; }
-keepclassmembers class me.englishhugging.core.settings.** { *; }
-keep class me.englishhugging.core.vocabulary.** { *; }

# Android UI & ViewBinding
-keep class me.englishhugging.android.databinding.** { *; }
-keepclassmembers class me.englishhugging.android.databinding.** { *; }
-keep class me.englishhugging.android.overlay.** { *; }

# Ignore compile-only annotations (Lombok)
-dontwarn lombok.**
