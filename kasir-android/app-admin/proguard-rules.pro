# ============================================
# POS Kedai Admin - ProGuard / R8 Rules
# ============================================

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android / Jetpack
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.poskedai.admin.ui.viewmodels.** { *; }

# Room
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class *
-keep interface * extends androidx.room.** { *; }
-dontwarn androidx.room.**

# Retrofit / OkHttp / Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * { @com.google.gson.annotations.* <fields>; }
-keep class com.poskedai.core.data.model.** { *; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class com.poskedai.admin.ui.** { *; }

# Firebase / ML Kit / CameraX
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Parcelable / Serializable
-keep class * implements android.os.Parcelable { *; }
-keep class * implements java.io.Serializable { *; }