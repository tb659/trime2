# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# disable obfuscation
#-dontobfuscate

# Keep JNI interface
-keep class com.osfans.trime.** { *; }
-keep class android.widget.**
-keepclassmembers class android.widget.** {
    public protected <fields>;
    public protected <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
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
-keep class com.android.cglib.** { *; }
# =================================================================
# 🛡️ 精准保护 androidx 包下的视图 (Views) 和适配器 (Adapters)
# =================================================================

# 1. 保护所有继承自 androidx.viewpager2.widget.ViewPager2 或 androidx.constraintlayout.widget.ConstraintLayout 等
# 以及所有在 androidx 包下可能用到的自定义 View 类名
-keep class androidx.** extends android.view.View {
    public protected <fields>;
    public protected <methods>;
}

# 2. 保护 androidx 下的所有 RecyclerView 适配器类名
-keep class * extends androidx.recyclerview.widget.RecyclerView$Adapter

# 3. 显式保护这些适配器内部的公开（public）和受保护（protected）方法/字段
# 确保布局管理器、数据绑定机制能正常反射调用它们
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$Adapter {
    public protected <fields>;
    public protected <methods>;
}

# 4. 保护所有的 RecyclerView$ViewHolder，防止视图持有者由于混淆导致内部的 View 绑定失效
-keep class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public protected <fields>;
    public protected <methods>;
}
# 1. 保护所有继承自 RecyclerView$LayoutManager 的类名不被混淆
# 确保在 XML 中通过 app:layoutManager="xxx" 引用时，反射机制能找到该类
-keep class * extends androidx.recyclerview.widget.RecyclerView$LayoutManager

# 2. 保护所有布局管理器内部的公开构造方法
# 因为 Android 系统在解析 XML 的 layoutManager 属性时，必须反射调用它们的构造方法
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$LayoutManager {
    public <init>(...);
}
# 如果你还没有 OkHttp 的基础混淆规则，建议一并加上
-keepattributes Signature, InnerClasses, AnnotationDefault
