# Sukoon Nocturne — مشغل موسيقى أصلي (Kotlin + Jetpack Compose)

مشغل موسيقى أندرويد بواجهة AMOLED سوداء، كلمات متزامنة، أغلفة ألبومات، وتحميل من يوتيوب.

## هيكل المشروع

```
music app/
  sukoon-nocturne/          تطبيق أندرويد (Kotlin + Compose) ← التطبيق الحقيقي
    app/src/main/java/com/example/
      MainActivity.kt       نقطة الدخول والتنقّل
      PlayerViewModel.kt    حالة التشغيل والمكتبة
      data/                 المسح المحلي، الكلمات، الميتاداتا، الإعدادات
      model/                الموديلات والنصوص (عربي/إنجليزي)
      playback/             خدمة التشغيل
      ui/screens/           الشاشات
      ui/components/        مكوّنات مشتركة
      ui/theme/             الثيم والألوان والخطوط
    app/src/main/res/       الموارد والأيقونات
    app/src/test/           اختبارات وحدة
  tools/                    أدوات مساعدة (نسخ الرجوع المحلية)
  docs/design/              صور التصاميم المعتمدة
  _archive/                 ملفات معزولة قديمة (فلاتر + باك أب) — مو جزء من البناء
```

## بناء وتثبيت

```powershell
cd sukoon-nocturne

# بناء APK للتجربة (debug)
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" assembleDebug

# تثبيت على الجوال (يحتاج adb من Android SDK)
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

أو افتح مجلد `sukoon-nocturne` في Android Studio وشغّل ▶ مباشرة.

**الأساسيات:** `applicationId = com.hiby.music`، `minSdk 24`، `targetSdk 36`، AGP 9.1.1، Gradle 9.3.1.
مفاتيح يوتيوب تُقرأ من `.env` (انسخه من `.env.example`) — غير متتبَّع في جيت.

## اختبارات

```powershell
cd sukoon-nocturne
& "...\gradle-9.3.1\bin\gradle.bat" testDebugUnitTest
```

## نسخ الرجوع (بدون GitHub)

```powershell
tools\snap.bat save "شنو سويت"
tools\snap.bat list
tools\snap.bat back 3
```

تحديث التطبيق على الجوال يدوي دائماً: تعديل → بناء APK → `adb install -r`.

## ملاحظات

- لا يوجد أي ريموت جيت ولا تحديث تلقائي للتطبيق.
- `_archive/` فيه نسخة فلاتر القديمة والملفات الميتة؛ راجع `_archive/README.md`.
