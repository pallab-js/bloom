# Add project specific ProGuard rules here.
-keep class com.vibenote.app.** { *; }

# Keep Room entities
-keep class com.vibenote.app.data.local.** { *; }
-keep class com.vibenote.app.domain.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**