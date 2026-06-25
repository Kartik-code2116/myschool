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

# Keep model classes used for Firebase Firestore serialization/deserialization
-keep class com.kartik.myschool.model.** { *; }

# Keep Room database, entities, and DAOs
-keep class com.kartik.myschool.data.** { *; }

# Firebase Firestore — model serialization
-keepattributes Signature
-keepattributes *Annotation*

# Firebase Auth + Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# iText PDF engine
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }

# RecyclerView adapters
-keep class com.kartik.myschool.adapter.** { *; }
-keep class com.kartik.myschool.ui.** { *; }

# Repository + utils
-keep class com.kartik.myschool.repository.** { *; }
-keep class com.kartik.myschool.utils.** { *; }

# Gson serialization (used in SessionContext)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep App Core Classes
-keep class com.kartik.myschool.MySchoolApplication { *; }
-keep class com.kartik.myschool.BaseActivity { *; }
-keep class com.kartik.myschool.SessionContext { *; }
-keep class com.kartik.myschool.AppCache { *; }