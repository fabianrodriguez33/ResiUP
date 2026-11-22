# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# SQLCipher carga sus clases nativas por nombre (JNI).
-keep class net.zetetic.database.** { *; }
# Dependencias opcionales que no viajan en la app.
-dontwarn org.slf4j.**

# Clases que el sistema crea por reflexión con su constructor vacío. Sin estas reglas la versión de entrega
# se cierra al abrir (WorkManager no puede crear su base de datos y ML Kit no registra el lector de QR).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class * implements androidx.startup.Initializer { <init>(); }
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
