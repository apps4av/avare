# R8 / Play DEX optimization.
# Do not keep the whole com.ds.avare package — that is why Play reported 2% obfuscation.

# WebView calls these methods by name from aircraft.html / plan.html / map HTML.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# USB serial driver probe uses reflection to instantiate chip-specific drivers.
-keep class com.hoho.android.usbserial.** { *; }

# METAR parser (Apache Oro) builds regex classes reflectively.
-keep class org.apache.oro.** { *; }

# Crashlytics: keep line numbers, but hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
