-repackageclasses ''

-keepattributes InnerClasses
-keepattributes LineNumberTable
-keepattributes Signature
-keepattributes SourceFile

-printmapping mapping.txt
-printseeds seeds.txt
-printusage unused.txt

-assumenosideeffects class android.util.Log {
	public static int d(...);
	public static int e(...);
	public static int i(...);
	public static int v(...);
	public static int w(...);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

### MANDARIN ###

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