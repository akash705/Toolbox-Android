# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.toolbox.**$$serializer { *; }
-keepclassmembers class com.toolbox.** {
    *** Companion;
}
-keepclasseswithmembers class com.toolbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance widgets — ActionCallback classes resolved by name at runtime
-keep class com.toolbox.widgets.** { *; }
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# Navigation Compose — keep @Serializable destination objects
-keep class com.toolbox.nav.** { *; }

# Keep Compose runtime for Glance
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**
