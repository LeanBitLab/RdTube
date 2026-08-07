# ProGuard / R8 Rules for RdTube

# -------------------------------------------------------------------
# kotlinx.serialization
# -------------------------------------------------------------------
# Preserve line numbers and source file names for readable stack traces & re-tracing
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowobfuscation,allowshrinking class *$$serializer { *; }
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.internal.** { *; }

# Preserve data models used in JSON deserialization
-keep class com.lean.reddittube.data.** { *; }
-keep class com.example.reddittube.data.** { *; }

# -------------------------------------------------------------------
# AndroidX Media3 / ExoPlayer
# -------------------------------------------------------------------
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.decoder.** { *; }

# Prevent reflection-loaded extractors from being stripped
-keep class androidx.media3.extractor.mp4.Mp4Extractor { *; }
-keep class androidx.media3.extractor.mp3.Mp3Extractor { *; }
-keep class androidx.media3.extractor.mkv.MatroskaExtractor { *; }
-keep class androidx.media3.extractor.ts.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.dash.** { *; }

-dontwarn androidx.media3.**

# -------------------------------------------------------------------
# Jetpack Compose & Navigation 3
# -------------------------------------------------------------------
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**
