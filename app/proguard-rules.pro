# Terista Environment ProGuard Rules

# Keep all Terista Environment app classes
-keep class com.terista.environment.** { *; }

# Keep BlackBox core library (required for virtual app engine)
-keep class top.niunaijun.blackbox.** { *; }
-keep class top.niunaijun.jnihook.** { *; }
-keep class top.niunaijun.blackreflection.** { *; }
-keep class mirror.** { *; }
-keep class android.** { *; }
-keep class com.android.** { *; }

# Keep BlackReflection annotations
-keep @top.niunaijun.blackreflection.annotation.BClass class * { *; }
-keep @top.niunaijun.blackreflection.annotation.BClassName class * { *; }
-keep @top.niunaijun.blackreflection.annotation.BClassNameNotProcess class * { *; }
-keepclasseswithmembernames class * {
    @top.niunaijun.blackreflection.annotation.BField* <methods>;
    @top.niunaijun.blackreflection.annotation.BFieldNotProcess* <methods>;
    @top.niunaijun.blackreflection.annotation.BFieldSetNotProcess* <methods>;
    @top.niunaijun.blackreflection.annotation.BFieldCheckNotProcess* <methods>;
    @top.niunaijun.blackreflection.annotation.BMethod* <methods>;
    @top.niunaijun.blackreflection.annotation.BStaticField* <methods>;
    @top.niunaijun.blackreflection.annotation.BStaticMethod* <methods>;
    @top.niunaijun.blackreflection.annotation.BMethodCheckNotProcess* <methods>;
    @top.niunaijun.blackreflection.annotation.BConstructor* <methods>;
    @top.niunaijun.blackreflection.annotation.BConstructorNotProcess* <methods>;
}

# Keep third-party libraries used in UI
-keep class com.imuxuan.floatingview.** { *; }
-keep class com.roger.catloadinglibrary.** { *; }
-keep class cbfg.rvadapter.** { *; }
-keep class com.othershe.cornerlabelview.** { *; }
-keep class com.github.nukc.stateview.** { *; }
-keep class com.ferfalk.simplesearchview.** { *; }
-keep class com.tbuonomo.viewpagerdotsindicator.** { *; }
-keep class org.osmdroid.** { *; }
-keep class com.afollestad.materialdialogs.** { *; }

# Keep Parcelable classes (BLocation etc)
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep AIDL-generated classes
-keep class * extends android.os.IInterface { *; }
-keep class * extends android.os.Binder { *; }

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Suppress warnings for internal Android APIs
-dontwarn android.**
-dontwarn com.android.**
-dontwarn dalvik.**
