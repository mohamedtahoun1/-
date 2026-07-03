# تطبيق الأندرويد - Remote Control Device Side

## إزاي تفتح المشروع
1. افتح Android Studio > New Project > Empty Activity (Kotlin)
2. انسخ الملفات دي جوه `app/src/main/java/com/example/remotecontrol/`
3. ضيف الـ dependencies والصلاحيات المكتوبة تحت

## app/build.gradle.kts - الإضافات المطلوبة

```kotlin
dependencies {
    // WebRTC (لإرسال الشاشة والتحكم)
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    // WebSocket client (للتواصل مع signaling server)
    implementation("org.java-websocket:Java-WebSocket:1.5.4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
```

## AndroidManifest.xml - الصلاحيات المطلوبة

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

<application ...>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <service
        android:name=".ScreenCaptureService"
        android:foregroundServiceType="mediaProjection"
        android:exported="false" />

    <service
        android:name=".RemoteControlAccessibilityService"
        android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
        android:exported="false">
        <intent-filter>
            <action android:name="android.accessibilityservice.AccessibilityService" />
        </intent-filter>
        <meta-data
            android:name="android.accessibilityservice"
            android:resource="@xml/accessibility_service_config" />
    </service>
</application>
```

## ملف res/xml/accessibility_service_config.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

## خطوات التشغيل على الموبايل (المستخدم)
1. تفتح التطبيق -> يظهر لك كود مكوّن من 6 أرقام
2. يطلب منك موافقتين ضروريتين ولازم تديهم يدويًا (أندرويد بيمنع تفعيلهم أوتوماتيك لأسباب أمنية):
   - **Screen Capture permission** (بيظهر تلقائي كـ dialog عند بدء البث)
   - **Accessibility Service** (لازم تروح الإعدادات وتفعّله يدويًا مرة واحدة بس)
3. بعد كده تقدر تدخل الكود ده من صفحة التحكم وتتصل فورًا في أي وقت لاحق (التطبيق ممكن يفضل شغال في الخلفية كـ foreground service مع notification ثابت يوضح إن فيه بث شغال - ده مطلوب من أندرويد لأسباب خصوصية، مينفعش يتشال).

## ملاحظة أمان مهمة
- الكود ده بيديك قناة اتصال، لكن **الشاشة والأوامر بتتبعت مباشرة (peer-to-peer) عن طريق WebRTC** ومشفرة تلقائيًا (DTLS-SRTP) - سيرفر الـ signaling مبيشوفش المحتوى.
- لو عايز أمان إضافي، ضيف باسورد/PIN يتوافق بين الطرفين قبل ما تسمح بالاتصال (فيه مكان لده في `MainActivity.kt`).
