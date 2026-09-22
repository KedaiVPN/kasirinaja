# ============================================
# POS Kedai - ProGuard / R8 Rules
# ============================================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# Android / Jetpack
# ============================================
-keep class android.support.** { *; }
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ViewModels, Composables & Navigation
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.poskedai.store.ui.viewmodels.** { *; }
-keep class com.poskedai.store.presentation.** { *; }

# Data classes (Room Entity, DTO)
-keep class com.poskedai.store.data.** { *; }
-keep class com.poskedai.core.data.** { *; }

# ============================================
# Room (persistence)
# ============================================
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class *
-keep interface * extends androidx.room.** { *; }
-dontwarn androidx.room.**

# ============================================
# Retrofit / OkHttp / Gson
# ============================================
# Retrofit does reflection on generic types. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Gson / Model classes used in API
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * { @com.google.gson.annotations.* <fields>; }
-keep class com.poskedai.store.data.model.** { *; }
-keep class com.poskedai.core.data.model.** { *; }
-keep class com.poskedai.core.network.** { *; }
-keep interface com.poskedai.core.network.** { *; }

# Kotlin data classes & sealed classes
-keep class kotlin.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.poskedai.core.network.** { *; }
-keepclassmembers class com.poskedai.store.data.local.** { *; }
-keepclassmembers class com.poskedai.core.data.local.** { *; }

# Retrofit interface methods (keep annotations)
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============================================
# Compose
# ============================================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class com.poskedai.store.ui.** { *; }

# ============================================
# Vico (charts)
# ============================================
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ============================================
# Coil (image loading)
# ============================================
-dontwarn coil.**
-keep class coil.** { *; }

# ============================================
# Firebase / ML Kit
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# ============================================
# WorkManager
# ============================================
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ============================================
# Kotlin / Coroutines
# ============================================
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlinx.coroutines.* <methods>;
}

# ============================================
# ZXing (QR / barcode)
# ============================================
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ============================================
# Serializable / Parcelable
# ============================================
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }
