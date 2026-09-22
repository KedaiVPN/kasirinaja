# =====================
# Compose & Kotlin
# =====================
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# =====================
# Room
# =====================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# =====================
# Retrofit & OkHttp
# =====================
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# =====================
# Gson
# =====================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep all model/DTO classes used with Gson
-keep class com.poskedai.core.data.dto.** { *; }
-keep class com.poskedai.core.data.model.** { *; }
-keep class com.poskedai.store.data.model.** { *; }
-keep class com.poskedai.store.data.local.** { *; }

# =====================
# Firebase
# =====================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# =====================
# Coil
# =====================
-dontwarn coil.**

# =====================
# ML Kit Barcode
# =====================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# =====================
# Vico Charts
# =====================
-dontwarn com.patrykandpatrick.vico.**

# =====================
# WorkManager
# =====================
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# =====================
# ZXing
# =====================
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# =====================
# CameraX
# =====================
-dontwarn androidx.camera.**

# =====================
# Navigation Compose
# =====================
-keep class androidx.navigation.** { *; }

# =====================
# Keep BuildConfig
# =====================
-keep class com.poskedai.store.BuildConfig { *; }
