# آشپزخانه ریحون — سیستم سفارش آنلاین

سه بخش اصلی:

| بخش | مسیر | توضیح |
|-----|------|--------|
| اپ آشپزخانه | `:app` | پنل ادمین اندروید، آلارم سفارش، وضعیت، حسابداری |
| اپ مشتری | `:customer` | ورود با کد، منو، ثبت سفارش، وضعیت، «تحویل گرفتم» |
| API + پنل HTML | `worker/` | Cloudflare Worker + مدیریت منو در مرورگر |

## ۱) دیپلوی سرور (Cloudflare Worker)

```bash
cd worker
npm i -g wrangler
wrangler login
wrangler deploy
```

آدرس خروجی را کپی کنید، مثلاً:
`https://reyhoon-api.xxxxx.workers.dev`

پنل ادمین منو:
`https://reyhoon-api.xxxxx.workers.dev/admin`  
کلید پیش‌فرض: `reyhoon-admin-2024`

## ۲) اتصال اپ‌ها به سرور

در **هر دو** فایل زیر `baseUrl` را همان آدرس Worker بگذارید:

- `app/src/main/java/com/reyhoon/kitchen/data/ApiConfig.kt`
- `customer/src/main/java/com/reyhoon/customer/ApiConfig.kt`

```kotlin
var baseUrl: String = "https://reyhoon-api.xxxxx.workers.dev"
```

سپس APK را دوباره بیلد کنید.

## ۳) جریان کار

1. در پنل HTML منو و قیمت را مدیریت کنید (هر دو اپ همان منو را می‌بینند).
2. مشتری را در پنل HTML یا اپ آشپزخانه با **کد اشتراک** بسازید.
3. مشتری با اپ مشتری وارد می‌شود، غذا سفارش می‌دهد.
4. اپ آشپزخانه هر ۱۲ ثانیه چک می‌کند → **آلارم صوتی + لرزش** برای سفارش جدید.
5. آشپزخانه وضعیت را عوض می‌کند:
   - سفارش ثبت شد → آماده‌سازی → ارسال شده → تحویل داده شد
6. مشتری هم می‌تواند **«تحویل گرفتم»** بزند → وضعیت تحویل + ساعت تحویل.

## بیلد محلی

```bash
./gradlew :app:assembleDebug :customer:assembleDebug
```

## GitHub Actions

با هر push دو آرتیفکت ساخته می‌شود:
- `reyhoon-kitchen-apk`
- `reyhoon-customer-apk`
