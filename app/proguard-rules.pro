# Keep kotlinx.serialization generated serializers
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.Serializable *; }
# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
