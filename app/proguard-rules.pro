# Retrofit + kotlinx.serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
# Room entities are referenced by generated code; keep them intact
-keep class com.nexus.ai.data.local.** { *; }
