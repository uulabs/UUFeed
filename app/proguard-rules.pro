# Release minify is off. If you turn it on, keep kotlinx.serialization companions.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class com.uulabs.uufeed.data.dto.** {
    *** Companion;
}
