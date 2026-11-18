# _archive — ملفات معزولة (مالها لزمة في المشروع الحالي)

كل اللي هنا **نُقل** من أماكنه الأصلية، ما انحذف. المشروع الشغّال الآن هو تطبيق **Kotlin / Jetpack Compose** فقط داخل `sukoon-nocturne/`.

## المحتويات

| المجلد | شنو فيه | ليش معزول |
| --- | --- | --- |
| `flutter-legacy/` | تطبيق Flutter القديم كامل (`lib/`, `android/`, `ios/`, `macos/`, `windows/`, `linux/`, `web/`, `test/`, `bin/`, `pubspec.*`, `.metadata`, `device_tracks.json`, أرشيف zip) | المشروع تحوّل لتطبيق Kotlin أصلي، وهذي نسخة قديمة غير مستخدمة |
| `kotlin-legacy/_compose_backup/` | نسخة قديمة من ملفات Compose | مجرد باك أب يدوي، الحالي أحدث |
| `kotlin-legacy/_legacy_backup/` | نسخة أقدم (MusicRepository, SukoonScreens...) | مرحلة سابقة من التطوير |
| `kotlin-legacy/scratch/` | سكربت Python لتنظيف ميتاداتا | أداة تجريبية |
| `kotlin-legacy/public/` + `kotlin-legacy/assets-public/` | بناء واجهة ويب React/Vite | **مافي أي كود Kotlin يستخدمه** — كان يزيد حجم APK ~2MB |
| `ide-local/` | إعدادات Android Studio القديمة (`.idea`) | غير مطلوبة، والأفضل يفتحها الأستوديو من جديد |

## استرجاع أي شيء

كله محفوظ في تاريخ جيت، فتقدر ترجّع أي ملف:

```
git checkout <hash> -- _archive/flutter-legacy/lib
```

أو ترجّع المشروع كامل لحالة سابقة بأمر واحد:

```
tools\snap.bat list
tools\snap.bat back 3
```

## التخلص النهائي

إذا تأكدت أنك ما تحتاج شيئاً منها:

```
Remove-Item _archive -Recurse -Force
```

وتاريخ جيت يبقى فيه كل شيء إلى الأبد (مافي ريموت، فالتاريخ محلي عندك).
