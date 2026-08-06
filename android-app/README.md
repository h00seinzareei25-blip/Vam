# Vam Debt Manager - Android App

اپ اندروید «مدیریت بدهی‌ها + ریز اقساط» با یادآوری نوتیفیکیشن.

## ویژگی‌ها
- مدیریت بدهی‌ها و اقساط (همان منطق وب‌اپ)
- یادآوری نوتیفیکیشن در روز سررسید و ۱ و ۳ روز قبل (ساعت ۹ صبح)
- ذخیره‌سازی محلی + خروجی/بازیابی JSON
- بازگردانی یادآوری‌ها بعد از ری‌استارت گوشی

## ساخت APK

```bash
# نیاز به Android SDK و JDK 17+
cd android-app
./gradlew assembleRelease
# خروجی:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

برای امضای دیباگ سریع‌تر:

```bash
./gradlew assembleDebug
```
