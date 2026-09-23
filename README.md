# ریحون | اشتراک آشپزخانه (Reyhoon Kitchen Subscription)

اپلیکیشن اندروید مدرن برای مشتریان آشپزخانه **ریحون**  
با قابلیت ورود کد اشتراک، مشاهده و ویرایش آدرس تحویل، و مشاهده منوی غذا همراه با قیمت‌ها.

## ویژگی‌ها

- **ورود با کد اشتراک**: با وارد کردن کد (مانند `REYHOON123`) آدرس و اطلاعات اشتراک بارگذاری می‌شود.
- **مدیریت آدرس**: مشاهده و ویرایش کامل آدرس تحویل، تلفن و یادداشت.
- **منوی غذا**: لیست کامل غذاها دسته‌بندی‌شده با قیمت به تومان.
- **رابط کاربری واکنش‌گرا (Responsive)**: کاملاً Scale می‌شود برای تمام اندازه‌های صفحه (موبایل، تبلت، صفحه عریض) با استفاده از Jetpack Compose و `BoxWithConstraints`.
- **پشتیبانی کامل RTL و فارسی**.
- **تم روشن/تاریک**.
- **GitHub Actions**: workflow برای بیلد خودکار APK.

## کدهای نمونه برای تست

| کد          | نام مشتری   | شهر   |
|-------------|-------------|-------|
| REYHOON123  | علی محمدی   | تهران |
| REYHOON456  | مریم احمدی  | اصفهان|
| TEST001     | کاربر تست   | شیراز |

## ساختار پروژه

```
Reyhoon-Kitchen-Subscription/
├── app/
│   ├── src/main/java/com/reyhoon/kitchen/
│   │   ├── MainActivity.kt
│   │   ├── data/          # Models + MockData
│   │   ├── navigation/    # NavGraph
│   │   └── ui/
│   │       ├── screens/   # EnterCode, Home, Menu, Address
│   │       └── theme/     # Color, Type, Theme
│   └── build.gradle.kts
├── .github/workflows/build-apk.yml
└── README.md
```

## نحوه اجرا

1. پروژه را در **Android Studio** (Hedgehog یا جدیدتر) باز کنید.
2. Gradle Sync را انجام دهید.
3. یک Emulator یا دستگاه واقعی انتخاب کنید.
4. Run کنید.

یا از خط فرمان (با Android SDK نصب‌شده):

```bash
./gradlew assembleDebug
```

APK در مسیر `app/build/outputs/apk/debug/` ساخته می‌شود.

## GitHub Actions

با هر push به `main` یا به صورت دستی (workflow_dispatch)، APK دیباگ ساخته و به عنوان Artifact آپلود می‌شود.  
از تب **Actions** می‌توانید فایل APK را دانلود کنید.

## تکنولوژی‌ها

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Adaptive / Responsive layouts
- Min SDK 24 | Target SDK 34

## توسعه آینده (پیشنهادی)

- اتصال به Backend واقعی (Firebase / REST API)
- پرداخت آنلاین و مدیریت سفارش
- نوتیفیکیشن وضعیت تحویل
- پنل ادمین برای مدیریت منو و اشتراک‌ها

---

ساخته شده با ❤️ برای آشپزخانه **ریحون**
