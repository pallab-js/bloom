# Add project specific ProGuard rules here.
-keep class com.vibenote.app.** { *; }

# Keep Room entities
-keep class com.vibenote.app.data.local.** { *; }
-keep class com.vibenote.app.domain.model.** { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer