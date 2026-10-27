# kotlinx.serialization DTOs used by Retrofit
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep,includedescriptorclasses class app.weddingplanner.**$$serializer { *; }
-keepclassmembers class app.weddingplanner.** { *** Companion; }
-keepclasseswithmembers class app.weddingplanner.** { kotlinx.serialization.KSerializer serializer(...); }
# Retrofit suspend functions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
