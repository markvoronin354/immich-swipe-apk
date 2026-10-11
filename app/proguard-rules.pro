# --- GSON & DATA MODELS ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep fields annotated with @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep data models used by Gson reflection for serialization/deserialization.
# Gson relies on exact field names (e.g., albumName, isFavorite) to match JSON keys.
-keep class com.markvoronin.immichswipe.domain.model.** { *; }
-keep class com.markvoronin.immichswipe.data.api.** { *; }
-keep class com.markvoronin.immichswipe.data.local.model.** { *; }

# --- RETROFIT ---
# Retrofit, OkHttp, Coil, and Media3/ExoPlayer ship consumer rules automatically in their AARs.
# We only need annotation retention for Retrofit interface methods:
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# --- ROOM ---
# Room compiler (KSP) generates entity and DAO keep rules automatically.
-keep class * extends androidx.room.RoomDatabase
