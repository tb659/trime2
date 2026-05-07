# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# disable obfuscation
-dontobfuscate

# Keep JNI interface
-keep class com.osfans.trime.core.* { *; }
-keep class android.widget.**
-keepclassmembers class android.widget.** {
    public protected <fields>;
    public protected <methods>;
}
# 1. 仅保护类名不被混淆（不进入大括号指定成员）
-keep class com.androlua.**

# 2. 显式保护公开（public）和受保护（protected）的成员
# 这样私有（private）的方法和字段就会因为没有被匹配到而被混淆
-keepclassmembers class com.androlua.** {
    public protected <fields>;
    public protected <methods>;
}
# 1. 仅保护类名不被混淆（不进入大括号指定成员）
-keep class org.luaj.**
-keep class com.vivo.speechsdk.**{*;}
# 百度语音 SDK 混淆保留规则
-keep class com.baidu.speech.**{*;}
-keep class com.baidu.tts.**{*;}

# 2. 显式保护公开（public）和受保护（protected）的成员
# 这样私有（private）的方法和字段就会因为没有被匹配到而被混淆
-keepclassmembers class org.luaj.** {
    public protected <fields>;
    public protected <methods>;
}
# remove kotlin null checks
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void checkReturnedValueIsNotNull(...);
    static void checkFieldIsNotNull(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# OkHttp 相关的可选依赖忽略
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.jsse.provider.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# 如果你还没有 OkHttp 的基础混淆规则，建议一并加上
-keepattributes Signature, InnerClasses, AnnotationDefault
