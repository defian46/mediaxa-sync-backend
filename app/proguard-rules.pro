# --- General Android & Kotlin Keep Rules ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- kotlinx.serialization Rules ---
# Keep serializable classes and their serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *** companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class *$$serializer { *; }

# --- Room Database Rules ---
# Keep Room annotations and database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# --- WorkManager Rules ---
# Keep Worker classes and their constructors
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
