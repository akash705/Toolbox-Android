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
