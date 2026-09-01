# ===================================================================
# ProGuard / R8 Optimization & Obfuscation Rules for RdTube
# ===================================================================

# -------------------------------------------------------------------
# General Optimization & Shrinking Directives
# -------------------------------------------------------------------
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses 'com.lean.rdtube.obf'

# Preserve line numbers and source file names for readable stack traces & re-tracing
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose, debug, and info logging in release builds for max speed and zero log leaks
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# -------------------------------------------------------------------
# Kotlin & Coroutines
# -------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.**

# Strip Coroutines debug probes in release
-dontwarn kotlinx.coroutines.debug.**
-assumenosideeffects class kotlinx.coroutines.internal.SystemPropsKt {
    static int systemProp(java.lang.String, int, int, int) return 0;
}

# -------------------------------------------------------------------
# kotlinx.serialization & Data Models
# -------------------------------------------------------------------
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

# Preserve app data models and JSON deserialization classes
-keep class com.lean.reddittube.data.** { *; }
-keep class com.example.reddittube.data.** { *; }
-keep class com.lean.reddittube.utils.GitHubRelease { *; }
-keep class com.lean.reddittube.utils.GitHubAsset { *; }

# -------------------------------------------------------------------
# AndroidX Media3 / ExoPlayer
# -------------------------------------------------------------------
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.decoder.** { *; }

# Media3 extractors & streaming modules (prevent reflection-loaded extractors from being stripped)
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
-keepclassmembers class androidx.compose.runtime.Recomposer { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.navigation3.**

# -------------------------------------------------------------------
# Baseline Profiles & ProfileInstaller
# -------------------------------------------------------------------
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**
