# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ~/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in show.gradle.
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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# keep these libraries but allow obfuscating
-dontwarn twitter4j.**
-keep,allowobfuscation class twitter4j.**  {*;}
-keep class twitter4j.TwitterImpl {*;}
-keep class twitter4j.conf.PropertyConfigurationFactory {*;}
-adaptclassstrings twitter4j.**
-adaptresourcefilenames twitter4j.**
-adaptresourcefilecontents twitter4j.**

-dontwarn javax.management.DynamicMBean
-keep,allowobfuscation class javax.management.DynamicMBean {*;}
-adaptclassstrings javax.management.DynamicMBean

-dontwarn org.conscrypt.Conscrypt
-keep,allowobfuscation class org.conscrypt.Conscrypt  {*;}
-adaptclassstrings org.conscrypt.Conscrypt

-dontwarn org.conscrypt.OpenSSLProvider
-keep,allowobfuscation class org.conscrypt.OpenSSLProvider  {*;}
-adaptclassstrings org.conscrypt.OpenSSLProvider

-dontwarn javax.annotation.Nullable
-keep,allowobfuscation class javax.annotation.Nullable  {*;}
-adaptclassstrings javax.annotation.Nullable


# use dictionaries to create random package names
-obfuscationdictionary dict/obfuscation-dictionary.txt
-classobfuscationdictionary dict/class-dictionary.txt
-packageobfuscationdictionary dict/package-dictionary.txt