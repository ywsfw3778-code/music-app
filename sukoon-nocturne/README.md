# Sukoon Nocturne — تطبيق أندرويد (Kotlin + Compose)

هذا مجلد التطبيق الحقيقي. مشغل موسيقى محلي بواجهة AMOLED، يقرأ ملفات الجهاز، يجيب الأغلفة والميتاداتا من Deezer/iTunes، والكلمات المتزامنة من lrclib.

## التشغيل

```powershell
# من داخل هذا المجلد
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-all\9ot9r568e8zfvvd4mn8rbu1j0\gradle-9.3.1\bin\gradle.bat" assembleDebug

# التثبيت على الجوال
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

أو افتح المجلد في Android Studio واضغط ▶.

## الاختبارات

```powershell
& "...\gradle-9.3.1\bin\gradle.bat" testDebugUnitTest
```

## خريطة الكود

| المسار | الوظيفة |
| --- | --- |
| `MainActivity.kt` | نقطة الدخول والتنقّل بين الشاشات |
| `PlayerViewModel.kt` | حالة التشغيل + المكتبة + الترشيحات |
| `PlaybackService.kt` | خدمة MediaSession للتحكم من الإشعارات |
| `data/AudioRepository.kt` | مسح ملفات الصوت من MediaStore والأغلفة |
| `data/ArtistNormalizer.kt` | توحيد أسماء الفنانين وتنظيف العناوين |
| `data/OnlineMetadataResolver.kt` | الأغلفة/الميتاداتا من Deezer وiTunes |
| `data/LyricsRepository.kt` | الكلمات المتزامنة من lrclib |
| `data/SettingsStore.kt` | حفظ الإعدادات (DataStore) |
| `model/` | الموديلات والنصوص عربي/إنجليزي |
| `ui/screens/` | الشاشات (المكتبة، المشغل، الإعدادات...) |
| `ui/components/` | مكوّنات مشتركة |
| `ui/theme/` | الألوان والخطوط والثيم |

## إعدادات البناء

- `applicationId = com.hiby.music` — `minSdk 24` — `targetSdk 36`
- AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.2.10
- `local.properties` و`debug.keystore` و`.env` غير متتبعة في جيت (محلية فقط)
- `debug.keystore` موجود في الجذر لتوقيع نسخ debug، لا تحذفه

## ملاحظات

- لا يوجد تحديث تلقائي ولا Git remote — التحديث يدوي ببناء APK وتثبيته.
- ملفات قديمة/غير مستخدمة عُزلت في `../_archive/`.
