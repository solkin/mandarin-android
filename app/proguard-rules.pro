# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-assumenosideeffects class android.util.Log {
  public static int d(...);
}
-keep class com.tomclaw.mandarin.util.Unobfuscatable
-keep class * implements com.tomclaw.mandarin.util.Unobfuscatable
-keepclassmembernames class * implements com.tomclaw.mandarin.util.Unobfuscatable {
  !transient <fields>;
}
-keepnames class * implements com.tomclaw.mandarin.util.Unobfuscatable {
  !transient <fields>;
}
-keepclassmembers class * implements com.tomclaw.mandarin.util.Unobfuscatable {
  <init>(...);
}

# Ugly workaround for 4.2.2 and Support library
-keep class !android.support.v7.internal.view.menu.*
-keep class * implements android.support.v4.internal.view.SupportMenu
-keep class android.support.v7.** {*;}
-keep interface android.support.v7.** { *; }
-dontwarn android.support.v7.**

# Support design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

-keep class com.akexorcist.roundcornerprogressbar.** { *; }
-dontwarn com.akexorcist.roundcornerprogressbar.**

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepattributes Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn java.nio.**
-dontwarn okio.**

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

-useuniqueclassmembernames
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-printseeds seeds.txt
-printusage unused.txt
-printmapping mapping.txt