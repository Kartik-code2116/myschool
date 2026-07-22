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

# Preserve line numbers for stack trace debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep Firestore & database model classes (required for reflection/serialization)
-keep class com.kartik.myschool.model.** { *; }
-keep class com.kartik.myschool.data.** { *; }

# Gson serialization
-keepclassmembers class * implements com.google.gson.TypeAdapter { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Suppress missing class warnings from Firebase KTX & third-party libraries
-dontwarn com.google.firebase.**
-dontwarn com.itextpdf.**